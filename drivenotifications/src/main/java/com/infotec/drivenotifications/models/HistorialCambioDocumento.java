package com.infotec.drivenotifications.models;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "historial_cambios_documentos")
public class HistorialCambioDocumento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "drive_item_id", nullable = false)
    private DriveItem driveItem;

    @Column(name = "autor_cambio")
    private String autorCambio;

    @Column(name = "fecha_cambio")
    private LocalDateTime fechaCambio = LocalDateTime.now();

    // --- CAMPOS CRÍTICOS PARA TU PROMPT DE IA ---

    @Column(name = "texto_original", columnDefinition = "TEXT")
    private String textoOriginal; // El "Antes"

    @Column(name = "texto_modificado", columnDefinition = "TEXT")
    private String textoModificado; // El "Después"

    @Column(name = "analisis_ia", columnDefinition = "TEXT")
    private String analisisIa; // Aquí guardarás el resultado del prompt (Descripción)

    @Column(name = "prioridad")
    private String prioridad; // BAJA, MEDIA o ALTA




    public HistorialCambioDocumento() {}

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DriveItem getDriveItem() {
        return driveItem;
    }

    public void setDriveItem(DriveItem driveItem) {
        this.driveItem = driveItem;
    }

    public String getAutorCambio() {
        return autorCambio;
    }

    public void setAutorCambio(String autorCambio) {
        this.autorCambio = autorCambio;
    }

    public LocalDateTime getFechaCambio() {
        return fechaCambio;
    }

    public void setFechaCambio(LocalDateTime fechaCambio) {
        this.fechaCambio = fechaCambio;
    }

    public String getTextoOriginal() {
        return textoOriginal;
    }

    public void setTextoOriginal(String textoOriginal) {
        this.textoOriginal = textoOriginal;
    }

    public String getTextoModificado() {
        return textoModificado;
    }

    public void setTextoModificado(String textoModificado) {
        this.textoModificado = textoModificado;
    }

    public String getAnalisisIa() {
        return analisisIa;
    }

    public void setAnalisisIa(String analisisIa) {
        this.analisisIa = analisisIa;
    }

    public String getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(String prioridad) {
        this.prioridad = prioridad;
    }

    



    
}