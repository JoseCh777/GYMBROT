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
    private final ConsultaGymbrotService consultaService = new ConsultaGymbrotService();

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
        String textoLower = texto.toLowerCase();

        // ── DETECTAR INTENCION CITA ────────────────────────────────────────
        if (textoLower.contains("agendar") || textoLower.contains("cita")
                || textoLower.contains("reservar") || textoLower.contains("turno")) {
            notificacionService.enviarSmsDirecto(
                "GYMBROT: Tu solicitud de cita fue recibida. Pronto un instructor te contactará para confirmar."
            );
            List<String> correosHistorial = extraerCorreosDeHistorial(historial);
            String contenidoCita = "<h2>¡Solicitud de cita recibida!</h2>" +
                "<p>Hola, hemos recibido tu solicitud de cita en <b>GYMBROT Valledupar</b>.</p>" +
                "<p><b>Detalle de tu solicitud:</b> " + texto + "</p>" +
                "<p>Uno de nuestros instructores se pondrá en contacto contigo pronto para confirmar.</p>" +
                "<p>Si tienes alguna pregunta, escríbenos por el chat.</p>" +
                "<p>¡Te esperamos!</p>";
            for (String correo : correosHistorial) {
                emailService.enviarCorreo(correo, "Confirmación de solicitud de cita - GYMBROT", contenidoCita);
                System.out.println("Confirmación de cita enviada a: " + correo);
            }
        }

        // ── DETECTAR CORREO EN EL MENSAJE ─────────────────────────────────
        List<String> correosDetectados = extraerCorreos(texto);
        boolean pidioInfo = textoLower.contains("envia") || textoLower.contains("envía")
                || textoLower.contains("manda") || textoLower.contains("información")
                || textoLower.contains("informacion") || textoLower.contains("planes")
                || textoLower.contains("detalles") || textoLower.contains("quiero saber");
        if (!correosDetectados.isEmpty() && pidioInfo) {
            String planesContexto = consultaService.buscarInfoRelevante("planes membresia precio");
            String instructoresContexto = consultaService.buscarInfoRelevante("instructor");
            String contenidoCorreo = "<h2>¡Bienvenido a GYMBROT Valledupar!</h2>" +
                "<p>Aquí está la información actualizada de nuestro gimnasio:</p>" +
                "<pre style='font-family:Arial;'>" +
                planesContexto + "\n" + instructoresContexto +
                "</pre>" +
                "<p>¡Te esperamos en GYMBROT!</p>";
            for (String correo : correosDetectados) {
                emailService.enviarCorreo(correo, "Información GYMBROT", contenidoCorreo);
                System.out.println("Correo enviado a: " + correo);
            }
        }

        // ── DETECTAR NUMERO DE TELEFONO ───────────────────────────────────
        String telefonoDetectado = extraerTelefono(texto);
        if (telefonoDetectado != null) {
            String contexto = consultaService.buscarInfoRelevante(texto);
            String mensajeSms = contexto.isEmpty() ?
                "GYMBROT: Plan Básico $80k | Premium $120k | Elite $200k/mes. ¡Visítanos!" :
                "GYMBROT: " + contexto.substring(0, Math.min(contexto.length(), 130));
            smsService.enviarSms("+57" + telefonoDetectado, mensajeSms);
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

    // ── EXTRAER TODOS LOS CORREOS DEL TEXTO ──────────────────────────────
    private List<String> extraerCorreos(String texto) {
        List<String> correos = new java.util.ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        java.util.regex.Matcher matcher = pattern.matcher(texto);
        while (matcher.find()) correos.add(matcher.group());
        return correos;
    }

    // ── EXTRAER CORREO DEL TEXTO ──────────────────────────────────────────
    private String extraerCorreo(String texto) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        java.util.regex.Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) return matcher.group();
        return null;
    }

    // ── EXTRAER TODOS LOS CORREOS DEL HISTORIAL ───────────────────────────
    private List<String> extraerCorreosDeHistorial(List<MensajeGymbrot> historial) {
        List<String> correosEncontrados = new java.util.ArrayList<>();
        if (historial == null) return correosEncontrados;
        for (MensajeGymbrot msg : historial) {
            if (msg.getRemitente().equals("CLIENTE")) {
                List<String> correos = extraerCorreos(msg.getContenido());
                for (String correo : correos) {
                    if (!correosEncontrados.contains(correo)) {
                        correosEncontrados.add(correo);
                    }
                }
            }
        }
        return correosEncontrados;
    }

    // ── EXTRAER TELEFONO DEL TEXTO ────────────────────────────────────────
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