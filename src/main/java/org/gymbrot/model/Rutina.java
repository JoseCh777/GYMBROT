package org.gymbrot.model;

import java.time.LocalDate;

public class Rutina {

    private int idRutina;
    private String idInstructor;
    private String idCliente;
    private String nombre;
    private String descripcion;
    private LocalDate fechaCreacion;
    private LocalDate fechaFin;
    private String diasSemana;
    private String objetivo;

    public Rutina() {}

    public Rutina(int idRutina, String idInstructor, String idCliente, String nombre,
                  String descripcion, LocalDate fechaCreacion, LocalDate fechaFin,
                  String diasSemana, String objetivo) {
        this.idRutina = idRutina;
        this.idInstructor = idInstructor;
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.fechaCreacion = fechaCreacion;
        this.fechaFin = fechaFin;
        this.diasSemana = diasSemana;
        this.objetivo = objetivo;
    }

    public int getIdRutina() { return idRutina; }
    public void setIdRutina(int idRutina) { this.idRutina = idRutina; }

    public String getIdInstructor() { return idInstructor; }
    public void setIdInstructor(String idInstructor) { this.idInstructor = idInstructor; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public LocalDate getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDate fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public String getDiasSemana() { return diasSemana; }
    public void setDiasSemana(String diasSemana) { this.diasSemana = diasSemana; }

    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }

    @Override
    public String toString() {
        return "Rutina{" +
                "idRutina=" + idRutina +
                ", idCliente='" + idCliente + '\'' +
                ", idInstructor='" + idInstructor + '\'' +
                ", nombre='" + nombre + '\'' +
                ", objetivo='" + objetivo + '\'' +
                ", fechaCreacion=" + fechaCreacion +
                ", fechaFin=" + fechaFin +
                '}';
    }
}