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
    private final EmailService emailService = new EmailService();
    private final SmsService smsService = new SmsService();

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
        msgUsuario.setRemitente("CLIENTE");
        msgUsuario.setContenido(texto);
        msgUsuario.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgUsuario);

        List<MensajeGymbrot> historial = mensajeDAO.listarPorSesion(idSesion);
String respuesta = groqService.enviarMensaje(texto, historial);

        // ── DETECTAR INTENCION CITA ────────────────────────────────────────
        String textoLower = texto.toLowerCase();
        if (textoLower.contains("agendar") || textoLower.contains("cita")
                || textoLower.contains("reservar") || textoLower.contains("turno")) {
            notificacionService.enviarSmsDirecto(
                "GYMBROT: Tu solicitud de cita fue recibida. Pronto un instructor te contactará para confirmar."
            );
        } else if (textoLower.contains("membresía") || textoLower.contains("membresia")
                || textoLower.contains("plan") || textoLower.contains("precio")) {
            notificacionService.enviarSmsDirecto(
                "GYMBROT: Te enviamos información sobre nuestros planes. Visítanos en Valledupar o escríbenos por este chat."
            );
        }
            
            

        // ── DETECTAR CORREO EN EL MENSAJE ─────────────────────────────────
        if (texto.matches(".*[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}.*")) {
            String correoDetectado = extraerCorreo(texto);
            if (correoDetectado != null) {
                emailService.enviarCorreo(
                    correoDetectado,
                    "Información de membresías GYMBROT",
                    "<h2>¡Bienvenido a GYMBROT!</h2>" +
                    "<p>Aquí están nuestros planes:</p>" +
                    "<ul>" +
                    "<li><b>Plan Mensual:</b> $80.000 COP</li>" +
                    "<li><b>Plan Semestral:</b> $400.000 COP</li>" +
                    "<li><b>Plan Anual:</b> $700.000 COP</li>" +
                    "</ul>" +
                    "<p>¡Te esperamos en GYMBROT Valledupar!</p>"
                );
                System.out.println("Correo enviado a: " + correoDetectado);
            }
        }
    // ── DETECTAR NUMERO DE TELEFONO ───────────────────────────────────
String telefonoDetectado = extraerTelefono(texto);
if (telefonoDetectado != null) {
    smsService.enviarSms(
        "+57" + telefonoDetectado,
        "GYMBROT: Hola! Aquí están nuestros planes:\n" +
        "- Plan Mensual: $80.000 COP\n" +
        "- Plan Semestral: $400.000 COP\n" +
        "- Plan Anual: $700.000 COP\n" +
        "¡Te esperamos en GYMBROT Valledupar!"
    );
    System.out.println("SMS enviado a: +57" + telefonoDetectado);
}
        MensajeGymbrot msgBot = new MensajeGymbrot();
        msgBot.setIdSesion(idSesion);
        msgBot.setRemitente("BOT");
        msgBot.setContenido(respuesta);
        msgBot.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgBot);

        return respuesta;
    }

     // ── EXTRAER CORREO DEL TEXTO ──────────────────────────────────────────
    private String extraerCorreo(String texto) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        java.util.regex.Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) return matcher.group();
        return null;
    }
     
    // ── EXTRAER TELEFONO DEL TEXTO ────────────────────────────────────
private String extraerTelefono(String texto) {
    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
        "\\b3[0-9]{9}\\b");
    java.util.regex.Matcher matcher = pattern.matcher(texto);
    if (matcher.find()) return matcher.group();
    return null;
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