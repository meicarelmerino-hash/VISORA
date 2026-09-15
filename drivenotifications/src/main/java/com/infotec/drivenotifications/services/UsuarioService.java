package com.infotec.drivenotifications.services;

import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.infotec.drivenotifications.models.Usuario;
import com.infotec.drivenotifications.repositories.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    public List<Usuario> buscarPorNombre(String nombre) {
        return usuarioRepository.findByNombreContainingIgnoreCase(nombre);
    }
    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public void guardarUsuario(Usuario usuario) {
        if (usuario.getId() != null) {
            Usuario usuarioExistente = usuarioRepository.findById(usuario.getId()).orElseThrow();
            
            // PREVENCIÓN: Si la lista de items viene nula del formulario, 
            // le reasignamos los que ya tenía en la BD para que JPA no intente borrarlos.
            if (usuario.getItems() == null) {
                usuario.setItems(usuarioExistente.getItems());
            }

            if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
                usuario.setPassword(usuarioExistente.getPassword());
            } else {
                usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
            }
        } else {
            usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        }
        usuarioRepository.save(usuario);
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }

    public void eliminarPorId(Long id) {
        usuarioRepository.deleteById(id);
    }

    public Usuario buscarPorNombreExacto(String nombre) {
        return usuarioRepository.findByNombre(nombre)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
    }


// En UsuarioService.java
    public void actualizarDatos(Usuario usuario) {
        // Esto guarda el objeto tal cual, sin re-encriptar nada
        usuarioRepository.save(usuario);
    }







    // En UsuarioService.java
    public void guardarCambiosPerfil(Usuario usuario) {
        // Usamos directamente el repositorio para evitar la lógica del service
        usuarioRepository.save(usuario);
    }


}