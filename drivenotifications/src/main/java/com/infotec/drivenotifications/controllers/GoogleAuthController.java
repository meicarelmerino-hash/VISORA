package com.infotec.drivenotifications.controllers;

import java.io.InputStreamReader;
import java.security.Principal; // Clase corregida
import java.util.Arrays;
import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeRequestUrl;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.infotec.drivenotifications.models.Usuario;
import com.infotec.drivenotifications.repositories.UsuarioRepository;

@Controller
public class GoogleAuthController {

    private final UsuarioRepository usuarioRepository;
    private static final String CLIENT_SECRET_FILE = "/credentials.json";
    
    //private static final String REDIRECT_URI = "https://investigacion-colaborativa-ecosistema-e.infotec.mx/auth/google/callback";
    private static final String REDIRECT_URI = "http://localhost:8080/auth/google/callback";

    public GoogleAuthController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/auth/google/login")
    public String redireccionarAGoogle(Principal principal) throws Exception {
        // 1. Identificar al usuario logueado
        Usuario usuario = usuarioRepository.findByNombre(principal.getName())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // 2. Cargar Client ID y Secret desde el JSON
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(CLIENT_SECRET_FILE)))
        );

        // 3. Construir la URL de autorización usando el flujo de CÓDIGO (Server-side)
        // Esto soluciona el conflicto entre access_type 'offline' y response_type 'token'
        String url = new GoogleAuthorizationCodeRequestUrl(
                clientSecrets.getDetails().getClientId(),
                REDIRECT_URI,
                Arrays.asList(
                    "https://www.googleapis.com/auth/drive",
                    "https://www.googleapis.com/auth/userinfo.email",
                    "https://www.googleapis.com/auth/userinfo.profile"
                )
        )
        .setState(usuario.getId().toString())
        .setAccessType("offline")      // Ahora sí es válido porque el response_type será 'code'
        .setApprovalPrompt("force")    // Fuerza la pantalla de consentimiento para asegurar el Refresh Token
        .build();

        // 4. Redirigir a Google
        return "redirect:" + url;
    }

    @GetMapping("/auth/google/callback")
    public String callbackGoogle(@RequestParam("code") String code, 
                                @RequestParam("state") String usuarioIdStr) throws Exception {
        
        // 1. Cargar secretos
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream(CLIENT_SECRET_FILE)))
        );

        // 2. Intercambiar el código recibido por los tokens (Access y Refresh)
        TokenResponse response = new GoogleAuthorizationCodeTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                "https://oauth2.googleapis.com/token",
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret(),
                code,
                REDIRECT_URI)
                .execute();

        // 3. Recuperar al usuario de la base de datos
        Long usuarioId = Long.parseLong(usuarioIdStr);
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado en el callback"));

        // 4. Guardar los nuevos datos de sesión de Google
        usuario.setAccessToken(response.getAccessToken());
        
        // El Refresh Token solo llega en la primera vinculación o si usamos ApprovalPrompt("force")
        if (response.getRefreshToken() != null) {
            usuario.setRefreshToken(response.getRefreshToken());
        }

        // Calcular tiempo de expiración
        long expiracion = System.currentTimeMillis() + (response.getExpiresInSeconds() * 1000);
        usuario.setExpirationTimeMilliseconds(expiracion);

        usuarioRepository.save(usuario);

        return "redirect:/panel?mensaje=Google Drive vinculado correctamente";
    }
}