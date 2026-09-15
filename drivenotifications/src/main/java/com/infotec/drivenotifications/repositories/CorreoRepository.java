package com.infotec.drivenotifications.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.infotec.drivenotifications.models.Correo;

public interface CorreoRepository extends JpaRepository<Correo, Long> {

    Optional<Correo> findByEmail(String email);

}