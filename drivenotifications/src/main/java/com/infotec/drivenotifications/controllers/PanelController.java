package com.infotec.drivenotifications.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.infotec.drivenotifications.models.DriveItem;
import com.infotec.drivenotifications.models.Usuario;
import com.infotec.drivenotifications.repositories.DriveItemRepository;
import com.infotec.drivenotifications.repositories.UsuarioRepository;
import com.infotec.drivenotifications.services.DriveMonitorService;
import com.infotec.drivenotifications.services.DriveService;




@Controller
public class PanelController {

    private final DriveService driveService;
    private final DriveMonitorService driveMonitorService;
    private final DriveItemRepository driveItemRepository;
    private final UsuarioRepository usuarioRepository;

    public PanelController(DriveService driveService, DriveMonitorService driveMonitorService, 
                           DriveItemRepository driveItemRepository, UsuarioRepository usuarioRepository) {
        this.driveService = driveService;
        this.driveMonitorService = driveMonitorService;
        this.driveItemRepository = driveItemRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Método auxiliar para obtener el usuario actual de forma limpia
     */
    private Usuario getUsuarioActual(Principal principal) {
        return usuarioRepository.findByNombre(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    private void cargarDatosPanel(Model model, Principal principal) {
        if (principal == null) return;

        Usuario usuario = getUsuarioActual(principal);
        model.addAttribute("usuarioActivo", usuario); 
        
        List<DriveItem> registros = driveItemRepository.findByCreadoPor(usuario);
        model.addAttribute("registrosDrive", registros);
        
        long totalVigilados = registros.stream().filter(DriveItem::isMonitoreando).count();
        model.addAttribute("totalVigilados", totalVigilados);
        model.addAttribute("driveMonitorService", driveMonitorService);
    }





















    


    @GetMapping("/panel")
    public String mostrarPanel(Model model, Principal principal) {
        cargarDatosPanel(model, principal);
        return "panel"; 
    }

    @PostMapping("/panel/guardar-drive")
    public String guardarDriveEnBaseDeDatos(@RequestParam("fileId") String fileId, 
                                            Principal principal, 
                                            RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = getUsuarioActual(principal);

            if (driveItemRepository.findByDriveIdAndCreadoPor(fileId, usuario).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Ya tienes este archivo en tu lista.");
                return "redirect:/panel";
            }

            // Pasamos el usuario para validar tokens
            var archivoGoogle = driveService.obtenerDetallesArchivo(fileId, usuario);

            DriveItem nuevoItem = new DriveItem();
            nuevoItem.setDriveId(archivoGoogle.getId());
            nuevoItem.setNombreDocumento(archivoGoogle.getName());
            nuevoItem.setCreadoPor(usuario);

            String tipo = "application/vnd.google-apps.folder".equals(archivoGoogle.getMimeType()) 
                        ? "Carpeta" : "Archivo/Documento";
            nuevoItem.setTipoDocumento(tipo);

            driveItemRepository.save(nuevoItem);
            redirectAttributes.addFlashAttribute("mensaje", "Archivo agregado con éxito.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
        }
        return "redirect:/panel";
    }

    @PostMapping("/panel/iniciar-monitoreo")
    public String iniciarMonitoreo(@RequestParam String fileId, Model model, Principal principal) {
        try {
            Usuario usuario = getUsuarioActual(principal);
            driveMonitorService.iniciarMonitoreo(fileId, usuario);
            
            DriveItem item = driveItemRepository.findByDriveIdAndCreadoPor(fileId, usuario)
                    .orElseThrow(() -> new RuntimeException("Registro no encontrado"));
            
            item.setMonitoreando(true);
            driveItemRepository.save(item);

            model.addAttribute("mensaje", "Monitoreo iniciado correctamente.");
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
        }
        cargarDatosPanel(model, principal);
        return "panel";
    }

    @PostMapping("/panel/detener-monitoreo")
    public String detenerMonitoreo(@RequestParam String fileId, Model model, Principal principal) {
        try {
            Usuario usuario = getUsuarioActual(principal);
            driveMonitorService.detenerMonitoreo(fileId);

            driveItemRepository.findByDriveIdAndCreadoPor(fileId, usuario).ifPresent(item -> {
                item.setMonitoreando(false);
                driveItemRepository.save(item);
            });

            model.addAttribute("mensaje", "Monitoreo detenido.");
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
        }
        cargarDatosPanel(model, principal);
        return "panel";
    }

    @PostMapping("/panel/eliminar")
    public String eliminarRegistro(@RequestParam String driveId, Model model, Principal principal) {
        try {
            Usuario usuario = getUsuarioActual(principal);
            driveMonitorService.detenerMonitoreo(driveId);

            driveItemRepository.findByDriveIdAndCreadoPor(driveId, usuario).ifPresent(driveItemRepository::delete);
            
            model.addAttribute("mensaje", "Registro eliminado.");
        } catch (Exception e) {
            model.addAttribute("error", "Error al eliminar: " + e.getMessage());
        }
        cargarDatosPanel(model, principal);
        return "panel";
    }










    @GetMapping("/panel/permisos")
    public String verPermisos(@RequestParam("fileId") String fileId, Model model, Principal principal) {
        try {
            Usuario usuario = getUsuarioActual(principal);
            List<com.google.api.services.drive.model.Permission> lista = driveService.obtenerPermisos(fileId, usuario);
            
            model.addAttribute("permisos", lista);
            model.addAttribute("fileIdConsultado", fileId); // Esto nos dice qué fila expandir
        } catch (Exception e) {
            model.addAttribute("error", "Error al obtener permisos: " + e.getMessage());
        }
        cargarDatosPanel(model, principal);
        return "panel";
    }









    @PostMapping("/panel/actualizar-todo")
    public String actualizarTodo(@RequestParam String fileId, 
                                @RequestParam String nuevoRol, 
                                Model model, Principal principal) {
        try {
            Usuario usuario = getUsuarioActual(principal);
            List<com.google.api.services.drive.model.Permission> permisos = driveService.obtenerPermisos(fileId, usuario);

            for (com.google.api.services.drive.model.Permission p : permisos) {
                // No intentamos cambiar permisos al dueño ni a "anyone" (acceso público)
                if (!"owner".equalsIgnoreCase(p.getRole()) && !"anyone".equalsIgnoreCase(p.getType())) {
                    driveService.actualizarPermiso(fileId, p.getEmailAddress(), nuevoRol, usuario);
                }
            }

            model.addAttribute("mensaje", "Se actualizaron todos los permisos a: " + nuevoRol);
            model.addAttribute("permisos", driveService.obtenerPermisos(fileId, usuario));
            model.addAttribute("fileIdConsultado", fileId);
        } catch (Exception e) {
            model.addAttribute("error", "Error al actualizar todo: " + e.getMessage());
        }
        cargarDatosPanel(model, principal);
        return "panel";
    }














    @PostMapping("/panel/actualizar")
    public String actualizarPermiso(@RequestParam String fileId, @RequestParam String email,
                                    @RequestParam String role, Model model, Principal principal) {
        try {
            Usuario usuario = getUsuarioActual(principal);
            driveService.actualizarPermiso(fileId, email, role, usuario);
            
            model.addAttribute("mensaje", "Permiso actualizado.");
            model.addAttribute("permisos", driveService.obtenerPermisos(fileId, usuario));
            model.addAttribute("fileIdConsultado", fileId);
        } catch (Exception e) {
            model.addAttribute("error", "Error: " + e.getMessage());
        }
        cargarDatosPanel(model, principal); 
        return "panel";
    }

    @PostMapping("/panel/restringir-acceso")
    public String restringirAcceso(@RequestParam String fileId, Model model, Principal principal) {
        try {
            Usuario usuario = getUsuarioActual(principal);
            driveService.eliminarAccesoPublico(fileId, usuario);
            
            model.addAttribute("mensaje", "Acceso restringido correctamente.");
            model.addAttribute("permisos", driveService.obtenerPermisos(fileId, usuario));
            model.addAttribute("fileIdConsultado", fileId);
        } catch (Exception e) {
            model.addAttribute("error", "Error al restringir: " + e.getMessage());
        }
        cargarDatosPanel(model, principal);
        return "panel";
    }


    @PostMapping("/panel/compartir")
    public String compartirRecurso(@RequestParam("fileId") String fileId,
                                @RequestParam("emails") String emails,
                                @RequestParam("role") String role,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = getUsuarioActual(principal);
            driveService.compartirArchivo(fileId, emails, role, usuario);
            redirectAttributes.addFlashAttribute("mensaje", "Invitaciones enviadas.");
        } catch (com.google.api.client.googleapis.json.GoogleJsonResponseException e) {
            // Esto nos dirá si es 403 (Permisos), 404 (No encontrado), etc.
            redirectAttributes.addFlashAttribute("error", "Error de Google: " + e.getDetails().getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error técnico: " + e.getMessage());
        }
        return "redirect:/panel";
    }






    @PostMapping("/panel/sincronizar")
    public String sincronizarNombres(Principal principal, RedirectAttributes redirectAttributes) {
        try {
            Usuario usuario = getUsuarioActual(principal);
            List<DriveItem> registros = driveItemRepository.findByCreadoPor(usuario);
            int actualizados = 0;

            for (DriveItem item : registros) {
                try {
                    // Consultamos a la API de Google Drive
                    var archivoGoogle = driveService.obtenerDetallesArchivo(item.getDriveId(), usuario);
                    
                    // Si el nombre es diferente, lo actualizamos
                    if (!item.getNombreDocumento().equals(archivoGoogle.getName())) {
                        item.setNombreDocumento(archivoGoogle.getName());
                        
                        // También aprovechamos para actualizar el tipo por si acaso
                        String tipo = "application/vnd.google-apps.folder".equals(archivoGoogle.getMimeType()) 
                                    ? "Carpeta" : "Archivo/Documento";
                        item.setTipoDocumento(tipo);
                        
                        driveItemRepository.save(item);
                        actualizados++;
                    }
                } catch (Exception e) {
                    System.err.println("Error al sincronizar archivo " + item.getDriveId() + ": " + e.getMessage());
                    // Si el archivo no existe en Drive (fue borrado), podríamos marcarlo aquí
                }
            }
            
            if (actualizados > 0) {
                redirectAttributes.addFlashAttribute("mensaje", "¡Sincronización completa! Se actualizaron " + actualizados + " nombres.");
            } else {
                redirectAttributes.addFlashAttribute("mensaje", "Todo está al día. No hubo cambios en los nombres.");
            }
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error general al sincronizar: " + e.getMessage());
        }
        return "redirect:/panel";
    }











}