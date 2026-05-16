package org.gymbrot.model;

import java.time.LocalDate;

public class HistorialMembresia {

    private int idHistorial;
    private String idCliente;
    private int idMembresia;
    private LocalDate fechaAsignacion;
    private LocalDate fechaFin;
    private boolean activa;

    public HistorialMembresia() {}

    public HistorialMembresia(int idHistorial, String idCliente, int idMembresia,
                              LocalDate fechaAsignacion, LocalDate fechaFin, boolean activa) {
        this.idHistorial = idHistorial;
        this.idCliente = idCliente;
        this.idMembresia = idMembresia;
        this.fechaAsignacion = fechaAsignacion;
        this.fechaFin = fechaFin;
        this.activa = activa;
    }

    public int getIdHistorial() { return idHistorial; }
    public void setIdHistorial(int idHistorial) { this.idHistorial = idHistorial; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public int getIdMembresia() { return idMembresia; }
    public void setIdMembresia(int idMembresia) { this.idMembresia = idMembresia; }

    public LocalDate getFechaAsignacion() { return fechaAsignacion; }
    public void setFechaAsignacion(LocalDate fechaAsignacion) { this.fechaAsignacion = fechaAsignacion; }

    public LocalDate getFechaFin() { return fechaFin; }
    public void setFechaFin(LocalDate fechaFin) { this.fechaFin = fechaFin; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    @Override
    public String toString() {
        return "HistorialMembresia{" +
                "idHistorial=" + idHistorial +
                ", idCliente='" + idCliente + '\'' +
                ", idMembresia=" + idMembresia +
                ", fechaAsignacion=" + fechaAsignacion +
                ", fechaFin=" + fechaFin +
                ", activa=" + activa +
                '}';
    }
}
