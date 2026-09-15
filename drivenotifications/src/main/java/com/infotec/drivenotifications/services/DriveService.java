package com.infotec.drivenotifications.services;

import java.io.InputStreamReader;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.drive.model.PermissionList;
import com.infotec.drivenotifications.models.Usuario;
import com.infotec.drivenotifications.repositories.UsuarioRepository;

@Service
public class DriveService {

    private static final String APPLICATION_NAME = "Infotec-drive";
    private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private final UsuarioRepository usuarioRepository;

    public DriveService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * MÉTODO CLAVE: Crea el servicio de Drive usando los tokens de la BD.
     * Si el token expiró, lo refresca automáticamente.
     */
    public Drive getDriveService(Usuario usuario) throws Exception {
        long margenSeguridad = 300000; // 5 minutos
        
        if (usuario.getExpirationTimeMilliseconds() <= System.currentTimeMillis() + margenSeguridad) {
            if (usuario.getRefreshToken() != null) {
                refrescarAccessToken(usuario);
            } else {
                throw new RuntimeException("Token expirado y no hay Refresh Token para: " + usuario.getNombre());
            }
        }

        HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        
        return new Drive.Builder(httpTransport, JSON_FACTORY, null)
                .setApplicationName(APPLICATION_NAME)
                .setHttpRequestInitializer(request -> {
                    request.getHeaders().setAuthorization("Bearer " + usuario.getAccessToken());
                })
                .build();
    }

    public String refrescarAccessToken(Usuario usuario) throws Exception {
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                JSON_FACTORY,
                new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("/credentials.json")))
        );

        TokenResponse response = new GoogleRefreshTokenRequest(
                new NetHttpTransport(),
                JSON_FACTORY,
                usuario.getRefreshToken(),
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret()
        ).execute();

        usuario.setAccessToken(response.getAccessToken());
        long nuevaExpiracion = System.currentTimeMillis() + (response.getExpiresInSeconds() * 1000);
        usuario.setExpirationTimeMilliseconds(nuevaExpiracion);

        usuarioRepository.save(usuario);
        System.out.println(">>> Token refrescado automáticamente para: " + usuario.getNombre());
        return response.getAccessToken();
    }

    // --- MÉTODOS DE NEGOCIO ACTUALIZADOS PARA USAR EL USUARIO ---

    public com.google.api.services.drive.model.File obtenerDetallesArchivo(String fileId, Usuario usuario) throws Exception {
        Drive service = getDriveService(usuario);
        return service.files().get(fileId)
                .setFields("id, name, mimeType")
                .execute();
    }

    public List<Permission> obtenerPermisos(String fileId, Usuario usuario) throws Exception {
        Drive service = getDriveService(usuario);
        PermissionList permissionList = service.permissions()
                .list(fileId)
                .setFields("permissions(id,emailAddress,role,type)")
                .execute();
        return permissionList.getPermissions();
    }

    public void actualizarPermiso(String fileId, String correo, String nuevoRol, Usuario usuario) throws Exception {
        Drive service = getDriveService(usuario);
        
        PermissionList permissionList = service.permissions().list(fileId)
                .setFields("permissions(id,emailAddress)")
                .execute();

        String permissionId = permissionList.getPermissions().stream()
                .filter(p -> correo.equalsIgnoreCase(p.getEmailAddress()))
                .map(Permission::getId)
                .findFirst()
                .orElseThrow(() -> new Exception("El correo no tiene permiso sobre este archivo."));

        Permission updatedPermission = new Permission();
        updatedPermission.setRole(nuevoRol);

        service.permissions().update(fileId, permissionId, updatedPermission).execute();
    }

    public void eliminarAccesoPublico(String fileId, Usuario usuario) throws Exception {
        Drive service = getDriveService(usuario);
        List<Permission> permisos = obtenerPermisos(fileId, usuario);

        for (Permission p : permisos) {
            if ("anyone".equalsIgnoreCase(p.getType())) {
                service.permissions().delete(fileId, p.getId()).execute();
                return;
            }
        }
        throw new Exception("El archivo no tiene acceso público activo.");
    }



    public void compartirArchivo(String fileId, String emails, String role, Usuario usuario) throws Exception {
        Drive service = getDriveService(usuario);
        
        // Separamos los correos por coma y quitamos espacios en blanco
        String[] listaCorreos = emails.split(",");
        
        for (String correo : listaCorreos) {
            String correoLimpio = correo.trim();
            if (correoLimpio.isEmpty()) continue;

            Permission newPermission = new Permission();
            newPermission.setType("user");
            newPermission.setRole(role); // reader, writer o commenter
            newPermission.setEmailAddress(correoLimpio);

            // setSendNotificationEmail(true) hace que Google les envíe el correo de aviso
            service.permissions().create(fileId, newPermission)
                    .setSendNotificationEmail(true) 
                    .execute();
        }
    }








}