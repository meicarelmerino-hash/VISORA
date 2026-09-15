package com.infotec.drivenotifications.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.infotec.drivenotifications.models.DriveItem;
import com.infotec.drivenotifications.models.HistorialCambioDocumento;

public interface HistorialCambioDocumentoRepository extends JpaRepository<HistorialCambioDocumento, Long> {
    
    List<HistorialCambioDocumento> findByDriveItem(DriveItem driveItem);
    
    // CORREGIDO: Ahora coincide exactamente con la variable 'fechaCambio' de tu modelo
    Optional<HistorialCambioDocumento> findFirstByDriveItemOrderByFechaCambioDesc(DriveItem driveItem);

    List<HistorialCambioDocumento> findByDriveItemOrderByFechaCambioDesc(DriveItem driveItem);

   

}