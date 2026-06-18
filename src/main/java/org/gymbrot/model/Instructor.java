package org.gymbrot.model;

import java.time.LocalDate;

public class Instructor extends Usuario {

    private int idEspecialidad;
    private String disponibilidad;
    private LocalDate fechaContratacion;

    public Instructor() {
        super();
    }

    public Instructor(String numeroIdentificacion, String tipoIdentificacion, String nombre,
                      String apellidos, String telefono, String correo, String contrasenaHash,
                      String fotoUrl, String estado, LocalDate fechaRegistro, String tipoUsuario,
                      int idEspecialidad, String disponibilidad, LocalDate fechaContratacion) {
        super(numeroIdentificacion, tipoIdentificacion, nombre, apellidos, telefono, correo,
                contrasenaHash, fotoUrl, estado, fechaRegistro, tipoUsuario);
        this.idEspecialidad = idEspecialidad;
        this.disponibilidad = disponibilidad;
        this.fechaContratacion = fechaContratacion;
    }

    public int getIdEspecialidad() { return idEspecialidad; }
    public void setIdEspecialidad(int idEspecialidad) { this.idEspecialidad = idEspecialidad; }

    public String getDisponibilidad() { return disponibilidad; }
    public void setDisponibilidad(String disponibilidad) { this.disponibilidad = disponibilidad; }

    public LocalDate getFechaContratacion() { return fechaContratacion; }
    public void setFechaContratacion(LocalDate fechaContratacion) { this.fechaContratacion = fechaContratacion; }

    @Override
    public String toString() {
        return "Instructor{" +
                "numeroIdentificacion='" + getNumeroIdentificacion() + '\'' +
                ", nombre='" + getNombre() + '\'' +
                ", apellidos='" + getApellidos() + '\'' +
                ", idEspecialidad=" + idEspecialidad +
                ", disponibilidad='" + disponibilidad + '\'' +
                ", fechaContratacion=" + fechaContratacion +
                '}';
    }
}
