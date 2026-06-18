package org.gymbrot.controller;

import org.gymbrot.model.MensajeGymbrot;
import org.gymbrot.model.SesionGymbrot;
import org.gymbrot.service.ChatbotService;

import java.util.List;

public class ChatbotController {

    private final ChatbotService chatbotService = new ChatbotService();
    private SesionGymbrot sesionActual;
    private String idClienteActual;

    // ── INITIALIZE ────────────────────────────────────────────────────────
    public void initialize(String idCliente) {
        this.idClienteActual = idCliente;
        sesionActual = chatbotService.iniciarSesion(idCliente);
        System.out.println("Sesión iniciada: " + sesionActual.getIdSesion());
    }

    // ── ENVIAR MENSAJE ────────────────────────────────────────────────────
    public String onEnviarMensaje(String texto) {
        if (sesionActual == null || texto == null || texto.trim().isEmpty()) {
            return "Por favor escribe un mensaje.";
        }
        return chatbotService.procesarMensaje(sesionActual.getIdSesion(), texto, idClienteActual);
    }

    // ── CARGAR HISTORIAL ──────────────────────────────────────────────────
    public List<MensajeGymbrot> cargarHistorial() {
        if (sesionActual == null) return null;
        return chatbotService.obtenerHistorial(sesionActual.getIdSesion());
    }

    // ── NUEVA SESION ──────────────────────────────────────────────────────
    public void onNuevaSesion(String idCliente) {
        if (sesionActual != null) {
            chatbotService.cerrarSesion(sesionActual.getIdSesion());
        }
        this.idClienteActual = idCliente;
        sesionActual = chatbotService.iniciarSesion(idCliente);
        System.out.println("Nueva sesión iniciada: " + sesionActual.getIdSesion());
    }
}