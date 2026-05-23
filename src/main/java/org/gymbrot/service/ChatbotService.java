package org.gymbrot.service;

import org.gymbrot.dao.SesionGymbrotDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.dao.MensajeGymbrotDAO;
import org.gymbrot.model.SesionGymbrot;
import org.gymbrot.model.Cita;
import org.gymbrot.model.Instructor;
import org.gymbrot.model.MensajeGymbrot;

import java.time.LocalDate;
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
    private final CitaService citaService = new CitaService();
    private final InstructorDAO instructorDAO = new InstructorDAO();

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
    public String procesarMensaje(int idSesion, String texto, String idCliente) {
        MensajeGymbrot msgUsuario = new MensajeGymbrot();
        msgUsuario.setIdSesion(idSesion);
        msgUsuario.setRemitente("CLIENTE");
        msgUsuario.setContenido(texto);
        msgUsuario.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgUsuario);

        List<MensajeGymbrot> historial = mensajeDAO.listarPorSesion(idSesion);
        String textoLower = texto.toLowerCase();
        String contextoExtra = "";

        // ── DETECTAR INTENCION CITA ────────────────────────────────────────
        if (textoLower.contains("agendar") || textoLower.contains("cita")
                || textoLower.contains("reservar") || textoLower.contains("turno")
                || textoLower.contains("agendame") || textoLower.contains("agenda")) {

            boolean agendada = agendarCitaDesdeChat(texto, idCliente, historial);

            if (agendada) {
                notificacionService.enviarSmsDirecto(
                    "GYMBROT: Tu cita fue agendada exitosamente. Pronto recibirás más detalles."
                );
                List<String> correosHistorial = extraerCorreosDeHistorial(historial);
                String contenidoCita = "<h2>¡Cita agendada exitosamente!</h2>" +
                    "<p>Hola, tu cita en <b>GYMBROT Valledupar</b> ha sido registrada en nuestro sistema.</p>" +
                    "<p><b>Detalle de tu cita:</b> " + texto + "</p>" +
                    "<p>Pronto recibirás confirmación con todos los detalles.</p>" +
                    "<p>Si necesitas cancelar o reprogramar, escríbenos por el chat.</p>" +
                    "<p>¡Te esperamos!</p>";
                for (String correo : correosHistorial) {
                    emailService.enviarCorreo(correo, "Cita agendada - GYMBROT", contenidoCita);
                    System.out.println("Confirmación de cita enviada a: " + correo);
                }
                contextoExtra = " [SISTEMA: La cita fue agendada exitosamente en la base de datos. " +
                    "Confirma al cliente de forma amable que su cita quedó registrada " +
                    "y que pronto será contactado para confirmar los detalles.]";
            } else {
                notificacionService.enviarSmsDirecto(
                    "GYMBROT: Tu solicitud de cita fue recibida. Pronto un instructor te contactará para confirmar."
                );
                contextoExtra = " [SISTEMA: No se pudo agendar la cita automaticamente. " +
                    "Disculpate con el cliente e indicale que un asesor se comunicara con el pronto " +
                    "para coordinar la cita manualmente.]";
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

        // ── DETECTAR CANCELACION DE CITA ──────────────────────────────────
        if (textoLower.contains("cancelar") || textoLower.contains("cancela")
                || textoLower.contains("cancelar cita") || textoLower.contains("no puedo ir")) {
            contextoExtra += " [SISTEMA: El cliente quiere cancelar una cita. " +
                "Indícale amablemente que para cancelar necesitamos el ID de su cita " +
                "o que un asesor se comunicará con él para gestionar la cancelación.]";
        }

        // ── ENVIAR MENSAJE A GROQ CON CONTEXTO ────────────────────────────
        String respuesta = groqService.enviarMensaje(texto + contextoExtra, historial);

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

    // ── AGENDAR CITA DESDE CHATBOT ────────────────────────────────────────
    private boolean agendarCitaDesdeChat(String texto, String idCliente, List<MensajeGymbrot> historial) {
        try {
            String idInstructor = null;
            List<Instructor> instructores = instructorDAO.listarTodos();
            for (Instructor i : instructores) {
                if (texto.toLowerCase().contains(i.getNombre().toLowerCase())) {
                    idInstructor = i.getNumeroIdentificacion();
                    break;
                }
            }
            if (idInstructor == null) return false;

            LocalDate fecha = null;
            LocalDate hoy = LocalDate.now();
            String textoLower = texto.toLowerCase();
            if (textoLower.contains("lunes")) fecha = hoy.with(java.time.DayOfWeek.MONDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 1 ? 1 : 0);
            else if (textoLower.contains("martes")) fecha = hoy.with(java.time.DayOfWeek.TUESDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 2 ? 1 : 0);
            else if (textoLower.contains("miércoles") || textoLower.contains("miercoles")) fecha = hoy.with(java.time.DayOfWeek.WEDNESDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 3 ? 1 : 0);
            else if (textoLower.contains("jueves")) fecha = hoy.with(java.time.DayOfWeek.THURSDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 4 ? 1 : 0);
            else if (textoLower.contains("viernes")) fecha = hoy.with(java.time.DayOfWeek.FRIDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 5 ? 1 : 0);
            else if (textoLower.contains("sábado") || textoLower.contains("sabado")) fecha = hoy.with(java.time.DayOfWeek.SATURDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 6 ? 1 : 0);
            if (fecha == null) return false;

            java.time.LocalTime hora = null;
            java.util.regex.Pattern horaPattern = java.util.regex.Pattern.compile("(\\d{1,2})(am|pm)");
            java.util.regex.Matcher horaMatcher = horaPattern.matcher(textoLower);
            if (horaMatcher.find()) {
                int h = Integer.parseInt(horaMatcher.group(1));
                if (horaMatcher.group(2).equals("pm") && h != 12) h += 12;
                hora = java.time.LocalTime.of(h, 0);
            }
            if (hora == null) return false;

            Cita cita = new Cita();
            cita.setIdCliente(idCliente);
            cita.setIdInstructor(idInstructor);
            cita.setFecha(fecha);
            cita.setHora(hora);
            cita.setTipoCita("CONSULTA");
            cita.setEstado("PENDIENTE");
            cita.setNotas("Cita agendada desde el chatbot GymBrot");

            boolean agendada = citaService.programarCita(cita);
            if (agendada) {
                System.out.println("Cita agendada en Oracle exitosamente.");
            }
            return agendada;

        } catch (Exception e) {
            System.err.println("Error al agendar cita: " + e.getMessage());
            return false;
        }
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