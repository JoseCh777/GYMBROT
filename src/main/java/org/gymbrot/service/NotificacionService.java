package org.gymbrot.service;

import org.gymbrot.dao.NotificacionDAO;
import org.gymbrot.dao.PlantillaMensajeDAO;
import org.gymbrot.model.Notificacion;
import org.gymbrot.model.PlantillaMensaje;

import java.time.LocalDateTime;
import java.util.List;

public class NotificacionService {

    private final NotificacionDAO notifDAO = new NotificacionDAO();
    private final PlantillaMensajeDAO plantillaDAO = new PlantillaMensajeDAO();
    private final EmailService emailService = new EmailService();
    private final SmsService smsService = new SmsService();

    // ── ENVIAR POR PLANTILLA ──────────────────────────────────────────────
    public boolean enviarPorPlantilla(String idCliente, int idPlantilla, String correo, String telefono) {
        PlantillaMensaje plantilla = plantillaDAO.buscarPorId(idPlantilla);
        if (plantilla == null) return false;

        Notificacion notif = new Notificacion();
        notif.setIdCliente(idCliente);
        notif.setIdPlantilla(idPlantilla);
        notif.setTipo(plantilla.getTipo());
        notif.setAsunto(plantilla.getAsunto());
        notif.setContenido(plantilla.getCuerpoHtml());
        notif.setEstadoEnvio("PENDIENTE");
        notif.setFechaEnvio(LocalDateTime.now());
        notif.setOrigen("SISTEMA");
        notifDAO.insertar(notif);

        boolean enviado = false;
        if (correo != null && !correo.isEmpty()) {
            enviado = emailService.enviarCorreo(correo, plantilla.getAsunto(), plantilla.getCuerpoHtml());
        }
        if (telefono != null && !telefono.isEmpty()) {
            enviado = smsService.enviarSms(telefono, plantilla.getCuerpoTexto());
        }
        return enviado;
    }

    // ── ENVIAR SMS AL NÚMERO FIJO (admin/notificaciones internas) ─────────
    public void enviarSmsDirecto(String mensaje) {
        try {
            java.io.FileInputStream fis = new java.io.FileInputStream("config.properties");
            java.util.Properties props = new java.util.Properties();
            props.load(fis);
            String telefono = props.getProperty("TWILIO_PHONE_TO");
            if (telefono != null && !telefono.isEmpty()) {
                smsService.enviarSms(telefono, mensaje);
            }
        } catch (Exception e) {
            System.err.println("Error al enviar SMS directo: " + e.getMessage());
        }
    }

    // ── ENVIAR SMS A UN NÚMERO ESPECÍFICO ─────────────────────────────────
    public void enviarSmsACliente(String telefono, String mensaje) {
        if (telefono == null || telefono.isEmpty()) return;
        try {
            String numero = telefono.startsWith("+") ? telefono : "+57" + telefono;
            smsService.enviarSms(numero, mensaje);
        } catch (Exception e) {
            System.err.println("Error al enviar SMS al cliente: " + e.getMessage());
        }
    }

    // ── LISTAR PENDIENTES ─────────────────────────────────────────────────
    public List<Notificacion> listarPendientes() {
        return notifDAO.listarPendientes();
    }

    // ── PROCESAR COLA ─────────────────────────────────────────────────────
    public void procesarCola() {
        List<Notificacion> pendientes = notifDAO.listarPendientes();
        for (Notificacion n : pendientes) {
            boolean enviado = false;
            if (n.getTipo().equals("EMAIL")) {
                enviado = emailService.enviarCorreo(
                        n.getIdCliente(), n.getAsunto(), n.getContenido());
            } else if (n.getTipo().equals("SMS")) {
                enviado = smsService.enviarSms(n.getIdCliente(), n.getContenido());
            }
            if (enviado) {
                notifDAO.marcarEnviada(n.getIdNotificacion());
            }
        }
    }
}