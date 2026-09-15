package com.infotec.drivenotifications.config;

import java.util.List;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.infotec.drivenotifications.models.DriveItem;
import com.infotec.drivenotifications.repositories.DriveItemRepository;
import com.infotec.drivenotifications.services.DriveMonitorService;

@Component
public class StartupMonitor {

    private final DriveItemRepository driveItemRepository;
    private final DriveMonitorService driveMonitorService;

    public StartupMonitor(DriveItemRepository driveItemRepository, DriveMonitorService driveMonitorService) {
        this.driveItemRepository = driveItemRepository;
        this.driveMonitorService = driveMonitorService;
    }

    // Este método se dispara solito cuando la app termina de arrancar
    @EventListener(ApplicationReadyEvent.class)
    public void reiniciarMonitoreosAlArrancar() {
        System.out.println(">>> Detectado reinicio de servidor. Reestableciendo hilos de monitoreo...");
        
        // 1. Buscar todos los archivos que en BD dicen estar activos
        List<DriveItem> itemsActivos = driveItemRepository.findAll().stream()
                .filter(item -> item.getMonitoreando() != null && item.getMonitoreando())
                .toList();

        // 2. Por cada uno, llamar al servicio para que inicie el hilo de nuevo
        for (DriveItem item : itemsActivos) {
            try {
                // Importante: Asegúrate de que tu DriveMonitorService.iniciarMonitoreo 
                // pueda manejar el arranque sin necesidad de una acción manual
                driveMonitorService.iniciarMonitoreo(item.getDriveId(), item.getCreadoPor());
                System.out.println(">>> Monitoreo reactivado para: " + item.getNombreDocumento());
            } catch (Exception e) {
                System.err.println(">>> No se pudo reactivar el monitoreo de: " + item.getNombreDocumento());
            }
        }
        System.out.println(">>> Proceso de reestablecimiento finalizado.");
    }
}