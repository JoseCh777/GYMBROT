package org.gymbrot.util;

import org.gymbrot.model.SesionGymbrot;

public class ChatbotSession {

    private static ChatbotSession instancia;
    private SesionGymbrot sesionActual;
    private String idCliente;

    private ChatbotSession() {}

    public static ChatbotSession getInstance() {
        if (instancia == null) instancia = new ChatbotSession();
        return instancia;
    }

    public SesionGymbrot getSesionActual() { return sesionActual; }
    public void setSesionActual(SesionGymbrot sesion) { this.sesionActual = sesion; }
    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }
    public boolean tieneSesionActiva() { return sesionActual != null && sesionActual.getIdSesion() > 0; }
}