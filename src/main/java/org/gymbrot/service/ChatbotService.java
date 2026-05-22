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
import java.time.LocalTime;
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
    public String procesarMensaje(int idSesion, String texto) {

        // 1. Guardar mensaje del usuario
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

            notificacionService.enviarSmsDirecto(
                "GYMBROT: Tu solicitud de cita fue recibida. Pronto un instructor te contactará para confirmar."
            );

            // Agendar y capturar resultado
            boolean agendada = agendarCitaDesdeChat(texto, "123456", historial);

            // Informar a Groq el resultado real
            if (agendada) {
                contextoExtra = " [SISTEMA: La cita fue agendada exitosamente en la base de datos. " +
                    "Confirma al cliente de forma amable y profesional que su cita quedo registrada " +
                    "y que pronto sera contactado para confirmar los detalles.]";
            } else {
                contextoExtra = " [SISTEMA: No se pudo agendar la cita automaticamente. " +
                    "Disculpate con el cliente e indicale que un asesor se comunicara con el pronto " +
                    "para coordinar la cita manualmente.]";
            }

            // Enviar correos de confirmación
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

        // 2. Llamar a Groq DESPUÉS de procesar todo, con contexto del resultado
        String respuesta = groqService.enviarMensaje(texto + contextoExtra, historial);

        // 3. Guardar respuesta del bot
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

    // ── EXTRAER HORA DEL TEXTO O HISTORIAL ───────────────────────────────
    private LocalTime extraerHora(String texto, List<MensajeGymbrot> historial) {
        LocalTime hora = extraerHoraDeTexto(texto);
        if (hora != null) return hora;

        // Buscar en historial de más reciente a más antiguo
        if (historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                MensajeGymbrot msg = historial.get(i);
                if (msg.getRemitente().equals("CLIENTE")) {
                    hora = extraerHoraDeTexto(msg.getContenido());
                    if (hora != null) return hora;
                }
            }
        }
        return null;
    }

    private LocalTime extraerHoraDeTexto(String texto) {
        String textoLower = texto.toLowerCase();
        java.util.regex.Pattern horaPattern = java.util.regex.Pattern.compile("(\\d{1,2})(am|pm)");
        java.util.regex.Matcher horaMatcher = horaPattern.matcher(textoLower);
        if (horaMatcher.find()) {
            int h = Integer.parseInt(horaMatcher.group(1));
            if (horaMatcher.group(2).equals("pm") && h != 12) h += 12;
            if (horaMatcher.group(2).equals("am") && h == 12) h = 0;
            return LocalTime.of(h, 0);
        }
        return null;
    }

    // ── EXTRAER DIA DEL TEXTO O HISTORIAL ────────────────────────────────
    private LocalDate extraerFecha(String texto, List<MensajeGymbrot> historial) {
        LocalDate fecha = extraerFechaDeTexto(texto);
        if (fecha != null) return fecha;

        // Buscar en historial de más reciente a más antiguo
        if (historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                MensajeGymbrot msg = historial.get(i);
                if (msg.getRemitente().equals("CLIENTE")) {
                    fecha = extraerFechaDeTexto(msg.getContenido());
                    if (fecha != null) return fecha;
                }
            }
        }
        return null;
    }

    private LocalDate extraerFechaDeTexto(String texto) {
        String textoLower = texto.toLowerCase();
        LocalDate hoy = LocalDate.now();
        if (textoLower.contains("lunes"))
            return hoy.with(java.time.DayOfWeek.MONDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 1 ? 1 : 0);
        if (textoLower.contains("martes"))
            return hoy.with(java.time.DayOfWeek.TUESDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 2 ? 1 : 0);
        if (textoLower.contains("miércoles") || textoLower.contains("miercoles"))
            return hoy.with(java.time.DayOfWeek.WEDNESDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 3 ? 1 : 0);
        if (textoLower.contains("jueves"))
            return hoy.with(java.time.DayOfWeek.THURSDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 4 ? 1 : 0);
        if (textoLower.contains("viernes"))
            return hoy.with(java.time.DayOfWeek.FRIDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 5 ? 1 : 0);
        if (textoLower.contains("sábado") || textoLower.contains("sabado"))
            return hoy.with(java.time.DayOfWeek.SATURDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 6 ? 1 : 0);
        return null;
    }

    // ── AGENDAR CITA DESDE CHATBOT ────────────────────────────────────────
    private boolean agendarCitaDesdeChat(String texto, String idCliente, List<MensajeGymbrot> historial) {
        try {
            List<Instructor> instructores = instructorDAO.listarTodos();

            // 1. Buscar instructor en mensaje actual
            String idInstructor = null;
            for (Instructor i : instructores) {
                if (texto.toLowerCase().contains(i.getNombre().toLowerCase())) {
                    idInstructor = i.getNumeroIdentificacion();
                    break;
                }
            }

            // 2. Si no encontró, buscar en historial (más reciente primero)
            if (idInstructor == null && historial != null) {
                for (int i = historial.size() - 1; i >= 0; i--) {
                    MensajeGymbrot msg = historial.get(i);
                    if (msg.getRemitente().equals("CLIENTE")) {
                        for (Instructor inst : instructores) {
                            if (msg.getContenido().toLowerCase().contains(inst.getNombre().toLowerCase())) {
                                idInstructor = inst.getNumeroIdentificacion();
                                break;
                            }
                        }
                    }
                    if (idInstructor != null) break;
                }
            }

            if (idInstructor == null) {
                System.err.println("No se encontró instructor en el mensaje ni en el historial.");
                return false;
            }

            // 3. Buscar fecha en mensaje actual o historial
            LocalDate fecha = extraerFecha(texto, historial);
            if (fecha == null) {
                System.err.println("No se encontró día en el mensaje ni en el historial.");
                return false;
            }

            // 4. Buscar hora en mensaje actual o historial
            LocalTime hora = extraerHora(texto, historial);
            if (hora == null) {
                System.err.println("No se encontró hora en el mensaje ni en el historial.");
                return false;
            }

            // 5. Construir y guardar la cita
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