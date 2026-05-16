package org.gymbrot.model;

import java.time.LocalDateTime;

public class Notificacion {

    private int idNotificacion;
    private String idCliente;
    private int idPlantilla;
    private String tipo;
    private String asunto;
    private String contenido;
    private String estadoEnvio;
    private LocalDateTime fechaEnvio;
    private String origen;

    public Notificacion() {}

    public Notificacion(int idNotificacion, String idCliente, int idPlantilla, String tipo,
                        String asunto, String contenido, String estadoEnvio,
                        LocalDateTime fechaEnvio, String origen) {
        this.idNotificacion = idNotificacion;
        this.idCliente = idCliente;
        this.idPlantilla = idPlantilla;
        this.tipo = tipo;
        this.asunto = asunto;
        this.contenido = contenido;
        this.estadoEnvio = estadoEnvio;
        this.fechaEnvio = fechaEnvio;
        this.origen = origen;
    }

    public int getIdNotificacion() { return idNotificacion; }
    public void setIdNotificacion(int idNotificacion) { this.idNotificacion = idNotificacion; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public String getEstadoEnvio() { return estadoEnvio; }
    public void setEstadoEnvio(String estadoEnvio) { this.estadoEnvio = estadoEnvio; }

    public LocalDateTime getFechaEnvio() { return fechaEnvio; }
    public void setFechaEnvio(LocalDateTime fechaEnvio) { this.fechaEnvio = fechaEnvio; }

    public String getOrigen() { return origen; }
    public void setOrigen(String origen) { this.origen = origen; }

    @Override
    public String toString() {
        return "Notificacion{" +
                "idNotificacion=" + idNotificacion +
                ", idCliente='" + idCliente + '\'' +
                ", tipo='" + tipo + '\'' +
                ", asunto='" + asunto + '\'' +
                ", estadoEnvio='" + estadoEnvio + '\'' +
                ", fechaEnvio=" + fechaEnvio +
                ", origen='" + origen + '\'' +
                '}';
    }
}