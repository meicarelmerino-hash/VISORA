package com.infotec.drivenotifications.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.infotec.drivenotifications.models.DriveItem;
import com.infotec.drivenotifications.models.Usuario;

public interface DriveItemRepository extends JpaRepository<DriveItem, Long> {

 
    Optional<DriveItem> findByDriveIdAndCreadoPor(String driveId, Usuario usuario);


    List<DriveItem> findByCreadoPor(Usuario usuario);

    DriveItem findByDriveId(String driveId);

}