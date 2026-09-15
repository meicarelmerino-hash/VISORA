package com.infotec.drivenotifications.services;

import java.io.ByteArrayOutputStream; // SOLUCIÓN AL ERROR DE ByteArrayOutputStream
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.stereotype.Service;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.infotec.drivenotifications.models.DriveItem;
import com.infotec.drivenotifications.models.HistorialCambioDocumento;
import com.infotec.drivenotifications.models.Usuario;
import com.infotec.drivenotifications.repositories.DriveItemRepository;
import com.infotec.drivenotifications.repositories.HistorialCambioDocumentoRepository;






@Service
public class DriveMonitorService {

    private static final String TELEGRAM_TOKEN = "xxxx";
    private static final String CHAT_ID = "-xxxx";
    private static final int INTERVALO = 30000; 

    private final DriveService driveService;
    private final HistorialCambioDocumentoRepository historialRepository; 
    private final DriveItemRepository driveItemRepository; 

    public DriveMonitorService(DriveService driveService, 
                               HistorialCambioDocumentoRepository historialRepository, 
                               DriveItemRepository driveItemRepository) {
        this.driveService = driveService;
        this.historialRepository = historialRepository;
        this.driveItemRepository = driveItemRepository;
    }

    private final Map<String, ScheduledExecutorService> monitores = new ConcurrentHashMap<>();
    private final Map<String, String> fechasMonitoreo = new ConcurrentHashMap<>();

    







    public void iniciarMonitoreo(String fileId, Usuario usuario) {
        if (monitores.containsKey(fileId)) return;

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        
        scheduler.scheduleAtFixedRate(() -> {
            try {
                System.out.println("Revisando cambios en: " + fileId + " a las " + java.time.LocalDateTime.now());
                Drive service = driveService.getDriveService(usuario);

                // Obtener metadatos del archivo
                File archivo = service.files().get(fileId)
                        .setFields("modifiedTime,lastModifyingUser,name")
                        .execute();

                String fechaActual = formatearFechaCDMX(archivo.getModifiedTime());
                String ultimaFecha = fechasMonitoreo.get(fileId);

                if (ultimaFecha == null) {
                    fechasMonitoreo.put(fileId, fechaActual);
                    System.out.println(">>> Monitoreo inicial activo para: " + archivo.getName());

                } else if (!fechaActual.equals(ultimaFecha)) {
                    fechasMonitoreo.put(fileId, fechaActual);
                    
                    // 1. Obtener el texto EN VIVO desde Google Drive (El "Después")
                    String textoNuevo = obtenerContenidoDocumento(service, fileId);
                    
                    // 2. Buscar el DriveItem en la BD
                    DriveItem item = driveItemRepository.findByDriveId(fileId);
                    if (item == null) return;

                    // 3. RECUPERAR EL PASADO: Buscar el registro más reciente en el historial
                    HistorialCambioDocumento ultimoRegistro = historialRepository
                        .findFirstByDriveItemOrderByFechaCambioDesc(item) 
                        .orElse(null);

                    // Si existe un registro previo, su "texto modificado" es ahora nuestro "texto original"
                    String textoAnterior = (ultimoRegistro != null) ? ultimoRegistro.getTextoModificado() : "Archivo recién registrado o sin historial previo.";

                    // 4. Ejecutar el análisis con IA (Prompt mejorado abajo)
                    String promptCompleto = prepararPrompt(textoAnterior, textoNuevo);
                    String resultadoIA = consultarLlama3(promptCompleto);
                    
                    // 5. Extraer prioridad y limpiar resultado para Telegram
                    String prioridad = extraerPrioridad(resultadoIA);

                    // 6. GUARDAR EL NUEVO REGISTRO
                    HistorialCambioDocumento nuevoHistorial = new HistorialCambioDocumento();
                    nuevoHistorial.setDriveItem(item);
                    nuevoHistorial.setTextoOriginal(textoAnterior); // Lo que había antes en la BD
                    nuevoHistorial.setTextoModificado(textoNuevo);   // Lo que acabamos de bajar de Drive
                    nuevoHistorial.setAnalisisIa(resultadoIA);
                    nuevoHistorial.setPrioridad(prioridad);
                    
                    String email = (archivo.getLastModifyingUser() != null) ? archivo.getLastModifyingUser().getEmailAddress() : "Usuario desconocido";
                    nuevoHistorial.setAutorCambio(email);
                    
                    historialRepository.save(nuevoHistorial);

                    // 7. Notificar con formato limpio
                    if (prioridad.equals("BAJA") || prioridad.equals("MEDIA") || prioridad.equals("ALTA")) {
                        enviarTelegram(formatearMensajeTelegram(archivo.getName(), email, prioridad, resultadoIA), usuario);
                        System.out.println("Se envió nueva alerta a telegram");
                    }else{
                        System.out.println("no hubo ningun tipo de prioridad");
                    }
                }
                
            } catch (Exception e) {
                System.err.println("Error en hilo de monitoreo para " + fileId + ": " + e.getMessage());
            }
        }, 0, INTERVALO, TimeUnit.MILLISECONDS);

        monitores.put(fileId, scheduler);
    }



    private String formatearMensajeTelegram(String nombreArchivo, String autor, String prioridad, String analisis) {
        String emoji = prioridad.equals("ALTA") ? "🔴" : "🟡";
        return String.format(
            "🔔 *Modificación Detectada*\n\n" +
            "📄 *Documento:* %s\n" +
            "👤 *Editor:* %s\n" +
            "⚠️ *Prioridad:* %s %s\n\n" +
            "🤖 *Análisis de la IA:*\n%s",
            nombreArchivo, autor, emoji, prioridad, analisis
        );
    }

