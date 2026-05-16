package org.gymbrot.model;

import java.time.LocalDate;

public class Pago {

    private int idPago;
    private int idMembresia;
    private String idCliente;
    private LocalDate fechaPago;
    private double valor;
    private String metodoPago;
    private String estadoPago;
    private String referenciaTransaccion;
    private String observaciones;

    public Pago() {}

    public Pago(int idPago, int idMembresia, String idCliente, LocalDate fechaPago, double valor,
                String metodoPago, String estadoPago, String referenciaTransaccion, String observaciones) {
        this.idPago = idPago;
        this.idMembresia = idMembresia;
        this.idCliente = idCliente;
        this.fechaPago = fechaPago;
        this.valor = valor;
        this.metodoPago = metodoPago;
        this.estadoPago = estadoPago;
        this.referenciaTransaccion = referenciaTransaccion;
        this.observaciones = observaciones;
    }

    public int getIdPago() { return idPago; }
    public void setIdPago(int idPago) { this.idPago = idPago; }

    public int getIdMembresia() { return idMembresia; }
    public void setIdMembresia(int idMembresia) { this.idMembresia = idMembresia; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public LocalDate getFechaPago() { return fechaPago; }
    public void setFechaPago(LocalDate fechaPago) { this.fechaPago = fechaPago; }

    public double getValor() { return valor; }
    public void setValor(double valor) { this.valor = valor; }

    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }

    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String estadoPago) { this.estadoPago = estadoPago; }

    public String getReferenciaTransaccion() { return referenciaTransaccion; }
    public void setReferenciaTransaccion(String referenciaTransaccion) { this.referenciaTransaccion = referenciaTransaccion; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    @Override
    public String toString() {
        return "Pago{" +
                "idPago=" + idPago +
                ", idMembresia=" + idMembresia +
                ", idCliente='" + idCliente + '\'' +
                ", fechaPago=" + fechaPago +
                ", valor=" + valor +
                ", metodoPago='" + metodoPago + '\'' +
                ", estadoPago='" + estadoPago + '\'' +
                ", referenciaTransaccion='" + referenciaTransaccion + '\'' +
                '}';
    }
}
