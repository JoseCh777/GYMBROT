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
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChatbotService {

    private static final Logger LOGGER = Logger.getLogger(ChatbotService.class.getName());

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

    public SesionGymbrot iniciarSesion(String idAdmin) {
        SesionGymbrot sesion = new SesionGymbrot();
        sesion.setIdCliente(idAdmin);
        sesion.setFecha(LocalDate.now());
        sesion.setHoraInicio(LocalDateTime.now());
        sesion.setContextoActivo("admin");
        int idSesion = sesionDAO.crearSesion(sesion);
        if (idSesion <= 0) {
            LOGGER.warning("No se pudo crear sesion para admin: " + idAdmin);
        }
        sesion.setIdSesion(idSesion);
        return sesion;
    }

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

        LOGGER.info("[procesarMensaje] texto=" + textoLower);

        if (textoLower.contains("crear cliente") || textoLower.contains("registrar cliente")
                || textoLower.contains("nuevo cliente") || textoLower.contains("agregar cliente")) {

            contextoExtra = procesarCrearCliente(texto, historial);

        } else if (textoLower.contains("crear rutina") || textoLower.contains("crea rutina")
                || textoLower.contains("generar rutina") || textoLower.contains("nueva rutina")) {

            contextoExtra = procesarCrearRutina(texto, historial);

        } else if ((textoLower.contains("eliminar cliente") || textoLower.contains("borrar cliente")
                || textoLower.contains("remover cliente") || textoLower.contains("eliminar usuario"))
                && (extraerIdCliente(texto) != null || extraerCorreo(texto, historial) != null)) {

            contextoExtra = procesarEliminarCliente(texto, historial);

        } else if ((textoLower.contains("consultar cliente") || textoLower.contains("buscar cliente")
                || textoLower.contains("informacion del cliente") || textoLower.contains("info del cliente")
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
                info.append(String.format("Telefono: %s\n", c.getTelefono()));
                info.append(String.format("Estado: %s\n", c.getEstado()));
                info.append(String.format("Direccion: %s\n", c.getDireccion()));
                if (c.getFechaNacimiento() != null)
                    info.append(String.format("Fecha nacimiento: %s\n", c.getFechaNacimiento().format(fmt)));

                List<Membresia> activas = membresiaDAO.listarPorEstado("ACTIVA");
                activas.stream()
                        .filter(m -> {
                            HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                            return h != null && idCliente.equals(h.getIdCliente());
                        })
                        .findFirst()
                        .ifPresentOrElse(
                                m -> info.append(String.format("Membresia activa: %s | Vence: %s\n",
                                        m.getTipoMembresia(), m.getFechaVencimiento().format(fmt))),
                                () -> info.append("Sin membresia activa.\n"));

                List<Cita> citas = citaDAO.listarPorCliente(idCliente);
                info.append(String.format("Citas registradas: %d\n", citas.size()));
                citas.stream().limit(3).forEach(cita ->
                        info.append(String.format("  * Cita #%d | %s | Instructor: %s | %s\n",
                                cita.getIdCita(), cita.getFecha().format(fmt),
                                cita.getIdInstructor(), cita.getEstado())));

                List<RegistroIngreso> ingresos = ingresoDAO.listarPorCliente(idCliente);
                long ingresosMes = ingresos.stream()
                        .filter(r -> r.getFecha().getMonth() == LocalDate.now().getMonth()
                                && r.getFecha().getYear() == LocalDate.now().getYear())
                        .count();
                info.append(String.format("Ingresos este mes: %d\n", ingresosMes));

                contextoExtra = " [SISTEMA: Informacion completa del cliente:\n" + info +
                        "Muestrasela al administrador de forma organizada y clara.]";
            } else {
                contextoExtra = " [SISTEMA: No existe ningun cliente con ID " + idCliente +
                        ". Informa al administrador.]";
            }

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
                    contextoExtra = " [SISTEMA: No existe ningun usuario con ID " + idCliente + ".]";
                }
            } else {
                contextoExtra = " [SISTEMA: El administrador quiere cambiar el estado de un cliente " +
                        "pero no indico el ID. Pidele el numero de identificacion.]";
            }

        } else if ((textoLower.contains("modificar cliente") || textoLower.contains("actualizar cliente")
                || textoLower.contains("editar cliente") || textoLower.contains("cambiar datos del cliente"))
                && !textoLower.contains("crear") && !textoLower.contains("registrar")) {

            contextoExtra = procesarModificarCliente(texto, historial);

        } else if ((textoLower.contains("modificar cita") || textoLower.contains("actualizar cita")
                || textoLower.contains("cambiar cita") || textoLower.contains("editar cita")
                || textoLower.contains("reprogramar cita") || textoLower.contains("cambiar fecha")
                || textoLower.contains("cambiar hora") || textoLower.contains("cambiar instructor"))
                && !textoLower.contains("cancelar") && !textoLower.contains("eliminar")) {

            contextoExtra = procesarModificarCita(texto, historial);

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
                            generarHtmlCita(nombreLimpio, nombreInst, fechaFmt, horaFmt, idCitaCreada));
                }

                contextoExtra = " [SISTEMA: Cita #" + idCitaCreada + " creada exitosamente. " +
                        "Instructor: " + nombreInst + " | Fecha: " + fechaFmt +
                        " | Hora: " + horaFmt + ". " +
                        "SMS y correo enviados al cliente. Confirma todos los detalles al administrador.]";
            } else {
                contextoExtra = " [SISTEMA: No se pudo crear la cita. Motivo: " + mensajeError +
                        ". Informa al administrador con este motivo exacto y pidele que corrija los datos.]";
            }

        } else if ((textoLower.contains("cancelar") || textoLower.contains("eliminar")
                || textoLower.contains("borrar")) && textoLower.contains("cita")) {

            int idCita = extraerIdCita(texto);
            if (idCita > 0) {
                Cita cita = citaDAO.buscarPorId(idCita);
                if (cita != null) {
                    boolean cancelada = citaService.cancelarCita(idCita);
                    if (cancelada) {
                        String nombreInst = obtenerNombreInstructor(cita.getIdInstructor());
                        String fechaFmt   = cita.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        String horaFmt    = cita.getHora().format(DateTimeFormatter.ofPattern("hh:mm a"));

                        notifService.enviarSmsDirecto(
                                "GYMBROT: Tu cita #" + idCita + " del " + fechaFmt +
                                        " a las " + horaFmt + " con " + nombreInst + " fue cancelada.");

                        Cliente cliente = clienteDAO.buscarPorId(cita.getIdCliente());
                        if (cliente != null && cliente.getCorreo() != null) {
                            String nombreLimpio = cliente.getNombre().split(" ")[0];
                            emailService.enviarCorreo(cliente.getCorreo(),
                                    "Cita cancelada - GYMBROT",
                                    generarHtmlCancelacion(nombreLimpio, nombreInst, fechaFmt, horaFmt, idCita));
                        }

                        contextoExtra = " [SISTEMA: Cita #" + idCita + " cancelada exitosamente. " +
                                "SMS y correo enviados al cliente. Confirma al administrador.]";
                    } else {
                        contextoExtra = " [SISTEMA: No se pudo cancelar la cita #" + idCita +
                                ". Puede que ya este cancelada. Informa al administrador.]";
                    }
                } else {
                    contextoExtra = " [SISTEMA: No existe la cita #" + idCita + ".]";
                }
            } else {
                contextoExtra = " [SISTEMA: El administrador quiere cancelar una cita pero no " +
                        "especifico el ID. Pidele el numero. Ejemplo: 'cancelar cita #12']";
            }

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
                        "Muestralas en formato claro al administrador.]";
            } else {
                contextoExtra = " [SISTEMA: No hay citas programadas para hoy.]";
            }

        } else if (textoLower.contains("membres") || textoLower.contains("venc")
                || textoLower.contains("renovar")) {

            LocalDate hoy = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            List<Membresia> vencidas  = membresiaDAO.listarVencidas();
            List<Membresia> activas   = membresiaDAO.listarPorEstado("ACTIVA");
            List<Membresia> porVencer = activas.stream()
                    .filter(m -> !m.getFechaVencimiento().isAfter(hoy.plusDays(7)))
                    .toList();

            StringBuilder lista = new StringBuilder();
            if (!vencidas.isEmpty()) {
                lista.append(String.format("VENCIDAS (%d):\n", vencidas.size()));
                for (Membresia m : vencidas) {
                    HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                    String idCliente = h != null ? h.getIdCliente() : "N/A";
                    lista.append(String.format("- #%d | Cliente: %s | Tipo: %s | Vencio: %s\n",
                            m.getIdMembresia(), idCliente, m.getTipoMembresia(),
                            m.getFechaVencimiento().format(fmt)));
                }
            }
            if (!porVencer.isEmpty()) {
                lista.append("POR VENCER (proximos 7 dias):\n");
                for (Membresia m : porVencer) {
                    HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                    String idCliente = h != null ? h.getIdCliente() : "N/A";
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, m.getFechaVencimiento());
                    lista.append(String.format(
                            "- #%d | Cliente: %s | Tipo: %s | Vence: %s | En %d dias\n",
                            m.getIdMembresia(), idCliente, m.getTipoMembresia(),
                            m.getFechaVencimiento().format(fmt), dias));
                    if (dias == 7 || dias == 3 || dias == 1) {
                        Cliente cliente = clienteDAO.buscarPorId(idCliente);
                        if (cliente != null) {
                            String nombreLimpio = cliente.getNombre().split(" ")[0];
                            notifService.enviarSmsDirecto(
                                    "GYMBROT: Hola " + nombreLimpio + ", tu membresia " +
                                            m.getTipoMembresia() + " vence el " +
                                            m.getFechaVencimiento().format(fmt) +
                                            ". Renuevala pronto.");
                            if (cliente.getCorreo() != null) {
                                emailService.enviarCorreo(cliente.getCorreo(),
                                        "Tu membresia esta por vencer - GYMBROT",
                                        generarHtmlMembresia(nombreLimpio, m, dias, fmt));
                            }
                        }
                    }
                }
            }
            if (vencidas.isEmpty() && porVencer.isEmpty()) {
                lista.append("No hay membresias vencidas ni por vencer en los proximos 7 dias.\n");
            }

            contextoExtra = " [SISTEMA: " + lista +
                    "Muestraselas al administrador de forma organizada.]";

        } else if (textoLower.contains("estadistica") || textoLower.contains("estadisticas")
                || textoLower.contains("resumen") || textoLower.contains("reporte")
                || textoLower.contains("cuantos") || textoLower.contains("total")) {

            String stats = consultaService.buscarInfoRelevante(texto);
            if (!stats.isEmpty()) {
                contextoExtra = " [SISTEMA: " + stats +
                        "Presenta estas estadisticas de forma clara al administrador.]";
            }
        }

        LOGGER.info("[procesarMensaje] contextoExtra=" + contextoExtra);

        String respuesta = groqService.enviarMensaje(texto + contextoExtra, historial);

        MensajeGymbrot msgBot = new MensajeGymbrot();
        msgBot.setIdSesion(idSesion);
        msgBot.setRemitente("BOT");
        msgBot.setContenido(respuesta);
        msgBot.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgBot);

        return respuesta;
    }

    // ── MODIFICAR CITA ────────────────────────────────────────────────────
    private String procesarModificarCita(String texto, List<MensajeGymbrot> historial) {

        int idCita = extraerIdCita(texto);
        if (idCita <= 0) {
            return " [SISTEMA: El administrador quiere modificar una cita pero no indico el ID. " +
                    "Pidele el numero. Ejemplo: 'modificar cita #501 hora: 2pm']";
        }

        Cita cita = citaDAO.buscarPorId(idCita);
        if (cita == null) {
            return " [SISTEMA: No existe la cita #" + idCita + ". Verifica el ID.]";
        }
        if (!cita.getEstado().equals("PENDIENTE")) {
            return " [SISTEMA: La cita #" + idCita + " tiene estado " + cita.getEstado() +
                    ". Solo se pueden modificar citas en estado PENDIENTE.]";
        }

        LOGGER.info("[procesarModificarCita] Modificando cita id=" + idCita);

        LocalDate nuevaFecha      = extraerFechaCita(texto);
        LocalTime nuevaHora       = extraerHoraCita(texto);
        String    nuevoInstructor = extraerInstructorId(texto);
        String    nuevasNotas     = extraerNotas(texto);

        LOGGER.info("[procesarModificarCita] fecha=" + nuevaFecha + " hora=" + nuevaHora
                + " instructor=" + nuevoInstructor + " notas=" + nuevasNotas);

        if (nuevaFecha == null && nuevaHora == null && nuevoInstructor == null && nuevasNotas == null) {
            return " [SISTEMA: El administrador quiere modificar la cita #" + idCita +
                    " pero no indico que cambiar. Pidele: fecha, hora, instructor o notas.]";
        }

        LocalDate fechaFinal      = nuevaFecha      != null ? nuevaFecha      : cita.getFecha();
        LocalTime horaFinal       = nuevaHora       != null ? nuevaHora       : cita.getHora();
        String    instructorFinal = nuevoInstructor != null ? nuevoInstructor : cita.getIdInstructor();

        if (nuevaFecha != null && !nuevaFecha.isAfter(LocalDate.now())) {
            return " [SISTEMA: La nueva fecha " + nuevaFecha +
                    " debe ser posterior a hoy. Pidele una fecha valida.]";
        }

        if (nuevaFecha != null || nuevaHora != null || nuevoInstructor != null) {
            List<Cita> citasInstructor = citaDAO.listarPorInstructor(instructorFinal);
            for (Cita c : citasInstructor) {
                if (c.getIdCita() != idCita
                        && c.getFecha().equals(fechaFinal)
                        && c.getHora().equals(horaFinal)
                        && c.getEstado().equals("PENDIENTE")) {
                    return " [SISTEMA: El instructor " + instructorFinal +
                            " ya tiene una cita PENDIENTE el " + fechaFinal +
                            " a las " + horaFinal + ". Elige otro horario.]";
                }
            }
        }

        StringBuilder cambiosLog = new StringBuilder();

        if (nuevaFecha != null && !nuevaFecha.equals(cita.getFecha())) {
            cambiosLog.append("fecha: ").append(cita.getFecha()).append(" → ").append(nuevaFecha).append(" | ");
            cita.setFecha(nuevaFecha);
        }
        if (nuevaHora != null && !nuevaHora.equals(cita.getHora())) {
            cambiosLog.append("hora: ").append(cita.getHora()).append(" → ").append(nuevaHora).append(" | ");
            cita.setHora(nuevaHora);
        }
        if (nuevoInstructor != null && !nuevoInstructor.equals(cita.getIdInstructor())) {
            cambiosLog.append("instructor: ").append(cita.getIdInstructor())
                    .append(" → ").append(nuevoInstructor).append(" | ");
            cita.setIdInstructor(nuevoInstructor);
        }
        if (nuevasNotas != null && !nuevasNotas.equals(cita.getNotas())) {
            cambiosLog.append("notas: ").append(cita.getNotas())
                    .append(" → ").append(nuevasNotas).append(" | ");
            cita.setNotas(nuevasNotas);
        }

        if (cambiosLog.isEmpty()) {
            return " [SISTEMA: La cita #" + idCita +
                    " ya tiene los mismos valores. No se realizo ningun cambio.]";
        }

        boolean actualizado = citaDAO.actualizar(cita);
        LOGGER.info("[procesarModificarCita] actualizado=" + actualizado);

        if (!actualizado) {
            return " [SISTEMA: Error al actualizar la cita #" + idCita +
                    " en la BD. Revisa los logs del servidor.]";
        }

        String nombreInst = obtenerNombreInstructor(cita.getIdInstructor());
        String fechaFmt   = cita.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String horaFmt    = cita.getHora().format(DateTimeFormatter.ofPattern("hh:mm a"));

        notifService.enviarSmsDirecto(
                "GYMBROT: Tu cita #" + idCita + " fue modificada. " +
                        "Nueva fecha: " + fechaFmt + " a las " + horaFmt +
                        " con " + nombreInst + ".");

        Cliente cliente = clienteDAO.buscarPorId(cita.getIdCliente());
        if (cliente != null && cliente.getCorreo() != null) {
            String nombreLimpio = cliente.getNombre().split(" ")[0];
            emailService.enviarCorreo(
                    cliente.getCorreo(),
                    "Cita modificada - GYMBROT",
                    generarHtmlCitaModificada(nombreLimpio, nombreInst, fechaFmt, horaFmt, idCita));
        }

        String cambiosResumen = cambiosLog.toString().replaceAll(" \\| $", "");
        return " [SISTEMA: Cita #" + idCita + " modificada exitosamente. " +
                "Cambios: " + cambiosResumen + ". " +
                "SMS y correo enviados al cliente. Confirma los cambios al administrador.]";
    }

    // ── EXTRACTORES ESPECÍFICOS PARA MODIFICAR CITA ───────────────────────
    private LocalDate extraerFechaCita(String texto) {
        Pattern pExplicito = Pattern.compile(
                "(?:fecha|nueva\\s+fecha)[:\\s]+([\\d]{4}-[\\d]{2}-[\\d]{2}|[\\d]{2}/[\\d]{2}/[\\d]{4})",
                Pattern.CASE_INSENSITIVE);
        Matcher m = pExplicito.matcher(texto);
        if (m.find()) {
            try {
                String val = m.group(1);
                if (val.contains("-")) return LocalDate.parse(val);
                String[] p = val.split("/");
                return LocalDate.of(Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
            } catch (Exception ignored) {}
        }
        return extraerFechaDeTexto(texto);
    }

    private LocalTime extraerHoraCita(String texto) {
        Pattern pExplicito = Pattern.compile(
                "(?:hora|nueva\\s+hora)[:\\s]+(\\S+)",
                Pattern.CASE_INSENSITIVE);
        Matcher m = pExplicito.matcher(texto);
        if (m.find()) {
            LocalTime t = extraerHoraDeTexto(m.group(1));
            if (t != null) return t;
        }
        return extraerHoraDeTexto(texto);
    }

    private String extraerInstructorId(String texto) {
        Pattern p = Pattern.compile(
                "(?:instructor|id\\s+instructor)[:\\s]+(INS\\d+)",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) return m.group(1).toUpperCase();
        return null;
    }

    private String extraerNotas(String texto) {
        Pattern p = Pattern.compile(
                "(?:notas?|observaciones?)[:\\s]+(.+?)(?:,|\\.|\\n|$)",
                Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto.trim());
        if (m.find()) {
            String val = m.group(1).trim();
            return val.isEmpty() ? null : val;
        }
        return null;
    }

    // ── HTML GENERATORS ───────────────────────────────────────────────────
    private String generarHtmlCita(String nombre, String instructor, String fecha, String hora, int idCita) {
        return "<!DOCTYPE html><html><head><style>" +
                "body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}" +
                ".container{max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:10px;}" +
                ".header{background:#00b4d8;color:white;padding:20px;text-align:center;border-radius:8px 8px 0 0;}" +
                ".content{padding:20px;}" +
                ".info{background:#e8f4f8;padding:15px;border-radius:8px;margin:15px 0;}" +
                ".footer{background:#f4f4f4;padding:10px;text-align:center;font-size:12px;border-radius:0 0 8px 8px;}" +
                "</style></head><body><div class='container'>" +
                "<div class='header'><h2>GYMBROT</h2></div>" +
                "<div class='content'>" +
                "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                "<p>Tu cita ha sido agendada exitosamente.</p>" +
                "<div class='info'>" +
                "<p><strong>Instructor:</strong> " + instructor + "</p>" +
                "<p><strong>Fecha:</strong> " + fecha + "</p>" +
                "<p><strong>Hora:</strong> " + hora + "</p>" +
                "<p><strong>ID de cita:</strong> #" + idCita + "</p>" +
                "</div>" +
                "<p>Recuerda llegar 10 minutos antes.</p>" +
                "<p>Te esperamos en GYMBROT.</p></div>" +
                "<div class='footer'><p>© 2026 GYMBROT Valledupar</p></div>" +
                "</div></body></html>";
    }

    private String generarHtmlCitaModificada(String nombre, String instructor, String fecha, String hora, int idCita) {
        return "<!DOCTYPE html><html><head><style>" +
                "body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}" +
                ".container{max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:10px;}" +
                ".header{background:#f4a261;color:white;padding:20px;text-align:center;border-radius:8px 8px 0 0;}" +
                ".content{padding:20px;}" +
                ".info{background:#fff3e0;padding:15px;border-radius:8px;margin:15px 0;}" +
                ".footer{background:#f4f4f4;padding:10px;text-align:center;font-size:12px;border-radius:0 0 8px 8px;}" +
                "</style></head><body><div class='container'>" +
                "<div class='header'><h2>GYMBROT</h2></div>" +
                "<div class='content'>" +
                "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                "<p>Tu cita ha sido modificada exitosamente.</p>" +
                "<div class='info'>" +
                "<p><strong>Instructor:</strong> " + instructor + "</p>" +
                "<p><strong>Nueva fecha:</strong> " + fecha + "</p>" +
                "<p><strong>Nueva hora:</strong> " + hora + "</p>" +
                "<p><strong>ID de cita:</strong> #" + idCita + "</p>" +
                "</div>" +
                "<p>Recuerda llegar 10 minutos antes.</p>" +
                "<p>Te esperamos en GYMBROT.</p></div>" +
                "<div class='footer'><p>© 2026 GYMBROT Valledupar</p></div>" +
                "</div></body></html>";
    }

    private String generarHtmlCancelacion(String nombre, String instructor, String fecha, String hora, int idCita) {
        return "<!DOCTYPE html><html><head><style>" +
                "body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}" +
                ".container{max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:10px;}" +
                ".header{background:#ff6b6b;color:white;padding:20px;text-align:center;border-radius:8px 8px 0 0;}" +
                ".content{padding:20px;}" +
                ".info{background:#ffe8e8;padding:15px;border-radius:8px;margin:15px 0;}" +
                ".footer{background:#f4f4f4;padding:10px;text-align:center;font-size:12px;border-radius:0 0 8px 8px;}" +
                "</style></head><body><div class='container'>" +
                "<div class='header'><h2>GYMBROT</h2></div>" +
                "<div class='content'>" +
                "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                "<p>Tu cita ha sido cancelada exitosamente.</p>" +
                "<div class='info'>" +
                "<p><strong>Instructor:</strong> " + instructor + "</p>" +
                "<p><strong>Fecha:</strong> " + fecha + "</p>" +
                "<p><strong>Hora:</strong> " + hora + "</p>" +
                "<p><strong>ID de cita:</strong> #" + idCita + "</p>" +
                "</div>" +
                "<p>Contactanos para reagendar una nueva cita.</p></div>" +
                "<div class='footer'><p>© 2026 GYMBROT Valledupar</p></div>" +
                "</div></body></html>";
    }

    private String generarHtmlMembresia(String nombre, Membresia m, long dias, DateTimeFormatter fmt) {
        return "<!DOCTYPE html><html><head><style>" +
                "body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}" +
                ".container{max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:10px;}" +
                ".header{background:#ffaa00;color:white;padding:20px;text-align:center;border-radius:8px 8px 0 0;}" +
                ".content{padding:20px;}" +
                ".footer{background:#f4f4f4;padding:10px;text-align:center;font-size:12px;border-radius:0 0 8px 8px;}" +
                "</style></head><body><div class='container'>" +
                "<div class='header'><h2>GYMBROT</h2></div>" +
                "<div class='content'>" +
                "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                "<p>Tu membresia <strong>" + m.getTipoMembresia() + "</strong> vence en <strong>" + dias + " dias</strong>.</p>" +
                "<p><strong>Fecha de vencimiento:</strong> " + m.getFechaVencimiento().format(fmt) + "</p>" +
                "<p>Renueva ahora y sigue entrenando sin interrupciones.</p>" +
                "<p>Contactanos para mas informacion.</p></div>" +
                "<div class='footer'><p>© 2026 GYMBROT Valledupar</p></div>" +
                "</div></body></html>";
    }

    private String generarHtmlRutina(String nombre, String objetivo, String diasSemana, int idRutina, String rutinaGenerada) {
        return "<!DOCTYPE html><html><head><style>" +
                "body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}" +
                ".container{max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:10px;}" +
                ".header{background:#00b4d8;color:white;padding:20px;text-align:center;border-radius:8px 8px 0 0;}" +
                ".content{padding:20px;}" +
                ".info{background:#e8f4f8;padding:15px;border-radius:8px;margin:15px 0;}" +
                ".rutina{background:#f4f4f4;padding:15px;border-radius:8px;font-family:monospace;white-space:pre-wrap;}" +
                ".footer{background:#f4f4f4;padding:10px;text-align:center;font-size:12px;border-radius:0 0 8px 8px;}" +
                "</style></head><body><div class='container'>" +
                "<div class='header'><h2>GYMBROT</h2></div>" +
                "<div class='content'>" +
                "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                "<p>Se ha creado una nueva rutina personalizada para ti.</p>" +
                "<div class='info'>" +
                "<p><strong>Objetivo:</strong> " + objetivo + "</p>" +
                "<p><strong>Dias de entrenamiento:</strong> " + diasSemana + "</p>" +
                "<p><strong>ID de rutina:</strong> #" + idRutina + "</p>" +
                "</div>" +
                "<div class='rutina'>" + rutinaGenerada.replace("\n", "<br>") + "</div>" +
                "<p>Recuerda mantener una buena hidratacion y alimentacion.</p></div>" +
                "<div class='footer'><p>© 2026 GYMBROT Valledupar</p></div>" +
                "</div></body></html>";
    }

    private String generarHtmlDatosActualizados(String nombre) {
        return "<!DOCTYPE html><html><head><style>" +
                "body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}" +
                ".container{max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:10px;}" +
                ".header{background:#00b4d8;color:white;padding:20px;text-align:center;border-radius:8px 8px 0 0;}" +
                ".content{padding:20px;}" +
                ".footer{background:#f4f4f4;padding:10px;text-align:center;font-size:12px;border-radius:0 0 8px 8px;}" +
                "</style></head><body><div class='container'>" +
                "<div class='header'><h2>GYMBROT</h2></div>" +
                "<div class='content'>" +
                "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                "<p>Tus datos en GYMBROT han sido actualizados exitosamente.</p>" +
                "<p>Si no reconoces este cambio, comunicate con nosotros de inmediato.</p></div>" +
                "<div class='footer'><p>© 2026 GYMBROT Valledupar</p></div>" +
                "</div></body></html>";
    }

    private String generarHtmlBienvenida(String nombre, String correo, String id) {
        return "<!DOCTYPE html><html><head><style>" +
                "body{font-family:Arial,sans-serif;line-height:1.6;color:#333;}" +
                ".container{max-width:600px;margin:0 auto;padding:20px;border:1px solid #ddd;border-radius:10px;}" +
                ".header{background:#00b4d8;color:white;padding:20px;text-align:center;border-radius:8px 8px 0 0;}" +
                ".content{padding:20px;}" +
                ".credenciales{background:#f4f4f4;padding:15px;border-radius:8px;margin:15px 0;}" +
                ".footer{background:#f4f4f4;padding:10px;text-align:center;font-size:12px;border-radius:0 0 8px 8px;}" +
                "</style></head><body><div class='container'>" +
                "<div class='header'><h2>GYMBROT</h2></div>" +
                "<div class='content'>" +
                "<p>Hola <strong>" + nombre + "</strong>,</p>" +
                "<p>Tu cuenta ha sido creada exitosamente en el sistema GYMBROT.</p>" +
                "<div class='credenciales'>" +
                "<h3>Tus credenciales de acceso:</h3>" +
                "<p><strong>Usuario:</strong> " + correo + "</p>" +
                "<p><strong>Contrasena temporal:</strong> " + id + "</p>" +
                "</div>" +
                "<p>Recomendacion: Cambia tu contrasena al ingresar por primera vez.</p>" +
                "<p>Atentamente, <strong>GYMBROT Valledupar</strong></p></div>" +
                "<div class='footer'><p>© 2026 GYMBROT Valledupar</p></div>" +
                "</div></body></html>";
    }

    // ── PROCESADORES ──────────────────────────────────────────────────────
    private String procesarEliminarCliente(String texto, List<MensajeGymbrot> historial) {

        String idCliente = extraerIdCliente(texto);

        if (idCliente == null) {
            String correoCliente = extraerCorreo(texto, historial);
            if (correoCliente == null) {
                return " [SISTEMA: No se encontro el ID o correo del cliente a eliminar.]";
            }
            Usuario usuario = usuarioDAO.buscarPorCorreo(correoCliente);
            if (usuario == null) {
                return " [SISTEMA: No existe un cliente con correo " + correoCliente + "]";
            }
            idCliente = usuario.getNumeroIdentificacion();
        }

        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) {
            return " [SISTEMA: No existe un cliente con ID " + idCliente + "]";
        }

        String nombreCliente         = cliente.getNombre();
        String correoClienteEliminar = cliente.getCorreo();
        String telefonoCliente       = cliente.getTelefono();

        // Notificar ANTES de eliminar
        if (correoClienteEliminar != null) {
            emailService.enviarCorreo(correoClienteEliminar,
                    "Cuenta eliminada - GYMBROT",
                    "<h2>Cuenta eliminada</h2>" +
                            "<p>Hola <b>" + nombreCliente + "</b>,</p>" +
                            "<p>Tu cuenta en GYMBROT ha sido eliminada.</p>" +
                            "<p>Si esto es un error, contacta al administrador.</p>");
            System.out.println("Correo enviado a: " + correoClienteEliminar);
        }
        if (telefonoCliente != null) {
            notifService.enviarSmsACliente(telefonoCliente,
                    "GYMBROT: Tu cuenta ha sido eliminada. Si no reconoces este cambio, contactanos.");
        }

        try {
            LOGGER.info("Eliminando cliente ID: " + idCliente);
            Connection conn = org.gymbrot.util.DatabaseConnection.getInstance();

            // 1. RUTINA_EJERCICIOS (via subquery de RUTINAS)
            ejecutarSQL(conn, "DELETE FROM RUTINA_EJERCICIOS WHERE id_rutina IN " +
                    "(SELECT id_rutina FROM RUTINAS WHERE id_cliente = ?)", idCliente);

            // 2. RUTINAS
            ejecutarSQL(conn, "DELETE FROM RUTINAS WHERE id_cliente = ?", idCliente);

            // 3. NOTIFICACIONES
            ejecutarSQL(conn, "DELETE FROM NOTIFICACIONES WHERE id_cliente = ?", idCliente);

            // 4. CITAS
            ejecutarSQL(conn, "DELETE FROM CITAS WHERE id_cliente = ?", idCliente);

            // 5. PAGOS
            ejecutarSQL(conn, "DELETE FROM PAGOS WHERE id_cliente = ?", idCliente);

            // 6. PROGRESOS
            ejecutarSQL(conn, "DELETE FROM PROGRESOS WHERE id_cliente = ?", idCliente);

            // 7. REGISTROS_INGRESOS
            ejecutarSQL(conn, "DELETE FROM REGISTROS_INGRESOS WHERE id_cliente = ?", idCliente);

            // 8. HISTORIAL_MEMBRESIAS
            ejecutarSQL(conn, "DELETE FROM HISTORIAL_MEMBRESIAS WHERE id_cliente = ?", idCliente);

            // 9. MENSAJES_GYMBROT (via subquery de SESIONES_GYMBROT)
            ejecutarSQL(conn, "DELETE FROM MENSAJES_GYMBROT WHERE id_sesion IN " +
                    "(SELECT id_sesion FROM SESIONES_GYMBROT WHERE id_cliente = ?)", idCliente);

            // 10. SESIONES_GYMBROT
            ejecutarSQL(conn, "DELETE FROM SESIONES_GYMBROT WHERE id_cliente = ?", idCliente);

            // 11. CLIENTES
            boolean eliminadoCliente = clienteDAO.eliminar(idCliente);
            LOGGER.info("CLIENTES eliminado: " + eliminadoCliente);

            // 12. USUARIOS
            boolean eliminadoUsuario = usuarioDAO.eliminar(idCliente);
            LOGGER.info("USUARIOS eliminado: " + eliminadoUsuario);

            if (eliminadoCliente && eliminadoUsuario) {
                return " [SISTEMA: Cliente " + nombreCliente + " (ID: " + idCliente +
                        ") eliminado exitosamente. SMS y correo enviados.]";
            } else {
                return " [SISTEMA: Error al eliminar el cliente " + idCliente + ".]";
            }

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error al eliminar cliente: " + e.getMessage(), e);
            return " [SISTEMA: Error al eliminar: " + e.getMessage() + "]";
        }
    }

    private void ejecutarSQL(Connection conn, String sql, String parametro) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, parametro);
            int filas = ps.executeUpdate();
            LOGGER.info("[ejecutarSQL] " + sql.substring(0, 30) + "... filas=" + filas);
        } catch (SQLException e) {
            LOGGER.warning("[ejecutarSQL] Error: " + e.getMessage() + " SQL: " + sql);
        }
    }

    private String procesarCrearRutina(String texto, List<MensajeGymbrot> historial) {
        LOGGER.info("Iniciando procesarCrearRutina");

        String idCliente = extraerIdCliente(texto);
        if (idCliente == null) {
            return " [SISTEMA: No se encontro el ID del cliente. Pidele al administrador que indique el ID.]";
        }

        Cliente cliente = clienteDAO.buscarPorId(idCliente);
        if (cliente == null) {
            return " [SISTEMA: No existe un cliente con ID " + idCliente + ". Informa al administrador.]";
        }

        String objetivo  = extraerObjetivoRutina(texto);
        if (objetivo == null) objetivo = "mejorar condicion fisica";

        String diasSemana = extraerDiasSemanaRutina(texto);
        if (diasSemana == null) diasSemana = "Lunes, Miercoles, Viernes";

        String prompt = "Crea una rutina de ejercicios semanal para un cliente de gimnasio con objetivo: " + objetivo +
                ". Los dias de entrenamiento son: " + diasSemana +
                ". Incluye series y repeticiones para cada ejercicio. Da solo la rutina, sin explicaciones adicionales.";

        String rutinaGenerada = groqService.enviarMensaje(prompt, historial);

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

        if (idRutina > 0) {
            String nombreLimpio = cliente.getNombre().split(" ")[0];

            notifService.enviarSmsDirecto(
                    "GYMBROT: Hola " + nombreLimpio + "! Se ha creado una nueva rutina para ti. " +
                            "Objetivo: " + objetivo + ". Dias: " + diasSemana +
                            ". Consulta tu correo para mas detalles.");

            if (cliente.getCorreo() != null) {
                emailService.enviarCorreo(cliente.getCorreo(),
                        "Nueva rutina asignada - GYMBROT",
                        generarHtmlRutina(nombreLimpio, objetivo, diasSemana, idRutina, rutinaGenerada));
            }

            return " [SISTEMA: Rutina creada exitosamente para el cliente " + nombreLimpio +
                    " (ID: " + idCliente + "). ID de rutina: #" + idRutina +
                    ". Objetivo: " + objetivo + ". Dias: " + diasSemana +
                    ". SMS y correo enviados al cliente.]";
        } else {
            return " [SISTEMA: No se pudo guardar la rutina en la base de datos. Informa al administrador.]";
        }
    }

    private int insertarRutinaYRetornarId(Rutina rutina) {
        String sql = "INSERT INTO RUTINAS (id_instructor, id_cliente, nombre, descripcion, " +
                "fecha_creacion, fecha_fin, dias_semana, objetivo) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

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
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar rutina: " + e.getMessage(), e);
        }
        return -1;
    }

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
            return " [SISTEMA: El administrador quiere modificar un cliente pero no indico el ID. " +
                    "Pidele el numero de identificacion del cliente a modificar.]";
        }

        Cliente clienteActual = clienteDAO.buscarPorId(id);
        if (clienteActual == null) {
            return " [SISTEMA: No existe ningun cliente con ID " + id +
                    ". Verifica el numero de identificacion e intentalo de nuevo.]";
        }

        String nuevoNombre     = extraerNombre(texto, historial);
        String nuevosApellidos = extraerApellidos(texto, historial);
        String nuevoCorreo     = extraerCorreo(texto, historial);
        String nuevoTelefono   = extraerTelefono(texto, historial);
        String nuevaDireccion  = extraerDireccion(texto, historial);
        LocalDate nuevaFechaNac = extraerFecha(texto, historial);

        if (nuevoNombre == null && nuevosApellidos == null && nuevoCorreo == null
                && nuevoTelefono == null && nuevaDireccion == null && nuevaFechaNac == null) {
            return " [SISTEMA: El administrador quiere modificar el cliente " + id +
                    " pero no indico que campos cambiar. Pidele que especifique: " +
                    "nombre, apellidos, correo, telefono, direccion o fecha de nacimiento.]";
        }

        if (nuevoCorreo != null && !nuevoCorreo.equalsIgnoreCase(clienteActual.getCorreo())) {
            Usuario existente = usuarioDAO.buscarPorCorreo(nuevoCorreo);
            if (existente != null && !existente.getNumeroIdentificacion().equals(id)) {
                return " [SISTEMA: El correo " + nuevoCorreo +
                        " ya esta registrado por otro usuario. No se puede asignar.]";
            }
        }

        StringBuilder cambiosLog = new StringBuilder();

        if (nuevoNombre != null && !nuevoNombre.equalsIgnoreCase(clienteActual.getNombre())) {
            cambiosLog.append("nombre: ").append(clienteActual.getNombre()).append(" → ").append(nuevoNombre).append(" | ");
            clienteActual.setNombre(nuevoNombre);
        }
        if (nuevosApellidos != null && !nuevosApellidos.equalsIgnoreCase(clienteActual.getApellidos())) {
            cambiosLog.append("apellidos: ").append(clienteActual.getApellidos()).append(" → ").append(nuevosApellidos).append(" | ");
            clienteActual.setApellidos(nuevosApellidos);
        }
        if (nuevoCorreo != null && !nuevoCorreo.equalsIgnoreCase(clienteActual.getCorreo())) {
            cambiosLog.append("correo: ").append(clienteActual.getCorreo()).append(" → ").append(nuevoCorreo).append(" | ");
            clienteActual.setCorreo(nuevoCorreo);
        }
        if (nuevoTelefono != null && !nuevoTelefono.equals(clienteActual.getTelefono())) {
            cambiosLog.append("telefono: ").append(clienteActual.getTelefono()).append(" → ").append(nuevoTelefono).append(" | ");
            clienteActual.setTelefono(nuevoTelefono);
        }
        if (nuevaDireccion != null && !nuevaDireccion.equalsIgnoreCase(clienteActual.getDireccion())) {
            cambiosLog.append("direccion: ").append(clienteActual.getDireccion()).append(" → ").append(nuevaDireccion).append(" | ");
            clienteActual.setDireccion(nuevaDireccion);
        }
        if (nuevaFechaNac != null && !nuevaFechaNac.equals(clienteActual.getFechaNacimiento())) {
            cambiosLog.append("fechaNacimiento: ").append(clienteActual.getFechaNacimiento()).append(" → ").append(nuevaFechaNac).append(" | ");
            clienteActual.setFechaNacimiento(nuevaFechaNac);
        }

        if (cambiosLog.isEmpty()) {
            return " [SISTEMA: El cliente " + id + " ya tiene los mismos datos. No se realizo ningun cambio.]";
        }

        boolean usuarioActualizado = usuarioDAO.actualizar(clienteActual);
        if (!usuarioActualizado) {
            return " [SISTEMA: Error al actualizar USUARIOS para el cliente " + id + ".]";
        }

        boolean clienteActualizado = clienteDAO.actualizar(clienteActual);
        if (!clienteActualizado) {
            return " [SISTEMA: USUARIOS actualizado pero error al actualizar CLIENTES para " + id + ".]";
        }

        String nombreLimpio = clienteActual.getNombre().split(" ")[0];
        notifService.enviarSmsDirecto("GYMBROT: Hola " + nombreLimpio +
                ", tus datos han sido actualizados. Si no realizaste este cambio, contactanos.");

        if (clienteActual.getCorreo() != null) {
            emailService.enviarCorreo(clienteActual.getCorreo(),
                    "Datos actualizados - GYMBROT",
                    generarHtmlDatosActualizados(nombreLimpio));
        }

        String cambiosResumen = cambiosLog.toString().replaceAll(" \\| $", "");
        return " [SISTEMA: Cliente " + id + " (" + clienteActual.getNombre() + " " +
                clienteActual.getApellidos() + ") modificado exitosamente. " +
                "Cambios: " + cambiosResumen + ". SMS y correo enviados. Confirma al administrador.]";
    }

    private String procesarCrearCliente(String texto, List<MensajeGymbrot> historial) {

        String nombre    = extraerNombre(texto, historial);
        String apellidos = extraerApellidos(texto, historial);
        String id        = extraerIdCliente(texto);
        String correo    = extraerCorreo(texto, historial);
        String telefono  = extraerTelefono(texto, historial);
        String direccion = extraerDireccion(texto, historial);

        if (nombre == null || id == null || correo == null) {
            StringBuilder faltantes = new StringBuilder("Faltan datos: ");
            if (nombre == null)  faltantes.append("nombre, ");
            if (id == null)      faltantes.append("numero de identificacion, ");
            if (correo == null)  faltantes.append("correo electronico, ");
            return " [SISTEMA: " + faltantes.toString().replaceAll(", $", "") +
                    ". Pidele al administrador que los proporcione.]";
        }

        if (usuarioDAO.buscarPorId(id) != null) {
            return " [SISTEMA: Ya existe un usuario con ID " + id + ". No se puede crear duplicado.]";
        }
        if (usuarioDAO.buscarPorCorreo(correo) != null) {
            return " [SISTEMA: Ya existe un usuario con el correo " + correo + ". No se puede crear duplicado.]";
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

        boolean usuarioCreado = usuarioDAO.insertar(usuario);
        if (!usuarioCreado) {
            return " [SISTEMA: Error al insertar en USUARIOS. Revisa los logs.]";
        }

        Cliente cliente = new Cliente();
        cliente.setNumeroIdentificacion(id);
        cliente.setDireccion(direccion != null ? direccion : "");
        cliente.setFechaNacimiento(null);
        cliente.setHuellaDactilar(null);

        boolean clienteCreado = insertarClienteSeguro(cliente);
        if (!clienteCreado) {
            usuarioDAO.eliminar(id);
            return " [SISTEMA: Error al insertar en CLIENTES. USUARIOS revertido automaticamente.]";
        }

        String nombreLimpio = nombre.split(" ")[0];
        notifService.enviarSmsDirecto(
                "GYMBROT: Hola " + nombreLimpio + "! Tu cuenta ha sido creada. " +
                        "Usuario: " + correo + " - Contrasena temporal: " + id);
        emailService.enviarCorreo(correo, "Bienvenido a GYMBROT",
                generarHtmlBienvenida(nombreLimpio, correo, id));

        return " [SISTEMA: Cliente " + nombreLimpio + " " + (apellidos != null ? apellidos : "") +
                " (ID: " + id + ") creado exitosamente. SMS y correo enviados a " + correo +
                ". Contrasena temporal: numero de identificacion. Confirma al administrador.]";
    }

    private boolean insertarClienteSeguro(Cliente cliente) {
        String sql = "INSERT INTO CLIENTES (numero_identificacion, direccion, " +
                "fecha_nacimiento, huella_dactilar) VALUES (?, ?, ?, ?)";
        try (Connection conn = org.gymbrot.util.DatabaseConnection.getInstance();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cliente.getNumeroIdentificacion());
            pstmt.setString(2, cliente.getDireccion());
            if (cliente.getFechaNacimiento() != null) {
                pstmt.setDate(3, Date.valueOf(cliente.getFechaNacimiento()));
            } else {
                pstmt.setNull(3, java.sql.Types.DATE);
            }
            if (cliente.getHuellaDactilar() != null) {
                pstmt.setBytes(4, cliente.getHuellaDactilar());
            } else {
                pstmt.setNull(4, java.sql.Types.BLOB);
            }
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error al insertar cliente: " + e.getMessage(), e);
            return false;
        }
    }

    private String[] agendarCitaAdmin(String texto, List<MensajeGymbrot> historial) {
        try {
            List<Instructor> instructores = instructorDAO.listarTodos();
            String idInstructor   = null;
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
                            if (msg.getContenido().toLowerCase().contains(inst.getNombre().toLowerCase())) {
                                idInstructor  = inst.getNumeroIdentificacion();
                                disponibilidad = inst.getDisponibilidad();
                                break outer;
                            }
                        }
                    }
                }
            }
            if (idInstructor == null)
                return new String[]{"-1", "No se encontro el instructor. Indica el nombre del instructor."};

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
                return new String[]{"-1", "No se encontro el ID del cliente. Indica el numero de identificacion."};

            if (clienteDAO.buscarPorId(idCliente) == null)
                return new String[]{"-1", "El cliente con ID " + idCliente + " no existe en la base de datos."};

            LocalDate fecha = extraerFecha(texto, historial);
            if (fecha == null)
                return new String[]{"-1", "No se encontro la fecha. Indica el dia (ejemplo: lunes, martes)."};

            LocalTime hora = extraerHora(texto, historial);
            if (hora == null)
                return new String[]{"-1", "No se encontro la hora. Indica la hora (ejemplo: 10am, 2pm)."};

            Cita cita = new Cita();
            cita.setIdCliente(idCliente);
            cita.setIdInstructor(idInstructor);
            cita.setFecha(fecha);
            cita.setHora(hora);
            cita.setTipoCita("CONSULTA");
            cita.setNotas("Cita creada por administrador desde GYMBROT AI");

            int idCita = citaService.programarCitaAdminYRetornarId(cita);

            if (idCita > 0)   return new String[]{String.valueOf(idCita), ""};
            if (idCita == -7) return new String[]{"-1", "El instructor no trabaja ese dia. Disponibilidad: " + disponibilidad};
            if (idCita == -8) return new String[]{"-1", "Hora fuera del horario del instructor. Disponibilidad: " + disponibilidad};
            if (idCita == -9) return new String[]{"-1", "El instructor ya tiene una cita en ese horario. Indica otra hora."};
            return new String[]{"-1", "No se pudo crear la cita. Verifica los datos."};

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error en agendarCitaAdmin: " + e.getMessage(), e);
            return new String[]{"-1", "Error interno: " + e.getMessage()};
        }
    }

    // ── EXTRACTORES GENERALES ─────────────────────────────────────────────
    private String extraerNombre(String texto, List<MensajeGymbrot> historial) {
        Pattern p = Pattern.compile("nombre[:\\s]+(\\p{L}+(?:\\s+\\p{L}+)?)",
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
        Pattern p = Pattern.compile("apellidos?[:\\s]+(\\p{L}+(?:\\s+\\p{L}+)?)",
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
                        .append(palabra.substring(1).toLowerCase()).append(" ");
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
        Pattern p = Pattern.compile("(?:direcci[oó]n|dir)[:\\s]+([\\p{L}\\d\\s#\\-.]+?)(?:,|\\.|$)",
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
        Pattern pEtiqueta = Pattern.compile("(?:tel[eé]fono|tel|celular|cel)[:\\s]+(3\\d{9})",
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
        Pattern p1 = Pattern.compile("(?:id|cedula|cédula|identificacion|identificación)[:\\s]+(\\d{4,12})",
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
            return LocalTime.of(Integer.parseInt(m24.group(1)), Integer.parseInt(m24.group(2)));
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

        if (t.contains("lunes"))    { LocalDate c = hoy.with(java.time.DayOfWeek.MONDAY);    return c.isBefore(hoy) ? c.plusWeeks(1) : c; }
        if (t.contains("martes"))   { LocalDate c = hoy.with(java.time.DayOfWeek.TUESDAY);   return c.isBefore(hoy) ? c.plusWeeks(1) : c; }
        if (t.contains("miércoles") || t.contains("miercoles")) { LocalDate c = hoy.with(java.time.DayOfWeek.WEDNESDAY); return c.isBefore(hoy) ? c.plusWeeks(1) : c; }
        if (t.contains("jueves"))   { LocalDate c = hoy.with(java.time.DayOfWeek.THURSDAY);  return c.isBefore(hoy) ? c.plusWeeks(1) : c; }
        if (t.contains("viernes"))  { LocalDate c = hoy.with(java.time.DayOfWeek.FRIDAY);    return c.isBefore(hoy) ? c.plusWeeks(1) : c; }
        if (t.contains("sábado") || t.contains("sabado"))   { LocalDate c = hoy.with(java.time.DayOfWeek.SATURDAY); return c.isBefore(hoy) ? c.plusWeeks(1) : c; }
        if (t.contains("domingo"))  { LocalDate c = hoy.with(java.time.DayOfWeek.SUNDAY);    return c.isBefore(hoy) ? c.plusWeeks(1) : c; }

        // Reconocer "3 de junio", "15 de mayo", "el 3 de junio", etc.
        String[] meses = {"enero","febrero","marzo","abril","mayo","junio",
                "julio","agosto","septiembre","octubre","noviembre","diciembre"};
        for (int i = 0; i < meses.length; i++) {
            if (t.contains(meses[i])) {
                Pattern pMes = Pattern.compile("(\\d{1,2})\\s+de\\s+" + meses[i]);
                Matcher mMes = pMes.matcher(t);
                if (mMes.find()) {
                    try {
                        int dia  = Integer.parseInt(mMes.group(1));
                        int mes  = i + 1;
                        int anio = LocalDate.now().getYear();
                        LocalDate fecha = LocalDate.of(anio, mes, dia);
                        if (fecha.isBefore(hoy)) fecha = fecha.plusYears(1);
                        return fecha;
                    } catch (Exception ignored) {}
                }
            }
        }

        // Reconocer DD/MM/YYYY o DD-MM-YYYY
        Pattern pFecha = Pattern.compile("(\\d{1,2})[/-](\\d{1,2})[/-](\\d{4})");
        Matcher mFecha = pFecha.matcher(t);
        if (mFecha.find()) {
            try {
                return LocalDate.of(Integer.parseInt(mFecha.group(3)),
                        Integer.parseInt(mFecha.group(2)),
                        Integer.parseInt(mFecha.group(1)));
            } catch (Exception ignored) {}
        }
        return null;
    }
    private String extraerObjetivoRutina(String texto) {
        Pattern p = Pattern.compile("objetivo[:\\s]+([\\w\\s]+?)(?:,|\\.|$)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto);
        if (m.find()) return m.group(1).trim();
        String t = texto.toLowerCase();
        if (t.contains("masa muscular")) return "ganar masa muscular";
        if (t.contains("perder peso"))   return "perder peso";
        if (t.contains("definicion"))    return "definicion muscular";
        if (t.contains("resistencia"))   return "mejorar resistencia";
        return null;
    }

    private String extraerDiasSemanaRutina(String texto) {
        // Busca "dias:" seguido de todo hasta punto o fin de línea
        Pattern p = Pattern.compile("dias[:\\s]+([\\w\\s,]+?)(?:\\.|$)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(texto.trim());
        if (m.find()) return m.group(1).trim();
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