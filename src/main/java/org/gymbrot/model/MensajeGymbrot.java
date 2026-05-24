package org.gymbrot.model;

import java.time.LocalDateTime;

public class MensajeGymbrot {

    private int idMensaje;
    private int idSesion;
    private Integer idCita;
    private String remitente;
    private String contenido;
    private LocalDateTime timestampMsg;
    private String tipoIntencion;

    public MensajeGymbrot() {}

    public MensajeGymbrot(int idMensaje, int idSesion, Integer idCita, String remitente,
                          String contenido, LocalDateTime timestampMsg, String tipoIntencion) {
        this.idMensaje = idMensaje;
        this.idSesion = idSesion;
        this.idCita = idCita;
        this.remitente = remitente;
        this.contenido = contenido;
        this.timestampMsg = timestampMsg;
        this.tipoIntencion = tipoIntencion;
    }

    public int getIdMensaje() { return idMensaje; }
    public void setIdMensaje(int idMensaje) { this.idMensaje = idMensaje; }

    public int getIdSesion() { return idSesion; }
    public void setIdSesion(int idSesion) { this.idSesion = idSesion; }

    public Integer getIdCita() { return idCita; }
    public void setIdCita(Integer idCita) { this.idCita = idCita; }

    public String getRemitente() { return remitente; }
    public void setRemitente(String remitente) { this.remitente = remitente; }

    public String getContenido() { return contenido; }
    public void setContenido(String contenido) { this.contenido = contenido; }

    public LocalDateTime getTimestampMsg() { return timestampMsg; }
    public void setTimestampMsg(LocalDateTime timestampMsg) { this.timestampMsg = timestampMsg; }

    public String getTipoIntencion() { return tipoIntencion; }
    public void setTipoIntencion(String tipoIntencion) { this.tipoIntencion = tipoIntencion; }

    @Override
    public String toString() {
        return "MensajeGymbrot{" +
                "idMensaje=" + idMensaje +
                ", idSesion=" + idSesion +
                ", remitente='" + remitente + '\'' +
                ", contenido='" + contenido + '\'' +
                ", timestampMsg=" + timestampMsg +
                ", tipoIntencion='" + tipoIntencion + '\'' +
                '}';
    }
}
