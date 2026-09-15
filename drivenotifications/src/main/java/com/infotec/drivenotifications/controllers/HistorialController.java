package com.infotec.drivenotifications.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.infotec.drivenotifications.models.DriveItem;
import com.infotec.drivenotifications.models.HistorialCambioDocumento;
import com.infotec.drivenotifications.models.Usuario;
import com.infotec.drivenotifications.repositories.DriveItemRepository;
import com.infotec.drivenotifications.repositories.HistorialCambioDocumentoRepository;
import com.infotec.drivenotifications.repositories.UsuarioRepository;

@Controller
public class HistorialController {

    private final HistorialCambioDocumentoRepository historialRepository;
    private final DriveItemRepository driveItemRepository;
    private final UsuarioRepository usuarioRepository;

    public HistorialController(HistorialCambioDocumentoRepository historialRepository, 
                               DriveItemRepository driveItemRepository, 
                               UsuarioRepository usuarioRepository) {
        this.historialRepository = historialRepository;
        this.driveItemRepository = driveItemRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/historial-alertas")
    public String listarHistorialGeneral(Model model, Principal principal) {
        Usuario usuario = usuarioRepository.findByNombre(principal.getName()).orElseThrow();
        
        // Obtenemos todos los archivos que posee el usuario
        List<DriveItem> misArchivos = driveItemRepository.findByCreadoPor(usuario);
        
        model.addAttribute("usuarioActivo", usuario);
        model.addAttribute("misArchivos", misArchivos);
        return "historial-alertas"; // Nombre de la nueva plantilla
    }

    @GetMapping("/historial-alertas/detalles")
    public String verDetallesArchivo(@RequestParam("fileId") String fileId, Model model, Principal principal) {
        Usuario usuario = usuarioRepository.findByNombre(principal.getName()).orElseThrow();
        
        DriveItem item = driveItemRepository.findByDriveIdAndCreadoPor(fileId, usuario)
                .orElseThrow(() -> new RuntimeException("No tienes acceso a este recurso"));

        List<HistorialCambioDocumento> cambios = historialRepository.findByDriveItemOrderByFechaCambioDesc(item);

        model.addAttribute("usuarioActivo", usuario);
        model.addAttribute("misArchivos", driveItemRepository.findByCreadoPor(usuario));
        model.addAttribute("itemSeleccionado", item);
        model.addAttribute("cambios", cambios);
        
        return "historial-alertas";
    }
}