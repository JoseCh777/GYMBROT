package org.gymbrot.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class SesionGymbrot {

    private int idSesion;
    private String idCliente;
    private LocalDate fecha;
    private LocalDateTime horaInicio;
    private LocalDateTime horaFin;
    private String contextoActivo;

    public SesionGymbrot() {}

    public SesionGymbrot(int idSesion, String idCliente, LocalDate fecha,
                         LocalDateTime horaInicio, LocalDateTime horaFin, String contextoActivo) {
        this.idSesion = idSesion;
        this.idCliente = idCliente;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.contextoActivo = contextoActivo;
    }

    public int getIdSesion() { return idSesion; }
    public void setIdSesion(int idSesion) { this.idSesion = idSesion; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalDateTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalDateTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalDateTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalDateTime horaFin) { this.horaFin = horaFin; }

    public String getContextoActivo() { return contextoActivo; }
    public void setContextoActivo(String contextoActivo) { this.contextoActivo = contextoActivo; }

    @Override
    public String toString() {
        return "SesionGymbrot{" +
                "idSesion=" + idSesion +
                ", idCliente='" + idCliente + '\'' +
                ", fecha=" + fecha +
                ", horaInicio=" + horaInicio +
                ", horaFin=" + horaFin +
                '}';
    }
}
