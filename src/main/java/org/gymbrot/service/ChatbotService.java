package org.gymbrot.service;

import org.gymbrot.dao.SesionGymbrotDAO;
import org.gymbrot.dao.MensajeGymbrotDAO;
import org.gymbrot.model.SesionGymbrot;
import org.gymbrot.model.MensajeGymbrot;

import java.time.LocalDateTime;
import java.util.List;

public class ChatbotService {

    private final SesionGymbrotDAO sesionDAO = new SesionGymbrotDAO();
    private final MensajeGymbrotDAO mensajeDAO = new MensajeGymbrotDAO();
    private final GroqService groqService = new GroqService();
    private final NotificacionService notificacionService = new NotificacionService();

    // ── INICIAR SESION ────────────────────────────────────────────────────
    public SesionGymbrot iniciarSesion(String idCliente) {
        SesionGymbrot sesion = new SesionGymbrot();
        sesion.setIdCliente(idCliente);
        sesion.setFecha(java.time.LocalDate.now());
        sesion.setHoraInicio(LocalDateTime.now());
        sesion.setContextoActivo("inicio");
        int idSesion = sesionDAO.crearSesion(sesion);
        sesion.setIdSesion(idSesion);
        return sesion;
    }

    // ── PROCESAR MENSAJE ──────────────────────────────────────────────────
   // ── PROCESAR MENSAJE ──────────────────────────────────────────────────
    public String procesarMensaje(int idSesion, String texto) {
        MensajeGymbrot msgUsuario = new MensajeGymbrot();
        msgUsuario.setIdSesion(idSesion);
        msgUsuario.setRemitente("CLIENTE");
        msgUsuario.setContenido(texto);
        msgUsuario.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgUsuario);

        String respuesta = groqService.enviarMensaje(texto);

        // ── DETECTAR INTENCION ─────────────────────────────────────────
        String textoLower = texto.toLowerCase();
        if (textoLower.contains("agendar") || textoLower.contains("cita")
                || textoLower.contains("reservar") || textoLower.contains("turno")) {
            notificacionService.enviarSmsDirecto(
                "GYMBROT: Tu solicitud de cita fue recibida. Pronto un instructor te contactará para confirmar."
            );
        } else if (textoLower.contains("membresía") || textoLower.contains("membresia")
                || textoLower.contains("plan") || textoLower.contains("precio")) {
            notificacionService.enviarSmsDirecto(
                "GYMBROT: Te enviamos información sobre nuestros planes. Contáctanos al 3001234567."
            );
        }

        MensajeGymbrot msgBot = new MensajeGymbrot();
        msgBot.setIdSesion(idSesion);
        msgBot.setRemitente("BOT");
        msgBot.setContenido(respuesta);
        msgBot.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgBot);

        return respuesta;
    }
    // ── CERRAR SESION ─────────────────────────────────────────────────────
    public void cerrarSesion(int idSesion) {
        sesionDAO.cerrarSesion(idSesion);
    }

    // ── OBTENER HISTORIAL ─────────────────────────────────────────────────
    public List<MensajeGymbrot> obtenerHistorial(int idSesion) {
        return mensajeDAO.listarPorSesion(idSesion);
    }
}