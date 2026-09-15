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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "permisos",
    uniqueConstraints = @UniqueConstraint(columnNames = {
        "drive_item_id", "correo_id", "tipo_permiso"
    })
)
public class Permiso {

    public Permiso(){} //Constructor

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "drive_item_id")
    private DriveItem driveItem;

    @ManyToOne
    @JoinColumn(name = "correo_id")
    private Correo correo;

    @Column(name = "tipo_permiso", nullable = false)
    private String tipoPermiso;

    @Column(nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_asignacion")
    private LocalDateTime fechaAsignacion = LocalDateTime.now();

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

    public Correo getCorreo() {
        return correo;
    }

    public void setCorreo(Correo correo) {
        this.correo = correo;
    }

    public String getTipoPermiso() {
        return tipoPermiso;
    }

    public void setTipoPermiso(String tipoPermiso) {
        this.tipoPermiso = tipoPermiso;
    }

    public Boolean getActivo() {
        return activo;
    }

    public void setActivo(Boolean activo) {
        this.activo = activo;
    }

    public LocalDateTime getFechaAsignacion() {
        return fechaAsignacion;
    }

    public void setFechaAsignacion(LocalDateTime fechaAsignacion) {
        this.fechaAsignacion = fechaAsignacion;
    }

    // Getters y Setters

    
}