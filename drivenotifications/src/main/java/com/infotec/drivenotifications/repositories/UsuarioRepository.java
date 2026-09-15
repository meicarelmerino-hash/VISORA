
package com.infotec.drivenotifications.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.infotec.drivenotifications.models.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByNombre(String nombre);
    List<Usuario> findByNombreContainingIgnoreCase(String nombre);


}