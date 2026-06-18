package org.gymbrot.model;

import java.time.LocalDate;
import java.time.LocalTime;

public class Cita {

    private int idCita;
    private String idInstructor;
    private String idCliente;
    private LocalDate fecha;
    private LocalTime hora;
    private String tipoCita;
    private String estado;
    private String notas;

    public Cita() {}

    public Cita(int idCita, String idInstructor, String idCliente, LocalDate fecha,
                LocalTime hora, String tipoCita, String estado, String notas) {
        this.idCita = idCita;
        this.idInstructor = idInstructor;
        this.idCliente = idCliente;
        this.fecha = fecha;
        this.hora = hora;
        this.tipoCita = tipoCita;
        this.estado = estado;
        this.notas = notas;
    }

    public int getIdCita() { return idCita; }
    public void setIdCita(int idCita) { this.idCita = idCita; }

    public String getIdInstructor() { return idInstructor; }
    public void setIdInstructor(String idInstructor) { this.idInstructor = idInstructor; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHora() { return hora; }
    public void setHora(LocalTime hora) { this.hora = hora; }

    public String getTipoCita() { return tipoCita; }
    public void setTipoCita(String tipoCita) { this.tipoCita = tipoCita; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }

    @Override
    public String toString() {
        return "Cita{" +
                "idCita=" + idCita +
                ", idInstructor='" + idInstructor + '\'' +
                ", idCliente='" + idCliente + '\'' +
                ", fecha=" + fecha +
                ", hora=" + hora +
                ", tipoCita='" + tipoCita + '\'' +
                ", estado='" + estado + '\'' +
                '}';
    }
}
