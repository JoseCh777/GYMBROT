package org.gymbrot.service;

import org.gymbrot.dao.*;
import org.gymbrot.model.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
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

    // ── INICIAR SESION ────────────────────────────────────────────────────
    public SesionGymbrot iniciarSesion(String idAdmin) {
        SesionGymbrot sesion = new SesionGymbrot();
        sesion.setIdCliente(idAdmin);
        sesion.setFecha(LocalDate.now());
        sesion.setHoraInicio(LocalDateTime.now());
        sesion.setContextoActivo("admin");
        int idSesion = sesionDAO.crearSesion(sesion);
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
        String textoLower  = texto.toLowerCase();
        String contextoExtra = "";

        // ── CREAR CLIENTE ─────────────────────────────────────────────────
        if ((textoLower.contains("crear cliente") || textoLower.contains("registrar cliente")
                || textoLower.contains("nuevo cliente") || textoLower.contains("agregar cliente"))) {

            contextoExtra = procesarCrearCliente(texto, historial);

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
                    info.append(String.format("Fecha nacimiento: %s\n", c.getFechaNacimiento().format(fmt)));

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
                                        m.getTipoMembresia(), m.getFechaVencimiento().format(fmt))),
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
                    String nuevoEstado = (textoLower.contains("activar")) ? "ACTIVO" : "INACTIVO";
                    if (textoLower.contains("suspender")) nuevoEstado = "SUSPENDIDO";
                    usuario.setEstado(nuevoEstado);
                    boolean ok = usuarioDAO.actualizar(usuario);
                    if (ok) {
                        contextoExtra = " [SISTEMA: Cliente " + idCliente + " actualizado a estado " +
                                nuevoEstado + " exitosamente. Confirma al administrador.]";
                    } else {
                        contextoExtra = " [SISTEMA: No se pudo actualizar el estado del cliente " +
                                idCliente + ". Informa al administrador.]";
                    }
                } else {
                    contextoExtra = " [SISTEMA: No existe ningún usuario con ID " + idCliente + ".]";
                }
            } else {
                contextoExtra = " [SISTEMA: El administrador quiere cambiar el estado de un cliente " +
                        "pero no indicó el ID. Pídele el número de identificación.]";
            }

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
                        "GYMBROT: Cita #" + idCitaCreada + " creada.\n" +
                                "Instructor: " + nombreInst + " | Fecha: " + fechaFmt + " | Hora: " + horaFmt);

                Cliente cliente = clienteDAO.buscarPorId(cita.getIdCliente());
                if (cliente != null && cliente.getCorreo() != null) {
                    emailService.enviarCorreo(cliente.getCorreo(), "Cita agendada - GYMBROT",
                            "<h2>¡Cita agendada!</h2>" +
                                    "<table style='border-collapse:collapse;'>" +
                                    "<tr><td><b>ID:</b></td><td>#" + idCitaCreada + "</td></tr>" +
                                    "<tr><td><b>Instructor:</b></td><td>" + nombreInst + "</td></tr>" +
                                    "<tr><td><b>Fecha:</b></td><td>" + fechaFmt + "</td></tr>" +
                                    "<tr><td><b>Hora:</b></td><td>" + horaFmt + "</td></tr>" +
                                    "</table><p>¡Te esperamos en GYMBROT!</p>");
                }

                contextoExtra = " [SISTEMA: Cita #" + idCitaCreada + " creada exitosamente. " +
                        "Instructor: " + nombreInst + " | Fecha: " + fechaFmt + " | Hora: " + horaFmt + ". " +
                        "SMS y correo enviados al cliente. Confirma todos los detalles al administrador.]";
            } else {
                contextoExtra = " [SISTEMA: No se pudo crear la cita. Motivo: " + mensajeError + ". " +
                        "Informa al administrador con este motivo exacto y pídele que corrija los datos.]";
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
                        String fechaFmt   = cita.getFecha().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                        String horaFmt    = cita.getHora().format(DateTimeFormatter.ofPattern("hh:mm a"));

                        notifService.enviarSmsDirecto(
                                "GYMBROT: Tu cita #" + idCita + " del " + fechaFmt +
                                        " a las " + horaFmt + " con " + nombreInst + " fue cancelada.");

                        Cliente cliente = clienteDAO.buscarPorId(cita.getIdCliente());
                        if (cliente != null && cliente.getCorreo() != null) {
                            emailService.enviarCorreo(cliente.getCorreo(), "Cita cancelada - GYMBROT",
                                    "<h2>Cita cancelada</h2>" +
                                            "<p>Tu cita <b>#" + idCita + "</b> con " + nombreInst +
                                            " el " + fechaFmt + " a las " + horaFmt + " fue cancelada.</p>" +
                                            "<p>Contáctanos para reagendar.</p>");
                        }

                        contextoExtra = " [SISTEMA: Cita #" + idCita + " cancelada exitosamente. " +
                                "SMS y correo enviados al cliente. Confirma al administrador.]";
                    } else {
                        contextoExtra = " [SISTEMA: No se pudo cancelar la cita #" + idCita + ". " +
                                "Puede que ya esté cancelada. Informa al administrador.]";
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
            List<Membresia> vencidas = membresiaDAO.listarVencidas();
            List<Membresia> activas  = membresiaDAO.listarPorEstado("ACTIVA");
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
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, m.getFechaVencimiento());
                    lista.append(String.format("- #%d | Cliente: %s | Tipo: %s | Vence: %s | En %d días\n",
                            m.getIdMembresia(), idCliente, m.getTipoMembresia(),
                            m.getFechaVencimiento().format(fmt), dias));
                    if (dias == 7 || dias == 3 || dias == 1) {
                        Cliente cliente = clienteDAO.buscarPorId(idCliente);
                        if (cliente != null) {
                            notifService.enviarSmsDirecto("GYMBROT: Tu membresía " +
                                    m.getTipoMembresia() + " vence el " +
                                    m.getFechaVencimiento().format(fmt) + ". Renuévala pronto.");
                            if (cliente.getCorreo() != null) {
                                emailService.enviarCorreo(cliente.getCorreo(),
                                        "Tu membresía vence pronto - GYMBROT",
                                        "<h2>Recordatorio</h2><p>Hola <b>" + cliente.getNombre() +
                                                "</b>, tu membresía <b>" + m.getTipoMembresia() +
                                                "</b> vence el <b>" + m.getFechaVencimiento().format(fmt) +
                                                "</b> (en " + dias + " días).</p>");
                            }
                        }
                    }
                }
            }
            if (vencidas.isEmpty() && porVencer.isEmpty())
                lista.append("No hay membresías vencidas ni por vencer en los próximos 7 días.\n");

            contextoExtra = " [SISTEMA: " + lista + "Muéstraselas al administrador de forma organizada.]";

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

        // 2. Llamar a Groq
        String respuesta = groqService.enviarMensaje(texto + contextoExtra, historial);

        // 3. Guardar respuesta
        MensajeGymbrot msgBot = new MensajeGymbrot();
        msgBot.setIdSesion(idSesion);
        msgBot.setRemitente("BOT");
        msgBot.setContenido(respuesta);
        msgBot.setTimestampMsg(LocalDateTime.now());
        mensajeDAO.insertar(msgBot);

        return respuesta;
    }

    // ── PROCESAR CREAR CLIENTE ────────────────────────────────────────────
    private String procesarCrearCliente(String texto, List<MensajeGymbrot> historial) {
        // Extraer datos del mensaje y del historial
        String nombre     = extraerDato(texto, historial, "nombre");
        String apellidos  = extraerDato(texto, historial, "apellidos");
        String id         = extraerIdCliente(texto);
        String correo     = extraerCorreo(texto, historial);
        String telefono   = extraerTelefono(texto, historial);
        String direccion  = extraerDato(texto, historial, "direccion");

        // Validar datos mínimos
        if (nombre == null || id == null || correo == null) {
            StringBuilder faltantes = new StringBuilder("Faltan datos para crear el cliente: ");
            if (nombre == null)   faltantes.append("nombre, ");
            if (id == null)       faltantes.append("número de identificación, ");
            if (correo == null)   faltantes.append("correo, ");
            return " [SISTEMA: " + faltantes.toString().replaceAll(", $", "") +
                    ". Pídele al administrador que los proporcione.]";
        }

        // Verificar duplicado
        if (usuarioDAO.buscarPorId(id) != null) {
            return " [SISTEMA: Ya existe un usuario con ID " + id +
                    ". No se puede crear duplicado. Informa al administrador.]";
        }

        // Crear usuario
        Usuario usuario = new Usuario();
        usuario.setNumeroIdentificacion(id);
        usuario.setTipoIdentificacion("CC");
        usuario.setNombre(nombre);
        usuario.setApellidos(apellidos != null ? apellidos : "");
        usuario.setTelefono(telefono != null ? telefono : "");
        usuario.setCorreo(correo);
        usuario.setContrasenaHash(id); // contraseña temporal = ID
        usuario.setEstado("ACTIVO");
        usuario.setFechaRegistro(LocalDate.now());
        usuario.setTipoUsuario("CLIENTE");

        boolean usuarioCreado = usuarioDAO.insertar(usuario);
        if (!usuarioCreado) {
            return " [SISTEMA: Error al crear el usuario en la base de datos. Informa al administrador.]";
        }

        // Crear cliente
        Cliente cliente = new Cliente();
        cliente.setNumeroIdentificacion(id);
        cliente.setDireccion(direccion != null ? direccion : "");
        cliente.setFechaNacimiento(LocalDate.of(2000, 1, 1)); // fecha por defecto

        boolean clienteCreado = clienteDAO.insertar(cliente);
        if (!clienteCreado) {
            return " [SISTEMA: Error al crear el cliente en la base de datos. Informa al administrador.]";
        }

        // SMS y correo de bienvenida
        notifService.enviarSmsDirecto(
                "¡Bienvenido a GYMBROT, " + nombre + "! Tu cuenta ha sido creada exitosamente. " +
                        "Tu contraseña temporal es tu número de identificación.");
        emailService.enviarCorreo(correo, "Bienvenido a GYMBROT",
                "<h2>¡Bienvenido a GYMBROT Valledupar!</h2>" +
                        "<p>Hola <b>" + nombre + "</b>, tu cuenta ha sido creada exitosamente.</p>" +
                        "<p>Tu contraseña temporal es tu número de identificación: <b>" + id + "</b></p>" +
                        "<p>Te recomendamos cambiarla al ingresar por primera vez.</p>" +
                        "<p>¡Te esperamos en GYMBROT!</p>");

        return " [SISTEMA: Cliente " + nombre + " (ID: " + id + ") creado exitosamente. " +
                "SMS y correo de bienvenida enviados a " + correo + ". " +
                "Contraseña temporal: número de identificación. Confirma al administrador.]";
    }

    // ── EXTRAER DATO GENÉRICO DEL TEXTO O HISTORIAL ───────────────────────
    private String extraerDato(String texto, List<MensajeGymbrot> historial, String campo) {
        String valor = extraerDatoDeTexto(texto, campo);
        if (valor != null) return valor;
        if (historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                MensajeGymbrot msg = historial.get(i);
                if (msg.getRemitente().equals("CLIENTE")) {
                    valor = extraerDatoDeTexto(msg.getContenido(), campo);
                    if (valor != null) return valor;
                }
            }
        }
        return null;
    }

    private String extraerDatoDeTexto(String texto, String campo) {
        String t = texto.toLowerCase();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                campo + "[:\\s]+([\\w\\s]+?)(?:,|\\.|$)", java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher m = p.matcher(t);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    // ── EXTRAER CORREO ────────────────────────────────────────────────────
    private String extraerCorreo(String texto, List<MensajeGymbrot> historial) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
        java.util.regex.Matcher m = p.matcher(texto);
        if (m.find()) return m.group();
        if (historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                java.util.regex.Matcher mh = p.matcher(historial.get(i).getContenido());
                if (mh.find()) return mh.group();
            }
        }
        return null;
    }

    // ── EXTRAER TELÉFONO ──────────────────────────────────────────────────
    private String extraerTelefono(String texto, List<MensajeGymbrot> historial) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b3[0-9]{9}\\b");
        java.util.regex.Matcher m = p.matcher(texto);
        if (m.find()) return m.group();
        if (historial != null) {
            for (int i = historial.size() - 1; i >= 0; i--) {
                java.util.regex.Matcher mh = p.matcher(historial.get(i).getContenido());
                if (mh.find()) return mh.group();
            }
        }
        return null;
    }

    // ── AGENDAR CITA DESDE ADMIN ──────────────────────────────────────────
    private String[] agendarCitaAdmin(String texto, List<MensajeGymbrot> historial) {
        try {
            List<Instructor> instructores = instructorDAO.listarTodos();

            String idInstructor = null;
            String disponibilidad = "";
            for (Instructor i : instructores) {
                if (texto.toLowerCase().contains(i.getNombre().toLowerCase())) {
                    idInstructor = i.getNumeroIdentificacion();
                    disponibilidad = i.getDisponibilidad();
                    System.out.println("Instructor encontrado: " + i.getNombre() + " | ID: " + idInstructor);
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
                                disponibilidad = inst.getDisponibilidad();
                                break;
                            }
                        }
                    }
                    if (idInstructor != null) break;
                }
            }
            if (idInstructor == null)
                return new String[]{"-1", "No se encontró el instructor. Indica el nombre del instructor."};

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
                return new String[]{"-1", "No se encontró el ID del cliente. Indica el número de identificación."};

            Cliente cliente = clienteDAO.buscarPorId(idCliente);
            if (cliente == null)
                return new String[]{"-1", "El cliente con ID " + idCliente + " no existe en la base de datos."};

            LocalDate fecha = extraerFecha(texto, historial);
            if (fecha == null)
                return new String[]{"-1", "No se encontró la fecha. Indica el día (ejemplo: lunes, martes)."};

            LocalTime hora = extraerHora(texto, historial);
            if (hora == null)
                return new String[]{"-1", "No se encontró la hora. Indica la hora (ejemplo: 10am, 2pm)."};

            Cita cita = new Cita();
            cita.setIdCliente(idCliente);
            cita.setIdInstructor(idInstructor);
            cita.setFecha(fecha);
            cita.setHora(hora);
            cita.setTipoCita("CONSULTA");
            cita.setNotas("Cita creada por administrador desde GYMBROT AI");

            int idCita = citaService.programarCitaAdminYRetornarId(cita);

            if (idCita > 0) return new String[]{String.valueOf(idCita), ""};
            else if (idCita == -7) return new String[]{"-1",
                    "El instructor no trabaja ese día. Su disponibilidad es: " + disponibilidad + ". Indica un día válido."};
            else if (idCita == -8) return new String[]{"-1",
                    "La hora está fuera del horario del instructor. Su disponibilidad es: " + disponibilidad + ". Indica una hora válida."};
            else if (idCita == -9) return new String[]{"-1",
                    "El instructor ya tiene una cita en ese horario. Indica otra hora."};
            else return new String[]{"-1", "No se pudo crear la cita. Verifica los datos."};

        } catch (Exception e) {
            System.err.println("ADMIN CITA EXCEPTION: " + e.getMessage());
            return new String[]{"-1", "Error interno: " + e.getMessage()};
        }
    }

    // ── EXTRAER ID CLIENTE ────────────────────────────────────────────────
    private String extraerIdCliente(String texto) {
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile("cliente\\s+(\\d{4,12})");
        java.util.regex.Matcher m1 = p1.matcher(texto.toLowerCase());
        if (m1.find()) return m1.group(1);
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile("\\b(\\d{6,12})\\b");
        java.util.regex.Matcher m2 = p2.matcher(texto);
        if (m2.find()) return m2.group(1);
        return null;
    }

    // ── EXTRAER ID DE CITA ────────────────────────────────────────────────
    private int extraerIdCita(String texto) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "#(\\d+)|cita\\s+(\\d+)|id\\s+(\\d+)");
        java.util.regex.Matcher m = p.matcher(texto.toLowerCase());
        if (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                if (m.group(i) != null) return Integer.parseInt(m.group(i));
            }
        }
        return -1;
    }

    // ── OBTENER NOMBRE DEL INSTRUCTOR ─────────────────────────────────────
    private String obtenerNombreInstructor(String idInstructor) {
        for (Instructor inst : instructorDAO.listarTodos()) {
            if (inst.getNumeroIdentificacion().equals(idInstructor)) return inst.getNombre();
        }
        return idInstructor;
    }

    // ── EXTRAER HORA ──────────────────────────────────────────────────────
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
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
                "(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)");
        java.util.regex.Matcher m = p.matcher(texto.toLowerCase());
        if (m.find()) {
            int h = Integer.parseInt(m.group(1));
            String ampm = m.group(3);
            if (ampm.equals("pm") && h != 12) h += 12;
            if (ampm.equals("am") && h == 12) h = 0;
            int min = m.group(2) != null ? Integer.parseInt(m.group(2)) : 0;
            return LocalTime.of(h, min);
        }
        java.util.regex.Pattern p24 = java.util.regex.Pattern.compile(
                "\\b([01]?\\d|2[0-3]):([0-5]\\d)\\b");
        java.util.regex.Matcher m24 = p24.matcher(texto.toLowerCase());
        if (m24.find()) return LocalTime.of(
                Integer.parseInt(m24.group(1)), Integer.parseInt(m24.group(2)));
        return null;
    }

    // ── EXTRAER FECHA ─────────────────────────────────────────────────────
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
        if (t.contains("hoy"))    return hoy;
        if (t.contains("mañana") || t.contains("manana")) return hoy.plusDays(1);
        if (t.contains("lunes"))
            return hoy.with(java.time.DayOfWeek.MONDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 1 ? 1 : 0);
        if (t.contains("martes"))
            return hoy.with(java.time.DayOfWeek.TUESDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 2 ? 1 : 0);
        if (t.contains("miércoles") || t.contains("miercoles"))
            return hoy.with(java.time.DayOfWeek.WEDNESDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 3 ? 1 : 0);
        if (t.contains("jueves"))
            return hoy.with(java.time.DayOfWeek.THURSDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 4 ? 1 : 0);
        if (t.contains("viernes"))
            return hoy.with(java.time.DayOfWeek.FRIDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 5 ? 1 : 0);
        if (t.contains("sábado") || t.contains("sabado"))
            return hoy.with(java.time.DayOfWeek.SATURDAY).plusWeeks(hoy.getDayOfWeek().getValue() >= 6 ? 1 : 0);
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