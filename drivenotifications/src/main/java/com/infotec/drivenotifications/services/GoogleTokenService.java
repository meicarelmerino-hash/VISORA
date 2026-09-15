package com.infotec.drivenotifications.services;

import java.io.InputStreamReader;

import org.springframework.stereotype.Service;

import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.auth.oauth2.GoogleRefreshTokenRequest;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.infotec.drivenotifications.models.Usuario;
import com.infotec.drivenotifications.repositories.UsuarioRepository;

@Service
public class GoogleTokenService {

    private final UsuarioRepository usuarioRepository;
    private static final String CLIENT_SECRET_FILE = "/credentials.json";

    public GoogleTokenService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public String refrescarTokenSiEsNecesario(Usuario usuario) throws Exception {
        // 1. Revisar si el token actual sigue siendo válido (con un margen de 1 minuto)
        long ahora = System.currentTimeMillis();
        if (usuario.getExpirationTimeMilliseconds() != null && ahora < (usuario.getExpirationTimeMilliseconds() - 60000)) {
            return usuario.getAccessToken(); // Sigue siendo válido, no hacemos nada
        }

        System.out.println("Renovando token para el usuario: " + usuario.getNombre());

        // 2. Si expiró, cargar credenciales de la app
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(
                GsonFactory.getDefaultInstance(),
                new InputStreamReader(getClass().getResourceAsStream(CLIENT_SECRET_FILE))
        );

        // 3. Pedir nuevo token a Google usando el Refresh Token
        TokenResponse response = new GoogleRefreshTokenRequest(
                new NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                usuario.getRefreshToken(),
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret()
        ).execute();

        // 4. Actualizar al usuario en la base de datos con los nuevos datos
        usuario.setAccessToken(response.getAccessToken());
        long nuevaExpiracion = System.currentTimeMillis() + (response.getExpiresInSeconds() * 1000);
        usuario.setExpirationTimeMilliseconds(nuevaExpiracion);

        usuarioRepository.save(usuario);

        return response.getAccessToken();
    }
}