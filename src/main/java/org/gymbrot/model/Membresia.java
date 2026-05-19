package org.gymbrot.model;


import java.time.LocalDate;

public class Membresia {

    private int idMembresia;
    private int idPlan;
    private String tipoMembresia;
    private String modalidadPago;
    private double valor;
    private LocalDate fechaInicio;
    private LocalDate fechaVencimiento;
    private String estado;

    public Membresia() {}

    public Membresia(int idMembresia, int idPlan, String tipoMembresia, String modalidadPago,
                     double valor, LocalDate fechaInicio, LocalDate fechaVencimiento, String estado) {
        this.idMembresia = idMembresia;
        this.idPlan = idPlan;
        this.tipoMembresia = tipoMembresia;
        this.modalidadPago = modalidadPago;
        this.valor = valor;
        this.fechaInicio = fechaInicio;
        this.fechaVencimiento = fechaVencimiento;
        this.estado = estado;
    }

    public int getIdMembresia() { return idMembresia; }
    public void setIdMembresia(int idMembresia) { this.idMembresia = idMembresia; }

    public int getIdPlan() { return idPlan; }
    public void setIdPlan(int idPlan) { this.idPlan = idPlan; }

    public String getTipoMembresia() { return tipoMembresia; }
    public void setTipoMembresia(String tipoMembresia) { this.tipoMembresia = tipoMembresia; }

    public String getModalidadPago() { return modalidadPago; }
    public void setModalidadPago(String modalidadPago) { this.modalidadPago = modalidadPago; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public LocalDate getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(LocalDate fechaInicio) { this.fechaInicio = fechaInicio; }

    public LocalDate getFechaVencimiento() { return fechaVencimiento; }
    public void setFechaVencimiento(LocalDate fechaVencimiento) { this.fechaVencimiento = fechaVencimiento; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public String toString() {
        return "Membresia{" +
                "idMembresia=" + idMembresia +
                ", idPlan=" + idPlan +
                ", tipoMembresia='" + tipoMembresia + '\'' +
                ", modalidadPago='" + modalidadPago + '\'' +
                ", valor=" + valor +
                ", fechaInicio=" + fechaInicio +
                ", fechaVencimiento=" + fechaVencimiento +
                ", estado='" + estado + '\'' +
                '}';
    }
}
