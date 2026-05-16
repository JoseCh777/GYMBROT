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
    public String procesarMensaje(int idSesion, String texto) {
        MensajeGymbrot msgUsuario = new MensajeGymbrot();
        msgUsuario.setIdSesion(idSesion);
        msgUsuario.setRemitente("cliente");
        msgUsuario.setContenido(texto);
        msgUsuario.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgUsuario);

        String respuesta = groqService.enviarMensaje(texto);

        MensajeGymbrot msgBot = new MensajeGymbrot();
        msgBot.setIdSesion(idSesion);
        msgBot.setRemitente("bot");
        msgBot.setContenido(respuesta);
        msgBot.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgBot);

        return respuesta;
    }
}