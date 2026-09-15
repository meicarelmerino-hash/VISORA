package com.infotec.drivenotifications.models;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany; // Para corregir el error de la lista
import jakarta.persistence.Table; // Este corrige el error de "CascadeType cannot be resolved"

@Entity
@Table(name = "usuarios")
public class Usuario {

    public Usuario(){} //Constructor

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 100)
    private String puesto;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();


    // En Usuario.java
    @Column(unique = true)
    private String email; // El correo de Google (indispensable)




    // ANTES: mappedBy = "usuario"
    // AHORA: mappedBy = "creadoPor"
    @OneToMany(mappedBy = "creadoPor", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DriveItem> items;

    // Dentro de tu clase Usuario.java añade estos campos:

    @Column(name = "telegram_token", length = 255, nullable = true)
    private String telegramToken;

    @Column(name = "telegram_chat_id", length = 100, nullable = true)
    private String telegramChatId;

    // --- AÑADE SUS GETTERS Y SETTERS ---

    public String getTelegramToken() { return telegramToken; }
    public void setTelegramToken(String telegramToken) { this.telegramToken = telegramToken; }

    public String getTelegramChatId() { return telegramChatId; }
    public void setTelegramChatId(String telegramChatId) { this.telegramChatId = telegramChatId; }






    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public Long getExpirationTimeMilliseconds() {
        return expirationTimeMilliseconds;
    }

    public void setExpirationTimeMilliseconds(Long expirationTimeMilliseconds) {
        this.expirationTimeMilliseconds = expirationTimeMilliseconds;
    }

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(columnDefinition = "TEXT")
    private String refreshToken;

    @Column(name = "token_expiration")
    private Long expirationTimeMilliseconds;




    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPuesto() {
        return puesto;
    }

    public void setPuesto(String puesto) {
        this.puesto = puesto;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
    public List<DriveItem> getItems() {
        return items;
    }
    public void setItems(List<DriveItem> items) {
        this.items = items;
    }

    // Getters y Setters
    
}