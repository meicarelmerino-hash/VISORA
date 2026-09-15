package com.infotec.drivenotifications.models;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table; // <--- AGREGA ESTA LÍNEA

@Entity
@Table(name = "drive_items")
public class DriveItem {

    public DriveItem(){} // Constructor

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "drive_id", nullable = false)
    private String driveId;

    @Column(name = "nombre_docuemnto", nullable = false)
    private String nombreDocumento;

    @Column(name = "tipo_documento", nullable = false)
    private String tipoDocumento;

    // --- ESTA ES LA LÍNEA QUE FALTABA ---
    @Column(name = "monitoreando")
    private Boolean monitoreando = false;

    @ManyToOne
    @JoinColumn(name = "creado_por")
    private Usuario creadoPor;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();






    // Dentro de la clase DriveItem
    @OneToMany(mappedBy = "driveItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HistorialCambioDocumento> historial = new ArrayList<>();

    // Getter y Setter
    public List<HistorialCambioDocumento> getHistorial() { return historial; }
    public void setHistorial(List<HistorialCambioDocumento> historial) { this.historial = historial; }

    // Métodos Getter y Setter para la nueva variable
    public boolean isMonitoreando() { 
        return monitoreando; 
    }

    public void setMonitoreando(boolean monitoreando) { 
        this.monitoreando = monitoreando; 
    }

    // --- El resto de tus Getters y Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getDriveId() { return driveId; }
    public void setDriveId(String driveId) { this.driveId = driveId; }

    public String getNombreDocumento() { return nombreDocumento; }
    public void setNombreDocumento(String nombreDocumento) { this.nombreDocumento = nombreDocumento; }

    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String tipoDocumento) { this.tipoDocumento = tipoDocumento; }

    public Usuario getCreadoPor() { return creadoPor; }
    public void setCreadoPor(Usuario creadoPor) { this.creadoPor = creadoPor; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }


    public Boolean getMonitoreando() { 
        return monitoreando != null && monitoreando; // Protección extra contra nulos
    }

    public void setMonitoreando(Boolean monitoreando) { 
        this.monitoreando = monitoreando; 
    }
}