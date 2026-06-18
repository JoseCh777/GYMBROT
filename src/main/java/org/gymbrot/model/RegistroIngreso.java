package org.gymbrot.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class RegistroIngreso {

    private int idIngreso;
    private String idCliente;
    private LocalDate fecha;
    private LocalDateTime horaEntrada;
    private LocalDateTime horaSalida;
    private String metodoVerificacion;
    private String estadoVerificacion;

    public RegistroIngreso() {}

    public RegistroIngreso(int idIngreso, String idCliente, LocalDate fecha,
                           LocalDateTime horaEntrada, LocalDateTime horaSalida,
                           String metodoVerificacion, String estadoVerificacion) {
        this.idIngreso = idIngreso;
        this.idCliente = idCliente;
        this.fecha = fecha;
        this.horaEntrada = horaEntrada;
        this.horaSalida = horaSalida;
        this.metodoVerificacion = metodoVerificacion;
        this.estadoVerificacion = estadoVerificacion;
    }

    public int getIdIngreso() { return idIngreso; }
    public void setIdIngreso(int idIngreso) { this.idIngreso = idIngreso; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalDateTime getHoraEntrada() { return horaEntrada; }
    public void setHoraEntrada(LocalDateTime horaEntrada) { this.horaEntrada = horaEntrada; }

    public LocalDateTime getHoraSalida() { return horaSalida; }
    public void setHoraSalida(LocalDateTime horaSalida) { this.horaSalida = horaSalida; }

    public String getMetodoVerificacion() { return metodoVerificacion; }
    public void setMetodoVerificacion(String metodoVerificacion) { this.metodoVerificacion = metodoVerificacion; }

    public String getEstadoVerificacion() { return estadoVerificacion; }
    public void setEstadoVerificacion(String estadoVerificacion) { this.estadoVerificacion = estadoVerificacion; }

    @Override
    public String toString() {
        return "RegistroIngreso{" +
                "idIngreso=" + idIngreso +
                ", idCliente='" + idCliente + '\'' +
                ", fecha=" + fecha +
                ", horaEntrada=" + horaEntrada +
                ", horaSalida=" + horaSalida +
                ", metodoVerificacion='" + metodoVerificacion + '\'' +
                ", estadoVerificacion='" + estadoVerificacion + '\'' +
                '}';
    }
}
