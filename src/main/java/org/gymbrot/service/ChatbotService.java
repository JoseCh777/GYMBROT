package org.gymbrot.service;

import org.gymbrot.dao.SesionGymbrotDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.dao.MensajeGymbrotDAO;
import org.gymbrot.dao.CitaDAO;
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
    private final CitaDAO citaDAO = new CitaDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();

    // ── INICIAR SESION ────────────────────────────────────────────────────
    public SesionGymbrot iniciarSesion(String idCliente) {
        SesionGymbrot sesion = new SesionGymbrot();
        sesion.setIdCliente(idCliente);
        sesion.setFecha(LocalDate.now());
        sesion.setHoraInicio(LocalDateTime.now());
        sesion.setContextoActivo("inicio");
        int idSesion = sesionDAO.crearSesion(sesion);
        sesion.setIdSesion(idSesion);
        return sesion;
    }

    // ── PROCESAR MENSAJE ──────────────────────────────────────────────────
    public String procesarMensaje(int idSesion, String texto, String idCliente) {

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

        // ── DETECTAR REAGENDAMIENTO ────────────────────────────────────────
        if (textoLower.contains("reagendar") || textoLower.contains("cambiar")
                || textoLower.contains("mover") || textoLower.contains("reprogramar")) {

            int idCitaVieja = extraerIdCita(texto);

            if (idCitaVieja > 0) {
                // Cancelar cita vieja
                boolean cancelada = citaService.cancelarCita(idCitaVieja);

                if (cancelada) {
                    // Agendar nueva cita
                    int idCitaNueva = agendarCitaDesdeChat(texto, idCliente, historial);

                    if (idCitaNueva > 0) {
                        notificacionService.enviarSmsDirecto(
                            "GYMBROT: Tu cita #" + idCitaVieja + " fue cancelada y reagendada con el ID #" + idCitaNueva + ". ¡Te esperamos!"
                        );
                        List<String> correosHistorial = extraerCorreosDeHistorial(historial);
                        for (String correo : correosHistorial) {
                            emailService.enviarCorreo(correo, "Cita reagendada - GYMBROT",
                                "<h2>¡Cita reagendada exitosamente!</h2>" +
                                "<p>Tu cita <b>#" + idCitaVieja + "</b> fue cancelada.</p>" +
                                "<p>Tu nueva cita tiene el ID <b>#" + idCitaNueva + "</b>.</p>" +
                                "<p>Guarda este nuevo ID para futuras cancelaciones o cambios.</p>" +
                                "<p>¡Te esperamos en GYMBROT!</p>");
                            System.out.println("Confirmación de reagendamiento enviada a: " + correo);
                        }
                        contextoExtra = " [SISTEMA: La cita #" + idCitaVieja + " fue cancelada exitosamente " +
                            "y se agendó una nueva cita con ID #" + idCitaNueva + ". " +
                            "IMPORTANTE: Confirma al cliente que su cita anterior #" + idCitaVieja +
                            " fue cancelada y que su nueva cita es la #" + idCitaNueva + ".]";
                    } else {
                        // Canceló la vieja pero no pudo agendar la nueva
                        contextoExtra = " [SISTEMA: La cita #" + idCitaVieja + " fue cancelada pero no se pudo " +
                            "agendar la nueva automáticamente. Informa al cliente que la cita anterior fue cancelada " +
                            "y pídele que especifique instructor, día y hora para la nueva cita.]";
                    }
                } else {
                    contextoExtra = " [SISTEMA: No se pudo cancelar la cita #" + idCitaVieja + ". " +
                        "Puede que ya esté cancelada o no exista. Informa al cliente amablemente.]";
                }
            } else {
                contextoExtra = " [SISTEMA: El cliente quiere reagendar una cita pero no proporcionó el ID. " +
                    "Indícale amablemente que necesitamos el número de cita para reagendarla. " +
                    "Ejemplo: 'Quiero reagendar la cita #86 para el jueves a las 2pm']";
            }

        // ── DETECTAR CANCELACION DE CITA ──────────────────────────────────
        } else if (textoLower.contains("cancelar") || textoLower.contains("cancela")
                || textoLower.contains("no puedo ir")) {

            int idCita = extraerIdCita(texto);
            if (idCita > 0) {
                boolean cancelada = citaService.cancelarCita(idCita);
                if (cancelada) {
                    notificacionService.enviarSmsDirecto(
                        "GYMBROT: Tu cita #" + idCita + " ha sido cancelada exitosamente."
                    );
                    List<String> correosHistorial = extraerCorreosDeHistorial(historial);
                    for (String correo : correosHistorial) {
                        emailService.enviarCorreo(correo, "Cita cancelada - GYMBROT",
                            "<h2>Cita cancelada</h2>" +
                            "<p>Tu cita <b>#" + idCita + "</b> ha sido cancelada exitosamente.</p>" +
                            "<p>Si deseas reagendar, escríbenos por el chat.</p>" +
                            "<p>¡Te esperamos en GYMBROT!</p>");
                        System.out.println("Confirmación de cancelación enviada a: " + correo);
                    }
                    contextoExtra = " [SISTEMA: La cita #" + idCita + " fue cancelada exitosamente en la base de datos. " +
                        "IMPORTANTE: No digas que no existe o que no la tienes registrada. " +
                        "Confirma directamente al cliente que su cita #" + idCita + " fue cancelada con exito " +
                        "y ofrécele reagendar si lo desea.]";
                } else {
                    contextoExtra = " [SISTEMA: No se pudo cancelar la cita #" + idCita + ". " +
                        "Puede que ya esté cancelada o no exista. Informa al cliente amablemente.]";
                }
            } else {
                contextoExtra = " [SISTEMA: El cliente quiere cancelar una cita pero no proporcionó el ID. " +
                    "Indícale amablemente que necesitamos el número de cita para cancelarla. " +
                    "Ejemplo: 'Quiero cancelar la cita #5']";
            }

        // ── DETECTAR INTENCION AGENDAR CITA ───────────────────────────────
        } else if (textoLower.contains("agendar") || textoLower.contains("cita")
                || textoLower.contains("reservar") || textoLower.contains("turno")
                || textoLower.contains("agendame") || textoLower.contains("agenda")) {

            int idCitaAgendada = agendarCitaDesdeChat(texto, idCliente, historial);

            if (idCitaAgendada > 0) {
                notificacionService.enviarSmsDirecto(
                    "GYMBROT: Tu cita #" + idCitaAgendada + " fue agendada exitosamente. ¡Te esperamos!"
                );
                List<String> correosHistorial = extraerCorreosDeHistorial(historial);
                String contenidoCita = "<h2>¡Cita agendada exitosamente!</h2>" +
                    "<p>Hola, tu cita en <b>GYMBROT Valledupar</b> ha sido registrada.</p>" +
                    "<p><b>ID de tu cita:</b> #" + idCitaAgendada + "</p>" +
                    "<p><b>Detalle:</b> " + texto + "</p>" +
                    "<p>Guarda este ID por si necesitas cancelar o reprogramar.</p>" +
                    "<p>¡Te esperamos!</p>";
                for (String correo : correosHistorial) {
                    emailService.enviarCorreo(correo, "Cita agendada - GYMBROT", contenidoCita);
                    System.out.println("Confirmación de cita enviada a: " + correo);
                }
                contextoExtra = " [SISTEMA: La cita fue agendada exitosamente con ID #" + idCitaAgendada + ". " +
                    "Confirma al cliente que su cita quedó registrada e indícale que su ID de cita es #" +
                    idCitaAgendada + " para futuras cancelaciones o cambios.]";
            } else {
                contextoExtra = " [SISTEMA: No se pudo agendar la cita automaticamente. " +
                    "Disculpate con el cliente e indicale que un asesor se comunicara con el pronto.]";
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

    // ── EXTRAER ID DE CITA DEL TEXTO ─────────────────────────────────────
    private int extraerIdCita(String texto) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "#(\\d+)|cita\\s+(\\d+)|id\\s+(\\d+)|pedro\\s+(\\d+)|laura\\s+(\\d+)|diego\\s+(\\d+)");
        java.util.regex.Matcher matcher = pattern.matcher(texto.toLowerCase());
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    return Integer.parseInt(matcher.group(i));
                }
            }
        }
        return -1;
    }

    // ── EXTRAER HORA DEL TEXTO O HISTORIAL ───────────────────────────────
    private LocalTime extraerHora(String texto, List<MensajeGymbrot> historial) {
        LocalTime hora = extraerHoraDeTexto(texto);
        if (hora != null) return hora;
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
        java.util.regex.Pattern horaPattern = java.util.regex.Pattern.compile("(\\d{1,2})(am|pm)");
        java.util.regex.Matcher horaMatcher = horaPattern.matcher(texto.toLowerCase());
        if (horaMatcher.find()) {
            int h = Integer.parseInt(horaMatcher.group(1));
            if (horaMatcher.group(2).equals("pm") && h != 12) h += 12;
            if (horaMatcher.group(2).equals("am") && h == 12) h = 0;
            return LocalTime.of(h, 0);
        }
        return null;
    }

    // ── EXTRAER FECHA DEL TEXTO O HISTORIAL ──────────────────────────────
    private LocalDate extraerFecha(String texto, List<MensajeGymbrot> historial) {
        LocalDate fecha = extraerFechaDeTexto(texto);
        if (fecha != null) return fecha;
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
    private int agendarCitaDesdeChat(String texto, String idCliente, List<MensajeGymbrot> historial) {
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

            // 2. Si no encontró, buscar en historial de más reciente a más antiguo
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
                return -1;
            }

            // 3. Buscar fecha en mensaje actual o historial
            LocalDate fecha = extraerFecha(texto, historial);
            if (fecha == null) {
                System.err.println("No se encontró día en el mensaje ni en el historial.");
                return -1;
            }

            // 4. Buscar hora en mensaje actual o historial
            LocalTime hora = extraerHora(texto, historial);
            if (hora == null) {
                System.err.println("No se encontró hora en el mensaje ni en el historial.");
                return -1;
            }

            // 5. Construir cita y obtener ID directamente
            Cita cita = new Cita();
            cita.setIdCliente(idCliente);
            cita.setIdInstructor(idInstructor);
            cita.setFecha(fecha);
            cita.setHora(hora);
            cita.setTipoCita("CONSULTA");
            cita.setNotas("Cita agendada desde el chatbot GymBrot");

            int idCita = citaService.programarCitaYRetornarId(cita);
            if (idCita > 0) {
                System.out.println("Cita agendada en Oracle con ID: #" + idCita);
            }
            return idCita;

        } catch (Exception e) {
            System.err.println("Error al agendar cita: " + e.getMessage());
            return -1;
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