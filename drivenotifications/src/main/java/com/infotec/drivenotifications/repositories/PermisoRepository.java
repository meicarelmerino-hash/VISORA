package com.infotec.drivenotifications.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.infotec.drivenotifications.models.Correo;
import com.infotec.drivenotifications.models.DriveItem;
import com.infotec.drivenotifications.models.Permiso;

public interface PermisoRepository extends JpaRepository<Permiso, Long> {

    List<Permiso> findByCorreoAndActivoTrue(Correo correo);

    List<Permiso> findByDriveItemAndActivoTrue(DriveItem driveItem);

    List<Permiso> findByCorreoAndDriveItemAndActivoTrue(Correo correo, DriveItem driveItem);

}