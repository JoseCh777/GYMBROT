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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

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
        boolean correosYaEnviados = false;

        // ── DETECTAR CONSULTA DE CITAS PENDIENTES ─────────────────────────
        if ((textoLower.contains("mis citas") || textoLower.contains("qué citas")
                || textoLower.contains("que citas") || textoLower.contains("citas tengo")
                || textoLower.contains("citas pendientes") || textoLower.contains("ver citas")
                || textoLower.contains("mostrar citas") || textoLower.contains("listar citas"))
                && !textoLower.contains("agendar") && !textoLower.contains("cancelar")) {

            List<Cita> citasPendientes = citaDAO.listarPorCliente(idCliente)
                .stream()
                .filter(c -> c.getEstado().equals("PENDIENTE"))
                .collect(Collectors.toList());

            if (!citasPendientes.isEmpty()) {
                DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter fmtHora  = DateTimeFormatter.ofPattern("HH:mm");
                StringBuilder listaCitas = new StringBuilder();
                for (Cita c : citasPendientes) {
                    String nombreInstructor = obtenerNombreInstructor(c.getIdInstructor());
                    listaCitas.append(String.format("Cita #%d — Instructor: %s | Fecha: %s | Hora: %s\n",
                        c.getIdCita(), nombreInstructor,
                        c.getFecha().format(fmtFecha), c.getHora().format(fmtHora)));
                }
                contextoExtra = " [SISTEMA: El cliente tiene las siguientes citas PENDIENTES:\n" +
                    listaCitas.toString() +
                    "Muéstraselas de forma clara y amable. Recuérdale que puede cancelar o reagendar " +
                    "cualquier cita indicando su número de ID.]";
            } else {
                contextoExtra = " [SISTEMA: El cliente no tiene citas pendientes en este momento. " +
                    "Infórmale amablemente y ofrécele agendar una nueva cita.]";
            }

        // ── DETECTAR REAGENDAMIENTO ────────────────────────────────────────
        } else if (textoLower.contains("reagendar") || textoLower.contains("cambiar")
                || textoLower.contains("mover") || textoLower.contains("reprogramar")) {

            int idCitaVieja = extraerIdCita(texto);
            if (idCitaVieja > 0) {
                boolean cancelada = citaService.cancelarCita(idCitaVieja);
                if (cancelada) {
                    int idCitaNueva = agendarCitaDesdeChat(texto, idCliente, historial);
                    if (idCitaNueva > 0) {
                        Cita citaNueva = citaDAO.buscarPorId(idCitaNueva);
                        String nombreInstructor = obtenerNombreInstructor(citaNueva.getIdInstructor());
                        notificacionService.enviarSmsDirecto(
                            "GYMBROT: Tu cita #" + idCitaVieja + " fue cancelada y reagendada con ID #" + idCitaNueva + ". ¡Te esperamos!"
                        );
                        List<String> correosHistorial = extraerCorreosDeHistorial(historial);
                        for (String correo : correosHistorial) {
                            emailService.enviarCorreo(correo, "Cita reagendada - GYMBROT",
                                "<h2>¡Cita reagendada exitosamente!</h2>" +
                                "<p>Tu cita anterior <b>#" + idCitaVieja + "</b> fue cancelada.</p>" +
                                "<p>Tu nueva cita ha sido registrada:</p>" +
                                "<table style='border-collapse:collapse;'>" +
                                "<tr><td><b>ID de cita:</b></td><td>#" + idCitaNueva + "</td></tr>" +
                                "<tr><td><b>Instructor:</b></td><td>" + nombreInstructor + "</td></tr>" +
                                "<tr><td><b>Fecha:</b></td><td>" + citaNueva.getFecha() + "</td></tr>" +
                                "<tr><td><b>Hora:</b></td><td>" + citaNueva.getHora() + "</td></tr>" +
                                "</table><br>" +
                                "<p>Guarda el ID por si necesitas cancelar o reprogramar.</p>" +
                                "<p>¡Te esperamos en GYMBROT!</p>");
                            System.out.println("Confirmación de reagendamiento enviada a: " + correo);
                        }
                        correosYaEnviados = true;
                        contextoExtra = " [SISTEMA: La cita #" + idCitaVieja + " fue cancelada y la nueva cita es #" + idCitaNueva + ". " +
                            "Confirma ambos IDs al cliente.]";
                    } else {
                        contextoExtra = " [SISTEMA: La cita #" + idCitaVieja + " fue cancelada pero no se pudo agendar la nueva. " +
                            "Pide al cliente instructor, día y hora para la nueva cita.]";
                    }
                } else {
                    contextoExtra = " [SISTEMA: No se pudo cancelar la cita #" + idCitaVieja + ". Informa al cliente amablemente.]";
                }
            } else {
                contextoExtra = " [SISTEMA: El cliente quiere reagendar pero no dio el ID. " +
                    "Pídele el número de cita. Ejemplo: 'Quiero reagendar la cita #86 para el jueves a las 2pm']";
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
                    correosYaEnviados = true;
                    contextoExtra = " [SISTEMA: La cita #" + idCita + " fue cancelada exitosamente. " +
                        "IMPORTANTE: Confirma al cliente que su cita #" + idCita + " fue cancelada con exito " +
                        "y ofrécele reagendar si lo desea.]";
                } else {
                    contextoExtra = " [SISTEMA: No se pudo cancelar la cita #" + idCita + ". " +
                        "Puede que ya esté cancelada o no exista. Informa al cliente amablemente.]";
                }
            } else {
                contextoExtra = " [SISTEMA: El cliente quiere cancelar pero no dio el ID. " +
                    "Pídele el número de cita. Ejemplo: 'Quiero cancelar la cita #5']";
            }

        // ── DETECTAR INTENCION AGENDAR CITA ───────────────────────────────
        } else if (textoLower.contains("agendar") || textoLower.contains("cita")
                || textoLower.contains("reservar") || textoLower.contains("turno")
                || textoLower.contains("agendame") || textoLower.contains("agenda")) {

            int idCitaAgendada = agendarCitaDesdeChat(texto, idCliente, historial);

            if (idCitaAgendada > 0) {
                Cita citaAgendada = citaDAO.buscarPorId(idCitaAgendada);
                String nombreInst = obtenerNombreInstructor(citaAgendada.getIdInstructor());
                String fechaFmt = citaAgendada.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String horaFmt = citaAgendada.getHora().format(DateTimeFormatter.ofPattern("hh:mm a"));
                notificacionService.enviarSmsDirecto(
                    "GYMBROT ✓ Cita #" + idCitaAgendada + " agendada\n" +
                    "Instructor: " + nombreInst + "\n" +
                    "Fecha: " + fechaFmt + "\n" +
                    "Hora: " + horaFmt + "\n" +
                    "Para cancelar indica tu ID de cita en el chat."
                );
                List<String> correosTextoActual = extraerCorreos(texto);
                List<String> correosHistorial = extraerCorreosDeHistorial(historial);
                List<String> todosCorreos = new java.util.ArrayList<>(correosTextoActual);
                for (String c : correosHistorial) {
                    if (!todosCorreos.contains(c)) todosCorreos.add(c);
                }
                String contenidoCita = "<h2>¡Cita agendada exitosamente!</h2>" +
                    "<p>Hola, tu cita en <b>GYMBROT Valledupar</b> ha sido registrada.</p>" +
                    "<table style='border-collapse:collapse;'>" +
                    "<tr><td><b>ID de cita:</b></td><td>#" + idCitaAgendada + "</td></tr>" +
                    "<tr><td><b>Instructor:</b></td><td>" + nombreInst + "</td></tr>" +
                    "<tr><td><b>Fecha:</b></td><td>" + fechaFmt + "</td></tr>" +
                    "<tr><td><b>Hora:</b></td><td>" + horaFmt + "</td></tr>" +
                    "</table><br>" +
                    "<p>Guarda el ID por si necesitas cancelar o reprogramar.</p>" +
                    "<p>¡Te esperamos en GYMBROT!</p>";
                for (String correo : todosCorreos) {
                    emailService.enviarCorreo(correo, "Cita agendada - GYMBROT", contenidoCita);
                    System.out.println("Confirmación de cita enviada a: " + correo);
                }
                correosYaEnviados = true;
                contextoExtra = " [SISTEMA: La cita fue agendada exitosamente con ID #" + idCitaAgendada + ". " +
                    "Confirma al cliente que su cita quedó registrada con ID #" + idCitaAgendada + ".]";
            } else {
                contextoExtra = " [SISTEMA: No se pudo agendar la cita automaticamente. " +
                    "Disculpate con el cliente e indicale que un asesor se comunicara con el pronto.]";
            }
        }

        // ── DETECTAR CORREO EN EL MENSAJE (solo info general) ─────────────
        if (!correosYaEnviados) {
            List<String> correosDetectados = extraerCorreos(texto);
            boolean pidioInfoGeneral = (textoLower.contains("envia") || textoLower.contains("envía")
                    || textoLower.contains("manda") || textoLower.contains("información")
                    || textoLower.contains("informacion") || textoLower.contains("planes")
                    || textoLower.contains("detalles") || textoLower.contains("quiero saber"))
                    && !textoLower.contains("cita")
                    && !textoLower.contains("agend")
                    && !textoLower.contains("reserv");
            if (!correosDetectados.isEmpty() && pidioInfoGeneral) {


                String planesContexto = consultaService.buscarInfoRelevante("planes membresia precio");
                String contenidoCorreo = "<h2>¡Bienvenido a GYMBROT Valledupar!</h2>" +
                        "<p>Aquí está la información actualizada de nuestro gimnasio:</p>" +
                        "<pre style='font-family:Arial;'>" +
                        planesContexto +
                        "</pre>" +
                        "<p>Para consultar disponibilidad de instructores, escríbenos por el chat.</p>" +
                        "<p>¡Te esperamos en GYMBROT!</p>";
                for (String correo : correosDetectados) {
                    emailService.enviarCorreo(correo, "Información GYMBROT", contenidoCorreo);
                    System.out.println("Correo enviado a: " + correo);
                }
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

        // 2. Llamar a Groq DESPUÉS de procesar todo
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

    // ── OBTENER NOMBRE DEL INSTRUCTOR ─────────────────────────────────────
    private String obtenerNombreInstructor(String idInstructor) {
        for (Instructor inst : instructorDAO.listarTodos()) {
            if (inst.getNumeroIdentificacion().equals(idInstructor)) {
                return inst.getNombre();
            }
        }
        return idInstructor;
    }

    // ── EXTRAER TODOS LOS CORREOS DEL TEXTO ──────────────────────────────
    private List<String> extraerCorreos(String texto) {
        List<String> correos = new java.util.ArrayList<>();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?<![\\w.])([a-zA-Z0-9][a-zA-Z0-9._%+-]*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
        java.util.regex.Matcher matcher = pattern.matcher(texto);
        while (matcher.find()) correos.add(matcher.group(1));
        return correos;
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
        String textoLower = texto.toLowerCase();

        //  Detecta: 10am, 10 am, 10:00, 10:00am, 10:00 am
        java.util.regex.Pattern horaPattern = java.util.regex.Pattern.compile(
            "(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)");
        java.util.regex.Matcher horaMatcher = horaPattern.matcher(textoLower);
        if (horaMatcher.find()) {
            int h = Integer.parseInt(horaMatcher.group(1));
            String ampm = horaMatcher.group(3);
            if (ampm.equals("pm") && h != 12) h += 12;
            if (ampm.equals("am") && h == 12) h = 0;
            int min = horaMatcher.group(2) != null ? Integer.parseInt(horaMatcher.group(2)) : 0;
            return LocalTime.of(h, min);
        }

        // Detecta formato 24h: 15:00, 08:30
        java.util.regex.Pattern hora24Pattern = java.util.regex.Pattern.compile(
            "\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b");
        java.util.regex.Matcher hora24Matcher = hora24Pattern.matcher(textoLower);
        if (hora24Matcher.find()) {
            int h = Integer.parseInt(hora24Matcher.group(1));
            int min = Integer.parseInt(hora24Matcher.group(2));
            return LocalTime.of(h, min);
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

            String idInstructor = null;
            for (Instructor i : instructores) {
                if (texto.toLowerCase().contains(i.getNombre().toLowerCase())) {
                    idInstructor = i.getNumeroIdentificacion();
                    break;
                }
            }

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

            LocalDate fecha = extraerFecha(texto, historial);
            if (fecha == null) {
                System.err.println("No se encontró día en el mensaje ni en el historial.");
                return -1;
            }

            LocalTime hora = extraerHora(texto, historial);
            if (hora == null) {
                System.err.println("No se encontró hora en el mensaje ni en el historial.");
                return -1;
            }

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