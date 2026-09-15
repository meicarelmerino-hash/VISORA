package com.infotec.drivenotifications.controllers;

import java.security.Principal;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.infotec.drivenotifications.models.Usuario;
import com.infotec.drivenotifications.services.UsuarioService;

@Controller
@RequestMapping("/usuarios")
public class UsuariosController {

    private final UsuarioService usuarioService;

    public UsuariosController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // LISTAR Y BUSCAR
    @GetMapping("/listar")
    public String listarUsuarios(@RequestParam(required = false) String nombre, Model model) {

        List<Usuario> usuarios;

        if (nombre != null && !nombre.isEmpty()) {
            usuarios = usuarioService.buscarPorNombre(nombre);
        } else {
            usuarios = usuarioService.listarTodos();
        }

        model.addAttribute("usuarios", usuarios);
        model.addAttribute("nombre", nombre);

        return "listar_usuarios";
    }

    // MOSTRAR FORMULARIO
    @GetMapping("/nuevo")
    public String mostrarFormulario(Model model) {
        model.addAttribute("usuario", new Usuario());
        return "form_usuario";
    }

    // GUARDAR USUARIO (CON CONTRASEÑA CIFRADA)
    @PostMapping("/guardar")
    public String guardarUsuario(@ModelAttribute Usuario usuario, 
                                @RequestParam(value = "propio", required = false) Boolean propio,
                                jakarta.servlet.http.HttpServletRequest request,
                                Principal principal) throws Exception {

        // 1. Si es mi perfil, verificamos si el nombre cambió ANTES de guardar
        boolean nombreCambio = false;
        if (Boolean.TRUE.equals(propio)) {
            String nombreActualEnSesion = principal.getName();
            // Si el nombre que viene del formulario es distinto al de la sesión...
            if (!nombreActualEnSesion.equals(usuario.getNombre())) {
                nombreCambio = true;
            }
        }

        // 2. Guardamos los cambios (encriptación y persistencia)
        usuarioService.guardarUsuario(usuario);

        // 3. Lógica de redirección basada en el cambio de nombre
        if (Boolean.TRUE.equals(propio)) {
            if (nombreCambio) {
                // El nombre cambió: Cerramos sesión y mandamos al login
                request.logout();
                return "redirect:/login?nameChanged=true";
            } else {
                // Solo cambió contraseña o datos: Se queda en el panel
                return "redirect:/panel?mensaje=Perfil+actualizado";
            }
        }

        // Si no es "propio" (es un admin editando a otro), regresa al listado
        return "redirect:/usuarios/listar";
    }











    @GetMapping("/editar")
    public String editarUsuario(@RequestParam Long id, Model model) {

        Usuario usuario = usuarioService.buscarPorId(id);

        model.addAttribute("usuario", usuario);

        return "form_usuario"; // reutilizamos el mismo formulario
    }
    

    @PostMapping("/eliminar")
    public String eliminarUsuario(@RequestParam Long id) {

        usuarioService.eliminarPorId(id);

        return "redirect:/usuarios/listar";
    }


    // MOSTRAR MI PERFIL (Solo mis datos)
    @GetMapping("/mi-perfil")
    public String verMiPerfil(Model model, java.security.Principal principal) {
        String username = principal.getName();
        // Buscamos el objeto completo para que el th:field lo mapee al formulario
        Usuario usuario = usuarioService.buscarPorNombreExacto(username); 
        model.addAttribute("usuario", usuario);
        return "form_mi_perfil"; 
    }

    // AUTO-ELIMINACIÓN
    @PostMapping("/eliminar-cuenta")
    public String autoEliminar(java.security.Principal principal, jakarta.servlet.http.HttpServletRequest request) throws Exception {
        String username = principal.getName();
        Usuario usuario = usuarioService.buscarPorNombreExacto(username);
        
        // 1. Eliminar de la base de datos
        usuarioService.eliminarPorId(usuario.getId());
        
        // 2. Invalidar la sesión manualmente para que lo saque del sistema de inmediato
        request.logout(); 
        
        return "redirect:/login?deleteSuccess";
    }










    @GetMapping("/configurar-telegram")
    public String mostrarConfigTelegram(Model model, Principal principal) {
        // Buscamos al usuario para pasar sus datos actuales al formulario
        Usuario usuario = usuarioService.buscarPorNombreExacto(principal.getName());
        model.addAttribute("usuarioActivo", usuario);
        
        // Si tienes alertas de éxito/error
        return "config_telegram"; 
    }




    @PostMapping("/guardar-telegram")
    public String guardarTelegram(@RequestParam String telegramToken, 
                                @RequestParam String telegramChatId, 
                                Principal principal, 
                                org.springframework.web.servlet.mvc.support.RedirectAttributes ra) {
        
        // 1. Buscamos al usuario por su nombre actual
        Usuario usuario = usuarioService.buscarPorNombreExacto(principal.getName());
        
        // 2. Seteamos los nuevos valores de Telegram
        usuario.setTelegramToken(telegramToken);
        usuario.setTelegramChatId(telegramChatId);
        
        // 3. ¡IMPORTANTE! Usar el método que NO encripta
        usuarioService.guardarCambiosPerfil(usuario);
        
        ra.addFlashAttribute("mensaje", "¡Configuración de Telegram actualizada!");
        return "redirect:/usuarios/configurar-telegram";
    }









}