    private String prepararPrompt(String anterior, String nuevo) {
        return """
            Eres un experto en auditoría de documentos. Tu objetivo es comparar dos versiones de un texto y explicar los cambios de forma humana y clara.

            INSTRUCCIONES:
            1. Compara el TEXTO_ANTERIOR con el TEXTO_NUEVO.
            2. Si no hay cambios significativos, indica que son cambios menores.
            3. Clasifica la prioridad según el impacto en el significado del documento.

            REGLAS DE PRIORIDAD:
            - ALTA: Cambios en fechas, nombres de personas, montos de dinero, cláusulas legales o acciones requeridas.
            - MEDIA: Reescritura de párrafos que mantienen la idea pero cambian el tono, o adición de información secundaria.
            - BAJA: Corrección de tildes, comas o errores de dedo.

            FORMATO DE RESPUESTA (Usa este esquema exacto):
            RESUMEN: (Escribe aquí un resumen de una sola línea de lo que pasó)
            DETALLES:
            • [Escribe aquí los cambios específicos usando viñetas]
            PRIORIDAD: [BAJA, MEDIA o ALTA]

            TEXTO_ANTERIOR:
            %s

            TEXTO_NUEVO:
            %s
            """.formatted(anterior, nuevo);
    }

    private String extraerPrioridad(String resultadoIA) {
        // Convertimos a mayúsculas para que la comparación sea más robusta
        String resultadoUpper = resultadoIA.toUpperCase();

        // Buscamos la palabra clave que la IA puso después de "PRIORIDAD:"
        if (resultadoUpper.contains("PRIORIDAD: ALTA") || resultadoUpper.contains("PRIORIDAD ALTA")) {
            return "ALTA";
        } else if (resultadoUpper.contains("PRIORIDAD: MEDIA") || resultadoUpper.contains("PRIORIDAD MEDIA")) {
            return "MEDIA";
        } else if (resultadoUpper.contains("PRIORIDAD: BAJA") || resultadoUpper.contains("PRIORIDAD BAJA")) {
            return "BAJA";
        }

        // Por defecto, si la IA no sigue el formato, lo marcamos como BAJA para no saturar
        return "BAJA";
    }


    // --- MÉTODOS DE APOYO (Sin cambios, solo correcciones de visibilidad) ---

    public void detenerMonitoreo(String fileId) {
        ScheduledExecutorService scheduler = monitores.remove(fileId);
        if (scheduler != null) {
            scheduler.shutdownNow();
            fechasMonitoreo.remove(fileId);
            System.out.println(">>> Monitoreo detenido para ID: " + fileId);
        }
    }

    private String obtenerContenidoDocumento(Drive service, String fileId) throws Exception {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        service.files().export(fileId, "text/plain").executeMediaAndDownloadTo(outputStream);
        return outputStream.toString("UTF-8");
    }







    private String consultarLlama3(String prompt) {
        try {
            java.net.URL url = new java.net.URL("http://localhost:11434/api/generate");
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setRequestProperty("Content-Type", "application/json");
            con.setDoOutput(true);

            // --- SOLUCIÓN: Crear el JSON de forma segura con Jackson ---
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("model", "llama3:8b");
            body.put("prompt", prompt);
            body.put("stream", false);

            String jsonInputString = mapper.writeValueAsString(body);

            try (java.io.OutputStream os = con.getOutputStream()) {
                os.write(jsonInputString.getBytes("utf-8"));
            }

            // --- SOLUCIÓN: Extraer solo el texto de la respuesta ---
            java.util.Scanner s = new java.util.Scanner(con.getInputStream()).useDelimiter("\\A");
            String responseJson = s.hasNext() ? s.next() : "";
            
            com.fasterxml.jackson.databind.JsonNode rootNode = mapper.readTree(responseJson);
            return rootNode.get("response").asText(); // Devuelve solo el texto del análisis

        } catch (Exception e) {
            System.err.println("Error detallado IA: " + e.getMessage());
            return "Error consultando IA: " + e.getMessage();
        }
    }










    private void enviarTelegram(String mensaje, Usuario usuario) {
        // ... (Tu código de enviarTelegram se mantiene igual)
        try {
            String tokenFinal = (usuario.getTelegramToken() != null && !usuario.getTelegramToken().isBlank()) ? usuario.getTelegramToken() : TELEGRAM_TOKEN;
            String chatIdFinal = (usuario.getTelegramChatId() != null && !usuario.getTelegramChatId().isBlank()) ? usuario.getTelegramChatId() : CHAT_ID;

            if (tokenFinal.equals("xxxx")) return;

            String urlString = "https://api.telegram.org/bot" + tokenFinal + "/sendMessage";
            java.net.URL url = new java.net.URL(urlString);
            java.net.HttpURLConnection con = (java.net.HttpURLConnection) url.openConnection();
            con.setRequestMethod("POST");
            con.setDoOutput(true);

            String parametros = "chat_id=" + chatIdFinal + "&text=" + java.net.URLEncoder.encode(mensaje, "UTF-8") + "&parse_mode=Markdown";

            try (java.io.OutputStream os = con.getOutputStream()) {
                os.write(parametros.getBytes("UTF-8"));
            }
            con.getResponseCode();
        } catch (Exception e) {
            System.err.println("Error enviando Telegram: " + e.getMessage());
        }
    }

    private String formatearFechaCDMX(com.google.api.client.util.DateTime googleFecha) {
        java.time.Instant instant = java.time.Instant.ofEpochMilli(googleFecha.getValue());
        ZonedDateTime fechaCDMX = instant.atZone(ZoneId.of("America/Mexico_City"));
        return fechaCDMX.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
    }





    public boolean estaSiendoVigilado(String fileId) {
        return monitores.containsKey(fileId);
    }

}





