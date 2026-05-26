package org.gymbrot.service;

import org.gymbrot.dao.*;
import org.gymbrot.model.*;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ChatbotService {

    private final SesionGymbrotDAO       sesionDAO       = new SesionGymbrotDAO();
    private final MensajeGymbrotDAO      mensajeDAO      = new MensajeGymbrotDAO();
    private final GroqService            groqService     = new GroqService();
    private final NotificacionService    notifService    = new NotificacionService();
    private final EmailService           emailService    = new EmailService();
    private final ConsultaGymbrotService consultaService = new ConsultaGymbrotService();
    private final CitaService            citaService     = new CitaService();
    private final CitaDAO                citaDAO         = new CitaDAO();
    private final InstructorDAO          instructorDAO   = new InstructorDAO();
    private final ClienteDAO             clienteDAO      = new ClienteDAO();
    private final UsuarioDAO             usuarioDAO      = new UsuarioDAO();
    private final MembresiaDAO           membresiaDAO    = new MembresiaDAO();
    private final HistorialMembresiaDAO  historialDAO    = new HistorialMembresiaDAO();
    private final RegistroIngresoDAO     ingresoDAO      = new RegistroIngresoDAO();

    // ── INICIAR SESIÓN ────────────────────────────────────────────────────
    public SesionGymbrot iniciarSesion(String idAdmin) {
        SesionGymbrot sesion = new SesionGymbrot();
        sesion.setIdCliente(idAdmin);
        sesion.setFecha(LocalDate.now());
        sesion.setHoraInicio(LocalDateTime.now());
        sesion.setContextoActivo("admin");
        int idSesion = sesionDAO.crearSesion(sesion);
        if (idSesion <= 0) {
            System.err.println("[ChatbotService] ERROR: No se pudo crear sesión para admin '"
                    + idAdmin + "'. Verifica que el ID exista en CLIENTES " +
                    "(FK: SESIONES_GYMBROT.id_cliente → CLIENTES).");
        }
        sesion.setIdSesion(idSesion);
        return sesion;
    }

    // ── PROCESAR MENSAJE ──────────────────────────────────────────────────
    public String procesarMensaje(int idSesion, String texto, String idAdmin) {

        MensajeGymbrot msgUsuario = new MensajeGymbrot();
        msgUsuario.setIdSesion(idSesion);
        msgUsuario.setRemitente("CLIENTE");
        msgUsuario.setContenido(texto);
        msgUsuario.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgUsuario);

        List<MensajeGymbrot> historial = mensajeDAO.listarPorSesion(idSesion);
        String textoLower    = texto.toLowerCase();
        String contextoExtra = "";

        // ── CREAR CLIENTE ─────────────────────────────────────────────────
        if (textoLower.contains("crear cliente") || textoLower.contains("registrar cliente")
                || textoLower.contains("nuevo cliente") || textoLower.contains("agregar cliente")) {

            contextoExtra = procesarCrearCliente(texto, historial);

            // ── CREAR RUTINA ─────────────────────────────────────────────────
        } else if (textoLower.contains("crear rutina") || textoLower.contains("crea rutina")
                || textoLower.contains("generar rutina") || textoLower.contains("nueva rutina")) {

            contextoExtra = procesarCrearRutina(texto, historial, idAdmin);

            // ── CONSULTAR CLIENTE ─────────────────────────────────────────────
        } else if ((textoLower.contains("consultar cliente") || textoLower.contains("buscar cliente")
                || textoLower.contains("información del cliente") || textoLower.contains("info del cliente")
                || textoLower.contains("ver cliente") || textoLower.contains("datos del cliente"))
                && extraerIdCliente(texto) != null) {

            String idCliente = extraerIdCliente(texto);
            Cliente c = clienteDAO.buscarPorId(idCliente);
            if (c != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                StringBuilder info = new StringBuilder();
                info.append(String.format("Nombre: %s %s\n", c.getNombre(), c.getApellidos()));
                info.append(String.format("ID: %s\n", c.getNumeroIdentificacion()));
                info.append(String.format("Correo: %s\n", c.getCorreo()));
                info.append(String.format("Teléfono: %s\n", c.getTelefono()));
                info.append(String.format("Estado: %s\n", c.getEstado()));
                info.append(String.format("Dirección: %s\n", c.getDireccion()));
                if (c.getFechaNacimiento() != null)
                    info.append(String.format("Fecha nacimiento: %s\n",
                            c.getFechaNacimiento().format(fmt)));

                // Membresía activa
                List<Membresia> activas = membresiaDAO.listarPorEstado("ACTIVA");
                activas.stream()
                        .filter(m -> {
                            HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                            return h != null && idCliente.equals(h.getIdCliente());
                        })
                        .findFirst()
                        .ifPresentOrElse(
                                m -> info.append(String.format("Membresía activa: %s | Vence: %s\n",
                                        m.getTipoMembresia(),
                                        m.getFechaVencimiento().format(fmt))),
                                () -> info.append("Sin membresía activa.\n"));

                // Últimas citas
                List<Cita> citas = citaDAO.listarPorCliente(idCliente);
                info.append(String.format("Citas registradas: %d\n", citas.size()));
                citas.stream().limit(3).forEach(cita ->
                        info.append(String.format("  * Cita #%d | %s | Instructor: %s | %s\n",
                                cita.getIdCita(), cita.getFecha().format(fmt),
                                cita.getIdInstructor(), cita.getEstado())));

                // Ingresos del mes
                List<RegistroIngreso> ingresos = ingresoDAO.listarPorCliente(idCliente);
                long ingresosMes = ingresos.stream()
                        .filter(r -> r.getFecha().getMonth() == LocalDate.now().getMonth()
                                && r.getFecha().getYear() == LocalDate.now().getYear())
                        .count();
                info.append(String.format("Ingresos este mes: %d\n", ingresosMes));

                contextoExtra = " [SISTEMA: Información completa del cliente:\n" + info +
                        "Muéstrasela al administrador de forma organizada y clara.]";
            } else {
                contextoExtra = " [SISTEMA: No existe ningún cliente con ID " + idCliente +
                        ". Informa al administrador.]";
            }

            // ── ACTIVAR / DESACTIVAR CLIENTE ──────────────────────────────────
        } else if ((textoLower.contains("activar") || textoLower.contains("desactivar")
                || textoLower.contains("suspender") || textoLower.contains("inactivar"))
                && textoLower.contains("cliente")) {

            String idCliente = extraerIdCliente(texto);
            if (idCliente != null) {
                Usuario usuario = usuarioDAO.buscarPorId(idCliente);
                if (usuario != null) {
                    String nuevoEstado = textoLower.contains("activar") ? "ACTIVO" : "INACTIVO";
                    if (textoLower.contains("suspender")) nuevoEstado = "SUSPENDIDO";
                    usuario.setEstado(nuevoEstado);
                    boolean ok = usuarioDAO.actualizar(usuario);
                    contextoExtra = ok
                            ? " [SISTEMA: Cliente " + idCliente + " actualizado a estado " +
                              nuevoEstado + " exitosamente. Confirma al administrador.]"
                            : " [SISTEMA: No se pudo actualizar el estado del cliente " +
                              idCliente + ". Informa al administrador.]";
                } else {
                    contextoExtra = " [SISTEMA: No existe ningún usuario con ID " + idCliente + ".]";
                }
            } else {
                contextoExtra = " [SISTEMA: El administrador quiere cambiar el estado de un cliente " +
                        "pero no indicó el ID. Pídele el número de identificación.]";
            }

            // ── MODIFICAR CLIENTE ─────────────────────────────────────────────
        } else if ((textoLower.contains("modificar cliente") || textoLower.contains("actualizar cliente")
                || textoLower.contains("editar cliente") || textoLower.contains("cambiar datos del cliente")
                || textoLower.contains("cambiar datos de cliente") || textoLower.contains("actualizar datos del cliente")
                || textoLower.contains("actualizar datos de cliente") || textoLower.contains("editar datos del cliente"))
                && !textoLower.contains("crear") && !textoLower.contains("registrar")) {

            contextoExtra = procesarModificarCliente(texto, historial);

            // ── CREAR CITA ────────────────────────────────────────────────────
        } else if ((textoLower.contains("crea") || textoLower.contains("agenda")
                || textoLower.contains("agendar") || textoLower.contains("programa")
                || textoLower.contains("registra"))
                && textoLower.contains("cita")
                && !textoLower.contains("cancelar") && !textoLower.contains("eliminar")
                && !textoLower.contains("ver") && !textoLower.contains("listar")) {

            String[] resultado = agendarCitaAdmin(texto, historial);
            int idCitaCreada = Integer.parseInt(resultado[0]);
            String mensajeError = resultado[1];

            if (idCitaCreada > 0) {
                Cita cita = citaDAO.buscarPorId(idCitaCreada);
                String nombreInst = obtenerNombreInstructor(cita.getIdInstructor());
                String fechaFmt   = cita.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                String horaFmt    = cita.getHora().format(DateTimeFormatter.ofPattern("hh:mm a"));

                notifService.enviarSmsDirecto(
                        "GYMBROT: Cita #" + idCitaCreada + " creada. " +
                                "Instructor: " + nombreInst + " | Fecha: " + fechaFmt +
                                " | Hora: " + horaFmt);

                Cliente cliente = clienteDAO.buscarPorId(cita.getIdCliente());
                if (cliente != null && cliente.getCorreo() != null) {
                    String nombreLimpio = cliente.getNombre().split(" ")[0];
                    emailService.enviarCorreo(cliente.getCorreo(),
                            "Cita agendada - GYMBROT",
                            "<html>" +
                                    "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>" +
                                    "<div style='background: #00b4d8; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                                    "<h2 style='margin: 0;'>GYMBROT</h2>" +
                                    "</div>" +
                                    "<div style='padding: 20px;'>" +
                                    "<p>Hola <strong>" + nombreLimpio + "</strong>,</p>" +
                                    "<p>Tu cita ha sido agendada exitosamente.</p>" +
                                    "<table style='width: 100%; border-collapse: collapse; margin: 15px 0;'>" +
                                    "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>Instructor:</strong></td><td style='padding: 8px;'>" + nombreInst + "</td></tr>" +
                                    "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>Fecha:</strong></td><td style='padding: 8px;'>" + fechaFmt + "</td></tr>" +
                                    "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>Hora:</strong></td><td style='padding: 8px;'>" + horaFmt + "</td></tr>" +
                                    "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>ID de cita:</strong></td><td style='padding: 8px;'>#" + idCitaCreada + "</td></tr>" +
                                    "</table>" +
                                    "<p>Recuerda llegar 10 minutos antes.</p>" +
                                    "<p>Te esperamos en GYMBROT.</p>" +
                                    "</div>" +
                                    "<div style='background: #f4f4f4; padding: 10px; text-align: center; font-size: 12px; border-radius: 0 0 8px 8px;'>" +
                                    "<p style='margin: 0;'>© 2026 GYMBROT Valledupar</p>" +
                                    "</div>" +
                                    "</div>" +
                                    "</body>" +
                                    "</html>");
                }

                contextoExtra = " [SISTEMA: Cita #" + idCitaCreada + " creada exitosamente. " +
                        "Instructor: " + nombreInst + " | Fecha: " + fechaFmt +
                        " | Hora: " + horaFmt + ". " +
                        "SMS y correo enviados al cliente. Confirma todos los detalles al administrador.]";
            } else {
                contextoExtra = " [SISTEMA: No se pudo crear la cita. Motivo: " + mensajeError +
                        ". Informa al administrador con este motivo exacto y pídele que corrija los datos.]";
            }

            // ── CANCELAR CITA ─────────────────────────────────────────────────
        } else if ((textoLower.contains("cancelar") || textoLower.contains("eliminar")
                || textoLower.contains("borrar")) && textoLower.contains("cita")) {

            int idCita = extraerIdCita(texto);
            if (idCita > 0) {
                Cita cita = citaDAO.buscarPorId(idCita);
                if (cita != null) {
                    boolean cancelada = citaService.cancelarCita(idCita);
                    if (cancelada) {
                        String nombreInst = obtenerNombreInstructor(cita.getIdInstructor());
                        String fechaFmt   = cita.getFecha()
                                .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        String horaFmt    = cita.getHora()
                                .format(DateTimeFormatter.ofPattern("hh:mm a"));

                        notifService.enviarSmsDirecto(
                                "GYMBROT: Tu cita #" + idCita + " del " + fechaFmt +
                                        " a las " + horaFmt + " con " + nombreInst + " fue cancelada.");

                        Cliente cliente = clienteDAO.buscarPorId(cita.getIdCliente());
                        if (cliente != null && cliente.getCorreo() != null) {
                            String nombreLimpio = cliente.getNombre().split(" ")[0];
                            emailService.enviarCorreo(cliente.getCorreo(),
                                    "Cita cancelada - GYMBROT",
                                    "<html>" +
                                            "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                                            "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>" +
                                            "<div style='background: #ff6b6b; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                                            "<h2 style='margin: 0;'>GYMBROT</h2>" +
                                            "</div>" +
                                            "<div style='padding: 20px;'>" +
                                            "<p>Hola <strong>" + nombreLimpio + "</strong>,</p>" +
                                            "<p>Tu cita ha sido cancelada exitosamente.</p>" +
                                            "<table style='width: 100%; border-collapse: collapse; margin: 15px 0;'>" +
                                            "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>Instructor:</strong></td><td style='padding: 8px;'>" + nombreInst + "</td></tr>" +
                                            "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>Fecha:</strong></td><td style='padding: 8px;'>" + fechaFmt + "</td></tr>" +
                                            "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>Hora:</strong></td><td style='padding: 8px;'>" + horaFmt + "</td></tr>" +
                                            "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>ID de cita:</strong></td><td style='padding: 8px;'>#" + idCita + "</td></tr>" +
                                            "</table>" +
                                            "<p>Contáctanos para reagendar una nueva cita.</p>" +
                                            "</div>" +
                                            "<div style='background: #f4f4f4; padding: 10px; text-align: center; font-size: 12px; border-radius: 0 0 8px 8px;'>" +
                                            "<p style='margin: 0;'>© 2026 GYMBROT Valledupar</p>" +
                                            "</div>" +
                                            "</div>" +
                                            "</body>" +
                                            "</html>");
                        }

                        contextoExtra = " [SISTEMA: Cita #" + idCita + " cancelada exitosamente. " +
                                "SMS y correo enviados al cliente. Confirma al administrador.]";
                    } else {
                        contextoExtra = " [SISTEMA: No se pudo cancelar la cita #" + idCita +
                                ". Puede que ya esté cancelada. Informa al administrador.]";
                    }
                } else {
                    contextoExtra = " [SISTEMA: No existe la cita #" + idCita + ".]";
                }
            } else {
                contextoExtra = " [SISTEMA: El administrador quiere cancelar una cita pero no " +
                        "especificó el ID. Pídele el número. Ejemplo: 'cancelar cita #12']";
            }

            // ── VER CITAS DEL DÍA ─────────────────────────────────────────────
        } else if ((textoLower.contains("citas") || textoLower.contains("agenda"))
                && (textoLower.contains("hoy") || textoLower.contains("pendientes")
                || textoLower.contains("ver") || textoLower.contains("listar"))) {

            List<Cita> citasHoy = citaDAO.listarPorFecha(LocalDate.now());
            if (!citasHoy.isEmpty()) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("HH:mm");
                StringBuilder lista = new StringBuilder();
                for (Cita c : citasHoy) {
                    String nombreInst = obtenerNombreInstructor(c.getIdInstructor());
                    lista.append(String.format(
                            "Cita #%d | Cliente: %s | Instructor: %s | Hora: %s | Estado: %s\n",
                            c.getIdCita(), c.getIdCliente(), nombreInst,
                            c.getHora().format(fmt), c.getEstado()));
                }
                contextoExtra = " [SISTEMA: Citas de hoy:\n" + lista +
                        "Muéstralas en formato claro al administrador.]";
            } else {
                contextoExtra = " [SISTEMA: No hay citas programadas para hoy.]";
            }

            // ── MEMBRESÍAS POR VENCER / VENCIDAS ─────────────────────────────
        } else if (textoLower.contains("membres") || textoLower.contains("venc")
                || textoLower.contains("renovar")) {

            LocalDate hoy = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            List<Membresia> vencidas  = membresiaDAO.listarVencidas();
            List<Membresia> activas   = membresiaDAO.listarPorEstado("ACTIVA");
            List<Membresia> porVencer = activas.stream()
                    .filter(m -> !m.getFechaVencimiento().isAfter(hoy.plusDays(7)))
                    .collect(Collectors.toList());

            StringBuilder lista = new StringBuilder();
            if (!vencidas.isEmpty()) {
                lista.append(String.format("VENCIDAS (%d):\n", vencidas.size()));
                for (Membresia m : vencidas) {
                    HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                    String idCliente = h != null ? h.getIdCliente() : "N/A";
                    lista.append(String.format("- #%d | Cliente: %s | Tipo: %s | Venció: %s\n",
                            m.getIdMembresia(), idCliente, m.getTipoMembresia(),
                            m.getFechaVencimiento().format(fmt)));
                }
            }
            if (!porVencer.isEmpty()) {
                lista.append("POR VENCER (próximos 7 días):\n");
                for (Membresia m : porVencer) {
                    HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                    String idCliente = h != null ? h.getIdCliente() : "N/A";
                    long dias = java.time.temporal.ChronoUnit.DAYS
                            .between(hoy, m.getFechaVencimiento());
                    lista.append(String.format(
                            "- #%d | Cliente: %s | Tipo: %s | Vence: %s | En %d días\n",
                            m.getIdMembresia(), idCliente, m.getTipoMembresia(),
                            m.getFechaVencimiento().format(fmt), dias));
                    if (dias == 7 || dias == 3 || dias == 1) {
                        Cliente cliente = clienteDAO.buscarPorId(idCliente);
                        if (cliente != null) {
                            String nombreLimpio = cliente.getNombre().split(" ")[0];
                            notifService.enviarSmsDirecto(
                                    "GYMBROT: Hola " + nombreLimpio + ", tu membresía " +
                                            m.getTipoMembresia() + " vence el " +
                                            m.getFechaVencimiento().format(fmt) +
                                            ". Renuévala pronto.");
                            if (cliente.getCorreo() != null) {
                                emailService.enviarCorreo(cliente.getCorreo(),
                                        "Tu membresía está por vencer - GYMBROT",
                                        "<html>" +
                                                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                                                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>" +
                                                "<div style='background: #ffaa00; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                                                "<h2 style='margin: 0;'>GYMBROT</h2>" +
                                                "</div>" +
                                                "<div style='padding: 20px;'>" +
                                                "<p>Hola <strong>" + nombreLimpio + "</strong>,</p>" +
                                                "<p>Te recordamos que tu membresía <strong>" + m.getTipoMembresia() +
                                                "</strong> vence en <strong>" + dias + " días</strong>.</p>" +
                                                "<p><strong>Fecha de vencimiento:</strong> " + m.getFechaVencimiento().format(fmt) + "</p>" +
                                                "<p>Renueva ahora y sigue entrenando sin interrupciones.</p>" +
                                                "<p>Contáctanos para más información.</p>" +
                                                "</div>" +
                                                "<div style='background: #f4f4f4; padding: 10px; text-align: center; font-size: 12px; border-radius: 0 0 8px 8px;'>" +
                                                "<p style='margin: 0;'>© 2026 GYMBROT Valledupar</p>" +
                                                "</div>" +
                                                "</div>" +
                                                "</body>" +
                                                "</html>");
                            }
                        }
                    }
                }
            }
            if (vencidas.isEmpty() && porVencer.isEmpty())
                lista.append("No hay membresías vencidas ni por vencer en los próximos 7 días.\n");

            contextoExtra = " [SISTEMA: " + lista +
                    "Muéstraselas al administrador de forma organizada.]";

            // ── ESTADÍSTICAS ──────────────────────────────────────────────────
        } else if (textoLower.contains("estadística") || textoLower.contains("estadistica")
                || textoLower.contains("resumen") || textoLower.contains("reporte")
                || textoLower.contains("cuantos") || textoLower.contains("total")) {

            String stats = consultaService.buscarInfoRelevante(texto);
            if (!stats.isEmpty()) {
                contextoExtra = " [SISTEMA: " + stats +
                        "Presenta estas estadísticas de forma clara al administrador.]";
            }
        }

        // ── Llamar a Groq ─────────────────────────────────────────────────
        String respuesta = groqService.enviarMensaje(texto + contextoExtra, historial);

        // ── Guardar respuesta ─────────────────────────────────────────────
        MensajeGymbrot msgBot = new MensajeGymbrot();
        msgBot.setIdSesion(idSesion);
        msgBot.setRemitente("BOT");
        msgBot.setContenido(respuesta);
        msgBot.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgBot);

        return respuesta;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CREAR RUTINA
    // ═════════════════════════════════════════════════════════════════════
    private String procesarCrearRutina(String texto, List<MensajeGymbrot> historial, String idAdmin) {
        System.out.println("[procesarCrearRutina] Iniciando...");

        String idCliente = extraerIdCliente(texto);
        if (idCliente == null) {
            return " [SISTEMA: No se encontró el ID del cliente. Pídele al administrador que indique el ID.]";
        }

        System.out.println("[procesarCrearRutina] Cliente ID: " + idCliente);

        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) {
            return " [SISTEMA: No existe un cliente con ID " + idCliente + ". Informa al administrador.]";
        }

        String objetivo = extraerObjetivoRutina(texto);
        if (objetivo == null) objetivo = "mejorar condición física";

        String diasSemana = extraerDiasSemanaRutina(texto);
        if (diasSemana == null) diasSemana = "Lunes, Miércoles, Viernes";

        System.out.println("[procesarCrearRutina] Objetivo: " + objetivo);
        System.out.println("[procesarCrearRutina] Días: " + diasSemana);

        String prompt = "Crea una rutina de ejercicios semanal para un cliente de gimnasio con objetivo: " + objetivo +
                ". Los días de entrenamiento son: " + diasSemana +
                ". Incluye series y repeticiones para cada ejercicio. Da solo la rutina, sin explicaciones adicionales.";

        String rutinaGenerada = groqService.enviarMensaje(prompt, historial);
        System.out.println("[procesarCrearRutina] Rutina generada por IA");

        Rutina rutina = new Rutina();
        rutina.setIdCliente(idCliente);
        rutina.setIdInstructor("INS001");
        rutina.setNombre("Rutina - " + objetivo);
        rutina.setDescripcion(rutinaGenerada);
        rutina.setFechaCreacion(LocalDate.now());
        rutina.setDiasSemana(diasSemana);
        rutina.setObjetivo(objetivo);
        rutina.setFechaFin(null);

        int idRutina = insertarRutinaYRetornarId(rutina);
        System.out.println("[procesarCrearRutina] ID Rutina: " + idRutina);

        if (idRutina > 0) {
            String nombreLimpio = cliente.getNombre().split(" ")[0];

            notifService.enviarSmsDirecto(
                    "GYMBROT: Hola " + nombreLimpio + "! Se ha creado una nueva rutina para ti. " +
                            "Objetivo: " + objetivo + ". Días: " + diasSemana + ". " +
                            "Consulta tu correo para más detalles.");

            if (cliente.getCorreo() != null) {
                emailService.enviarCorreo(cliente.getCorreo(),
                        "Nueva rutina asignada - GYMBROT",
                        "<html>" +
                                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                                "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>" +
                                "<div style='background: #00b4d8; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                                "<h2 style='margin: 0;'>GYMBROT</h2>" +
                                "</div>" +
                                "<div style='padding: 20px;'>" +
                                "<p>Hola <strong>" + nombreLimpio + "</strong>,</p>" +
                                "<p>Tu instructor <strong>Pedro</strong> ha creado una nueva rutina personalizada para ti.</p>" +
                                "<table style='width: 100%; border-collapse: collapse; margin: 15px 0;'>" +
                                "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>Objetivo:</strong></td><td style='padding: 8px;'>" + objetivo + "</td></tr>" +
                                "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>Días de entrenamiento:</strong></td><td style='padding: 8px;'>" + diasSemana + "</td></tr>" +
                                "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>Instructor:</strong></td><td style='padding: 8px;'>Pedro</td></tr>" +
                                "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>ID de rutina:</strong></td><td style='padding: 8px;'>#" + idRutina + "</td></tr>" +
                                "</table>" +
                                "<div style='background: #f4f4f4; padding: 15px; border-radius: 8px; font-family: monospace; white-space: pre-wrap; margin: 15px 0;'>" +
                                rutinaGenerada.replace("\n", "<br>") +
                                "</div>" +
                                "<p style='margin-top: 20px;'>¡A entrenar! Recuerda mantener una buena hidratación y alimentación.</p>" +
                                "<p>¿Tienes dudas? Comunícate con tu instructor o responde este correo.</p>" +
                                "</div>" +
                                "<div style='background: #f4f4f4; padding: 10px; text-align: center; font-size: 12px; border-radius: 0 0 8px 8px;'>" +
                                "<p style='margin: 0;'>© 2026 GYMBROT Valledupar</p>" +
                                "</div>" +
                                "</div>" +
                                "</body>" +
                                "</html>");
            }

            return " [SISTEMA: Rutina creada exitosamente para el cliente " + nombreLimpio +
                    " (ID: " + idCliente + "). ID de rutina: #" + idRutina +
                    ". Objetivo: " + objetivo + ". Días: " + diasSemana +
                    ". SMS y correo enviados al cliente.]";
        } else {
            return " [SISTEMA: No se pudo guardar la rutina en la base de datos. Informa al administrador.]";
        }
    }

    private int insertarRutinaYRetornarId(Rutina rutina) {
        String sql = "INSERT INTO RUTINAS (id_instructor, id_cliente, nombre, descripcion, fecha_creacion, fecha_fin, dias_semana, objetivo) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = org.gymbrot.util.DatabaseConnection.getInstance();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"id_rutina"})) {

            ps.setString(1, rutina.getIdInstructor());
            ps.setString(2, rutina.getIdCliente());
            ps.setString(3, rutina.getNombre());
            ps.setString(4, rutina.getDescripcion());
            ps.setDate(5, Date.valueOf(rutina.getFechaCreacion()));
            ps.setDate(6, rutina.getFechaFin() != null ? Date.valueOf(rutina.getFechaFin()) : null);
            ps.setString(7, rutina.getDiasSemana());
            ps.setString(8, rutina.getObjetivo());

            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("[insertarRutinaYRetornarId] Error: " + e.getMessage());
            e.printStackTrace();
        }
        return -1;
    }

    private String extraerObjetivoRutina(String texto) {
        Pattern p = Pattern.compile("objetivo[:\\s]+([\\w\\s]+?)(?:,|\\.|$)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) return m.group(1).trim();

        if (texto.toLowerCase().contains("masa muscular")) return "ganar masa muscular";
        if (texto.toLowerCase().contains("perder peso")) return "perder peso";
        if (texto.toLowerCase().contains("definición")) return "definición muscular";
        if (texto.toLowerCase().contains("resistencia")) return "mejorar resistencia";
        return null;
    }

    private String extraerDiasSemanaRutina(String texto) {
        Pattern p = Pattern.compile("dias[:\\s]+([\\w\\s,]+?)(?:,|\\.|$)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    // ═════════════════════════════════════════════════════════════════════
    //  MODIFICAR CLIENTE
    // ═════════════════════════════════════════════════════════════════════
    private String procesarModificarCliente(String texto, List<MensajeGymbrot> historial) {

        String id = extraerIdCliente(texto);
        if (id == null && historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                MensajeGymbrot msg = historial.get(i);
                if (msg.getRemitente().equals("CLIENTE")) {
                    id = extraerIdCliente(msg.getContenido());
                    if (id != null) break;
                }
            }
        }
        if (id == null) {
            return " [SISTEMA: El administrador quiere modificar un cliente pero no indicó el ID. " +
                    "Pídele el número de identificación del cliente a modificar.]";
        }

        Cliente clienteActual = clienteDAO.buscarPorId(id);
        if (clienteActual == null) {
            return " [SISTEMA: No existe ningún cliente con ID " + id +
                    ". Verifica el número de identificación e inténtalo de nuevo.]";
        }

        System.out.println("[procesarModificarCliente] Modificando cliente id=" + id);

        String nuevoNombre    = extraerNombre(texto, historial);
        String nuevosApellidos= extraerApellidos(texto, historial);
        String nuevoCorreo    = extraerCorreo(texto, historial);
        String nuevoTelefono  = extraerTelefono(texto, historial);
        String nuevaDireccion = extraerDireccion(texto, historial);
        LocalDate nuevaFechaNac = extraerFecha(texto, historial);

        System.out.println("[procesarModificarCliente] Nuevos datos recibidos — " +
                "nombre=" + nuevoNombre + " | apellidos=" + nuevosApellidos +
                " | correo=" + nuevoCorreo + " | telefono=" + nuevoTelefono +
                " | direccion=" + nuevaDireccion + " | fechaNac=" + nuevaFechaNac);

        if (nuevoNombre == null && nuevosApellidos == null && nuevoCorreo == null
                && nuevoTelefono == null && nuevaDireccion == null && nuevaFechaNac == null) {
            return " [SISTEMA: El administrador quiere modificar el cliente " + id +
                    " pero no indicó qué campos cambiar. Pídele que especifique: " +
                    "nombre, apellidos, correo, teléfono, dirección o fecha de nacimiento.]";
        }

        if (nuevoCorreo != null && !nuevoCorreo.equalsIgnoreCase(clienteActual.getCorreo())) {
            Usuario existente = usuarioDAO.buscarPorCorreo(nuevoCorreo);
            if (existente != null && !existente.getNumeroIdentificacion().equals(id)) {
                return " [SISTEMA: El correo " + nuevoCorreo +
                        " ya está registrado por otro usuario. " +
                        "No se puede asignar. Informa al administrador.]";
            }
        }

        StringBuilder cambiosLog = new StringBuilder();

        if (nuevoNombre != null && !nuevoNombre.equalsIgnoreCase(clienteActual.getNombre())) {
            cambiosLog.append("nombre: ").append(clienteActual.getNombre())
                    .append(" → ").append(nuevoNombre).append(" | ");
            clienteActual.setNombre(nuevoNombre);
        }
        if (nuevosApellidos != null && !nuevosApellidos.equalsIgnoreCase(clienteActual.getApellidos())) {
            cambiosLog.append("apellidos: ").append(clienteActual.getApellidos())
                    .append(" → ").append(nuevosApellidos).append(" | ");
            clienteActual.setApellidos(nuevosApellidos);
        }
        if (nuevoCorreo != null && !nuevoCorreo.equalsIgnoreCase(clienteActual.getCorreo())) {
            cambiosLog.append("correo: ").append(clienteActual.getCorreo())
                    .append(" → ").append(nuevoCorreo).append(" | ");
            clienteActual.setCorreo(nuevoCorreo);
        }
        if (nuevoTelefono != null && !nuevoTelefono.equals(clienteActual.getTelefono())) {
            cambiosLog.append("telefono: ").append(clienteActual.getTelefono())
                    .append(" → ").append(nuevoTelefono).append(" | ");
            clienteActual.setTelefono(nuevoTelefono);
        }
        if (nuevaDireccion != null && !nuevaDireccion.equalsIgnoreCase(clienteActual.getDireccion())) {
            cambiosLog.append("direccion: ").append(clienteActual.getDireccion())
                    .append(" → ").append(nuevaDireccion).append(" | ");
            clienteActual.setDireccion(nuevaDireccion);
        }
        if (nuevaFechaNac != null && !nuevaFechaNac.equals(clienteActual.getFechaNacimiento())) {
            cambiosLog.append("fechaNacimiento: ").append(clienteActual.getFechaNacimiento())
                    .append(" → ").append(nuevaFechaNac).append(" | ");
            clienteActual.setFechaNacimiento(nuevaFechaNac);
        }

        if (cambiosLog.length() == 0) {
            return " [SISTEMA: El cliente " + id +
                    " ya tiene los mismos datos que se intentaron asignar. No se realizó ningún cambio.]";
        }

        System.out.println("[procesarModificarCliente] Cambios a aplicar: " + cambiosLog);

        boolean usuarioActualizado = usuarioDAO.actualizar(clienteActual);
        System.out.println("[procesarModificarCliente] USUARIOS actualizado=" + usuarioActualizado);

        if (!usuarioActualizado) {
            return " [SISTEMA: Error al actualizar la tabla USUARIOS para el cliente " + id +
                    ". Revisa los logs del servidor. No se guardó ningún cambio.]";
        }

        boolean clienteActualizado = clienteDAO.actualizar(clienteActual);
        System.out.println("[procesarModificarCliente] CLIENTES actualizado=" + clienteActualizado);

        if (!clienteActualizado) {
            return " [SISTEMA: USUARIOS fue actualizado correctamente, pero hubo un error " +
                    "al actualizar la tabla CLIENTES para el cliente " + id +
                    ". Revisa los logs del servidor.]";
        }

        String nombreLimpio = clienteActual.getNombre().split(" ")[0];
        notifService.enviarSmsDirecto(
                "GYMBROT: Hola " + nombreLimpio +
                        ", tus datos han sido actualizados en el sistema. " +
                        "Si no realizaste este cambio, contáctanos.");

        if (clienteActual.getCorreo() != null) {
            emailService.enviarCorreo(
                    clienteActual.getCorreo(),
                    "Datos actualizados - GYMBROT",
                    "<html>" +
                            "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                            "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>" +
                            "<div style='background: #00b4d8; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                            "<h2 style='margin: 0;'>GYMBROT</h2>" +
                            "</div>" +
                            "<div style='padding: 20px;'>" +
                            "<p>Hola <strong>" + nombreLimpio + "</strong>,</p>" +
                            "<p>Tus datos en GYMBROT han sido actualizados exitosamente.</p>" +
                            "<p>Si no reconoces este cambio, comunícate con nosotros de inmediato.</p>" +
                            "</div>" +
                            "<div style='background: #f4f4f4; padding: 10px; text-align: center; font-size: 12px; border-radius: 0 0 8px 8px;'>" +
                            "<p style='margin: 0;'>© 2026 GYMBROT Valledupar</p>" +
                            "</div>" +
                            "</div>" +
                            "</body>" +
                            "</html>");
        }

        String cambiosResumen = cambiosLog.toString().replaceAll(" \\| $", "");
        return " [SISTEMA: Cliente " + id + " (" + clienteActual.getNombre() + " " +
                clienteActual.getApellidos() + ") modificado exitosamente en USUARIOS y CLIENTES. " +
                "Cambios aplicados: " + cambiosResumen + ". " +
                "SMS y correo de notificación enviados. Confirma al administrador todos los cambios.]";
    }

    // ═════════════════════════════════════════════════════════════════════
    //  CREAR CLIENTE
    // ═════════════════════════════════════════════════════════════════════
    private String procesarCrearCliente(String texto, List<MensajeGymbrot> historial) {

        String nombre    = extraerNombre(texto, historial);
        String apellidos = extraerApellidos(texto, historial);
        String id        = extraerIdCliente(texto);
        String correo    = extraerCorreo(texto, historial);
        String telefono  = extraerTelefono(texto, historial);
        String direccion = extraerDireccion(texto, historial);

        System.out.println("[procesarCrearCliente] nombre="    + nombre
                + " | apellidos=" + apellidos
                + " | id="        + id
                + " | correo="    + correo
                + " | telefono="  + telefono
                + " | direccion=" + direccion);

        if (nombre == null || id == null || correo == null) {
            StringBuilder faltantes = new StringBuilder("Faltan datos para crear el cliente: ");
            if (nombre == null)  faltantes.append("nombre, ");
            if (id == null)      faltantes.append("número de identificación, ");
            if (correo == null)  faltantes.append("correo electrónico, ");
            return " [SISTEMA: " + faltantes.toString().replaceAll(", $", "") +
                    ". Pídele al administrador que los proporcione.]";
        }

        if (usuarioDAO.buscarPorId(id) != null) {
            return " [SISTEMA: Ya existe un usuario con ID " + id +
                    ". No se puede crear duplicado. Informa al administrador.]";
        }

        if (usuarioDAO.buscarPorCorreo(correo) != null) {
            return " [SISTEMA: Ya existe un usuario con el correo " + correo +
                    ". No se puede crear duplicado. Informa al administrador.]";
        }

        Usuario usuario = new Usuario();
        usuario.setNumeroIdentificacion(id);
        usuario.setTipoIdentificacion("CC");
        usuario.setNombre(nombre);
        usuario.setApellidos(apellidos != null ? apellidos : "");
        usuario.setTelefono(telefono   != null ? telefono  : "");
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(id);
        usuario.setFotoUrl(null);
        usuario.setEstado("ACTIVO");
        usuario.setFechaRegistro(LocalDate.now());
        usuario.setTipoUsuario("CLIENTE");

        System.out.println("[procesarCrearCliente] Intentando insertar USUARIO id=" + id);
        boolean usuarioCreado = usuarioDAO.insertar(usuario);
        System.out.println("[procesarCrearCliente] USUARIO insertado=" + usuarioCreado);

        if (!usuarioCreado) {
            return " [SISTEMA: Error al insertar en la tabla USUARIOS. " +
                    "Revisa los logs del servidor. Informa al administrador.]";
        }

        Cliente cliente = new Cliente();
        cliente.setNumeroIdentificacion(id);
        cliente.setDireccion(direccion != null ? direccion : "");
        cliente.setFechaNacimiento(null);
        cliente.setHuellaDactilar(null);

        System.out.println("[procesarCrearCliente] Intentando insertar CLIENTE id=" + id);
        boolean clienteCreado = insertarClienteSeguro(cliente);
        System.out.println("[procesarCrearCliente] CLIENTE insertado=" + clienteCreado);

        if (!clienteCreado) {
            usuarioDAO.eliminar(id);
            System.out.println("[procesarCrearCliente] Rollback: USUARIO " + id + " eliminado.");
            return " [SISTEMA: Error al insertar en la tabla CLIENTES. " +
                    "El registro en USUARIOS fue revertido automáticamente. " +
                    "Revisa los logs del servidor. Informa al administrador.]";
        }

        String nombreLimpio = nombre.split(" ")[0];

        notifService.enviarSmsDirecto(
                "GYMBROT: Hola " + nombreLimpio + "! Tu cuenta ha sido creada exitosamente. " +
                        "Usuario: " + correo + " - Contraseña temporal: " + id + ". " +
                        "Guarda tus credenciales para futuras validaciones.");

        emailService.enviarCorreo(correo, "Bienvenido a GYMBROT",
                "<html>" +
                        "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                        "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>" +
                        "<div style='background: #00b4d8; color: white; padding: 20px; text-align: center; border-radius: 8px 8px 0 0;'>" +
                        "<h2 style='margin: 0;'>GYMBROT</h2>" +
                        "</div>" +
                        "<div style='padding: 20px;'>" +
                        "<p>Hola <strong>" + nombreLimpio + "</strong>,</p>" +
                        "<p>Tu cuenta ha sido creada exitosamente en el sistema GYMBROT.</p>" +
                        "<h3 style='margin: 20px 0 10px;'>Tus credenciales de acceso:</h3>" +
                        "<table style='width: 100%; border-collapse: collapse; margin: 10px 0;'>" +
                        "<tr><td style='padding: 8px; background: #f4f4f4;'><strong>Usuario:</strong><td>" +
                        "<td style='padding: 8px;'>" + correo + "</td>" +
                        "</tr>" +
                        "<tr>" +
                        "<td style='padding: 8px; background: #f4f4f4;'><strong>Contraseña temporal:</strong></td>" +
                        "<td style='padding: 8px;'><strong>" + id + "</strong></td>" +
                        "</tr>" +
                        "</table>" +
                        "<p style='margin-top: 20px;'><strong>Recomendación:</strong> Cambia tu contraseña al ingresar por primera vez.</p>" +
                        "<p>Para agendar citas, consultar horarios o resolver dudas, comunícate con nuestro equipo de atención al cliente.</p>" +
                        "<p style='margin-top: 20px;'>Atentamente,</p>" +
                        "<p><strong>GYMBROT Valledupar</strong></p>" +
                        "</div>" +
                        "<div style='background: #f4f4f4; padding: 10px; text-align: center; font-size: 12px; border-radius: 0 0 8px 8px;'>" +
                        "<p style='margin: 0;'>© 2026 GYMBROT Valledupar</p>" +
                        "</div>" +
                        "</div>" +
                        "</body>" +
                        "</html>");

        return " [SISTEMA: Cliente " + nombreLimpio + " " + (apellidos != null ? apellidos : "") +
                " (ID: " + id + ") creado exitosamente en USUARIOS y CLIENTES. " +
                "SMS y correo de bienvenida enviados a " + correo + ". " +
                "Contraseña temporal: número de identificación. Confirma al administrador.]";
    }

    // ═════════════════════════════════════════════════════════════════════
    //  INSERTAR CLIENTE SEGURO
    // ═════════════════════════════════════════════════════════════════════
    private boolean insertarClienteSeguro(Cliente cliente) {
        String sql = "INSERT INTO CLIENTES (numero_identificacion, direccion, " +
                "fecha_nacimiento, huella_dactilar) VALUES (?, ?, ?, ?)";

        try (java.sql.Connection conn = org.gymbrot.util.DatabaseConnection.getInstance();
             java.sql.PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cliente.getNumeroIdentificacion());
            pstmt.setString(2, cliente.getDireccion());

            if (cliente.getFechaNacimiento() != null) {
                pstmt.setDate(3, java.sql.Date.valueOf(cliente.getFechaNacimiento()));
            } else {
                pstmt.setNull(3, java.sql.Types.DATE);
            }

            if (cliente.getHuellaDactilar() != null) {
                pstmt.setBytes(4, cliente.getHuellaDactilar());
            } else {
                pstmt.setNull(4, java.sql.Types.BLOB);
            }

            int filas = pstmt.executeUpdate();
            System.out.println("[insertarClienteSeguro] filas afectadas=" + filas);
            return filas > 0;

        } catch (java.sql.SQLException e) {
            System.err.println("[insertarClienteSeguro] ERROR SQL: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  AGENDAR CITA DESDE ADMIN
    // ═════════════════════════════════════════════════════════════════════
    private String[] agendarCitaAdmin(String texto, List<MensajeGymbrot> historial) {
        try {
            List<Instructor> instructores = instructorDAO.listarTodos();

            String idInstructor  = null;
            String disponibilidad = "";

            for (Instructor i : instructores) {
                if (texto.toLowerCase().contains(i.getNombre().toLowerCase())) {
                    idInstructor  = i.getNumeroIdentificacion();
                    disponibilidad = i.getDisponibilidad();
                    break;
                }
            }
            if (idInstructor == null && historial != null) {
                outer:
                for (int i = historial.size() - 1; i >= 0; i--) {
                    MensajeGymbrot msg = historial.get(i);
                    if (msg.getRemitente().equals("CLIENTE")) {
                        for (Instructor inst : instructores) {
                            if (msg.getContenido().toLowerCase()
                                    .contains(inst.getNombre().toLowerCase())) {
                                idInstructor  = inst.getNumeroIdentificacion();
                                disponibilidad = inst.getDisponibilidad();
                                break outer;
                            }
                        }
                    }
                }
            }
            if (idInstructor == null)
                return new String[]{"-1",
                        "No se encontró el instructor. Indica el nombre del instructor."};

            String idCliente = extraerIdCliente(texto);
            if (idCliente == null && historial != null) {
                for (int i = historial.size() - 1; i >= 0; i--) {
                    MensajeGymbrot msg = historial.get(i);
                    if (msg.getRemitente().equals("CLIENTE")) {
                        idCliente = extraerIdCliente(msg.getContenido());
                        if (idCliente != null) break;
                    }
                }
            }
            if (idCliente == null)
                return new String[]{"-1",
                        "No se encontró el ID del cliente. Indica el número de identificación."};

            if (clienteDAO.buscarPorId(idCliente) == null)
                return new String[]{"-1",
                        "El cliente con ID " + idCliente + " no existe en la base de datos."};

            LocalDate fecha = extraerFecha(texto, historial);
            if (fecha == null)
                return new String[]{"-1",
                        "No se encontró la fecha. Indica el día (ejemplo: lunes, martes)."};

            LocalTime hora = extraerHora(texto, historial);
            if (hora == null)
                return new String[]{"-1",
                        "No se encontró la hora. Indica la hora (ejemplo: 10am, 2pm)."};

            Cita cita = new Cita();
            cita.setIdCliente(idCliente);
            cita.setIdInstructor(idInstructor);
            cita.setFecha(fecha);
            cita.setHora(hora);
            cita.setTipoCita("CONSULTA");
            cita.setNotas("Cita creada por administrador desde GYMBROT AI");

            int idCita = citaService.programarCitaAdminYRetornarId(cita);

            if (idCita > 0)   return new String[]{String.valueOf(idCita), ""};
            if (idCita == -7) return new String[]{"-1",
                    "El instructor no trabaja ese día. Su disponibilidad es: " +
                            disponibilidad + ". Indica un día válido."};
            if (idCita == -8) return new String[]{"-1",
                    "La hora está fuera del horario del instructor. Su disponibilidad es: " +
                            disponibilidad + ". Indica una hora válida."};
            if (idCita == -9) return new String[]{"-1",
                    "El instructor ya tiene una cita en ese horario. Indica otra hora."};
            return new String[]{"-1", "No se pudo crear la cita. Verifica los datos."};

        } catch (Exception e) {
            System.err.println("[agendarCitaAdmin] EXCEPTION: " + e.getMessage());
            e.printStackTrace();
            return new String[]{"-1", "Error interno: " + e.getMessage()};
        }
    }

    // ═════════════════════════════════════════════════════════════════════
    //  EXTRACTORES DE DATOS
    // ═════════════════════════════════════════════════════════════════════

    private String extraerNombre(String texto, List<MensajeGymbrot> historial) {
        Pattern p = Pattern.compile(
                "nombre[:\\s]+([\\p{L}]+(?:\\s+[\\p{L}]+)?)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        Matcher m = p.matcher(texto);
        if (m.find()) return capitalizar(m.group(1).trim());
        if (historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                if (historial.get(i).getRemitente().equals("CLIENTE")) {
                    Matcher mh = p.matcher(historial.get(i).getContenido());
                    if (mh.find()) return capitalizar(mh.group(1).trim());
                }
            }
        }
        return null;
    }

    private String extraerApellidos(String texto, List<MensajeGymbrot> historial) {
        Pattern p = Pattern.compile(
                "apellidos?[:\\s]+([\\p{L}]+(?:\\s+[\\p{L}]+)?)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        Matcher m = p.matcher(texto);
        if (m.find()) return capitalizar(m.group(1).trim());
        if (historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                if (historial.get(i).getRemitente().equals("CLIENTE")) {
                    Matcher mh = p.matcher(historial.get(i).getContenido());
                    if (mh.find()) return capitalizar(mh.group(1).trim());
                }
            }
        }
        return null;
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isEmpty()) return texto;
        String[] palabras = texto.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String palabra : palabras) {
            if (!palabra.isEmpty()) {
                sb.append(Character.toUpperCase(palabra.charAt(0)))
                        .append(palabra.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        return sb.toString().trim();
    }

    private String extraerDireccion(String texto, List<MensajeGymbrot> historial) {
        String valor = extraerDireccionDeTexto(texto);
        if (valor != null) return valor;
        if (historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                MensajeGymbrot msg = historial.get(i);
                if (msg.getRemitente().equals("CLIENTE")) {
                    valor = extraerDireccionDeTexto(msg.getContenido());
                    if (valor != null) return valor;
                }
            }
        }
        return null;
    }

    private String extraerDireccionDeTexto(String texto) {
        Pattern p = Pattern.compile(
                "(?:direcci[oó]n|dir)[:\\s]+([\\p{L}\\d\\s#\\-\\.]+?)(?:,|\\.|$)",
                Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CHARACTER_CLASS);
        Matcher m = p.matcher(texto);
        if (m.find()) {
            String val = m.group(1).trim();
            return val.isEmpty() ? null : val;
        }
        return null;
    }

    private String extraerCorreo(String texto, List<MensajeGymbrot> historial) {
        Pattern p = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        Matcher m = p.matcher(texto);
        if (m.find()) return m.group();
        if (historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                Matcher mh = p.matcher(historial.get(i).getContenido());
                if (mh.find()) return mh.group();
            }
        }
        return null;
    }

    private String extraerTelefono(String texto, List<MensajeGymbrot> historial) {
        Pattern pEtiqueta = Pattern.compile(
                "(?:tel[eé]fono|tel|celular|cel)[:\\s]+(3\\d{9})",
                Pattern.CASE_INSENSITIVE);
        Matcher mE = pEtiqueta.matcher(texto);
        if (mE.find()) return mE.group(1);

        Pattern p = Pattern.compile("\\b(3[0-9]{9})\\b");
        Matcher m = p.matcher(texto);
        if (m.find()) return m.group();

        if (historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                MensajeGymbrot msg = historial.get(i);
                if (msg.getRemitente().equals("CLIENTE")) {
                    Matcher mh = p.matcher(msg.getContenido());
                    if (mh.find()) return mh.group();
                }
            }
        }
        return null;
    }

    private String extraerIdCliente(String texto) {
        Pattern p1 = Pattern.compile(
                "(?:id|cedula|cédula|identificacion|identificación)[:\\s]+(\\d{4,12})",
                Pattern.CASE_INSENSITIVE);
        Matcher m1 = p1.matcher(texto);
        if (m1.find()) return m1.group(1);

        Pattern p2 = Pattern.compile("cliente\\s+(\\d{4,12})", Pattern.CASE_INSENSITIVE);
        Matcher m2 = p2.matcher(texto.toLowerCase());
        if (m2.find()) return m2.group(1);

        Pattern p3 = Pattern.compile("\\b(\\d{6,12})\\b");
        Matcher m3 = p3.matcher(texto);
        while (m3.find()) {
            String candidato = m3.group(1);
            if (candidato.length() == 10 && candidato.startsWith("3")) continue;
            return candidato;
        }
        return null;
    }

    private int extraerIdCita(String texto) {
        Pattern p = Pattern.compile("#(\\d+)|cita\\s+(\\d+)|id\\s+(\\d+)");
        Matcher m = p.matcher(texto.toLowerCase());
        if (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                if (m.group(i) != null) return Integer.parseInt(m.group(i));
            }
        }
        return -1;
    }

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
        Pattern p12 = Pattern.compile("(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)");
        Matcher m12 = p12.matcher(texto.toLowerCase());
        if (m12.find()) {
            int h = Integer.parseInt(m12.group(1));
            String ampm = m12.group(3);
            if (ampm.equals("pm") && h != 12) h += 12;
            if (ampm.equals("am") && h == 12) h = 0;
            int min = m12.group(2) != null ? Integer.parseInt(m12.group(2)) : 0;
            return LocalTime.of(h, min);
        }
        Pattern p24 = Pattern.compile("\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b");
        Matcher m24 = p24.matcher(texto.toLowerCase());
        if (m24.find())
            return LocalTime.of(
                    Integer.parseInt(m24.group(1)),
                    Integer.parseInt(m24.group(2)));
        return null;
    }

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
        String t = texto.toLowerCase();
        LocalDate hoy = LocalDate.now();

        if (t.contains("hoy")) return hoy;
        if (t.contains("mañana") || t.contains("manana")) return hoy.plusDays(1);

        if (t.contains("lunes")) {
            LocalDate candidato = hoy.with(java.time.DayOfWeek.MONDAY);
            return candidato.isBefore(hoy) ? candidato.plusWeeks(1) : candidato;
        }
        if (t.contains("martes")) {
            LocalDate candidato = hoy.with(java.time.DayOfWeek.TUESDAY);
            return candidato.isBefore(hoy) ? candidato.plusWeeks(1) : candidato;
        }
        if (t.contains("miércoles") || t.contains("miercoles")) {
            LocalDate candidato = hoy.with(java.time.DayOfWeek.WEDNESDAY);
            return candidato.isBefore(hoy) ? candidato.plusWeeks(1) : candidato;
        }
        if (t.contains("jueves")) {
            LocalDate candidato = hoy.with(java.time.DayOfWeek.THURSDAY);
            return candidato.isBefore(hoy) ? candidato.plusWeeks(1) : candidato;
        }
        if (t.contains("viernes")) {
            LocalDate candidato = hoy.with(java.time.DayOfWeek.FRIDAY);
            return candidato.isBefore(hoy) ? candidato.plusWeeks(1) : candidato;
        }
        if (t.contains("sábado") || t.contains("sabado")) {
            LocalDate candidato = hoy.with(java.time.DayOfWeek.SATURDAY);
            return candidato.isBefore(hoy) ? candidato.plusWeeks(1) : candidato;
        }
        if (t.contains("domingo")) {
            LocalDate candidato = hoy.with(java.time.DayOfWeek.SUNDAY);
            return candidato.isBefore(hoy) ? candidato.plusWeeks(1) : candidato;
        }

        Pattern pFecha = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})");
        Matcher mFecha = pFecha.matcher(t);
        if (mFecha.find()) {
            try {
                return LocalDate.of(
                        Integer.parseInt(mFecha.group(3)),
                        Integer.parseInt(mFecha.group(2)),
                        Integer.parseInt(mFecha.group(1)));
            } catch (Exception ignored) {}
        }
        return null;
    }

    private String obtenerNombreInstructor(String idInstructor) {
        for (Instructor inst : instructorDAO.listarTodos()) {
            if (inst.getNumeroIdentificacion().equals(idInstructor))
                return inst.getNombre();
        }
        return idInstructor;
    }

    public void cerrarSesion(int idSesion) {
        sesionDAO.cerrarSesion(idSesion);
    }

    public List<MensajeGymbrot> obtenerHistorial(int idSesion) {
        return mensajeDAO.listarPorSesion(idSesion);
    }
}