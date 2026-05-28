package org.gymbrot.service;

import org.gymbrot.dao.*;
import org.gymbrot.model.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ConsultaGymbrotService {

    private final PlanMembresiaDAO planDAO = new PlanMembresiaDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final EjercicioDAO ejercicioDAO = new EjercicioDAO();
    private final CitaDAO citaDAO = new CitaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final MembresiaDAO membresiaDAO = new MembresiaDAO();
    private final HistorialMembresiaDAO historialDAO = new HistorialMembresiaDAO();
    private final RegistroIngresoDAO ingresoDAO = new RegistroIngresoDAO();
    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    // ── BUSCAR INFO RELEVANTE SEGUN LA PREGUNTA ───────────────────────────
    public String buscarInfoRelevante(String pregunta) {
        String preguntaLower = pregunta.toLowerCase();
        StringBuilder contexto = new StringBuilder();

        // ── ESTADÍSTICAS GENERALES ────────────────────────────────────────
        if (preguntaLower.contains("estadística") || preguntaLower.contains("estadistica")
                || preguntaLower.contains("clientes activos") || preguntaLower.contains("total clientes")
                || preguntaLower.contains("cuantos clientes") || preguntaLower.contains("resumen")
                || preguntaLower.contains("dashboard") || preguntaLower.contains("reporte")) {

            List<Cliente> clientes = clienteDAO.listarTodos();
            List<Instructor> instructores = instructorDAO.listarTodos();
            long clientesActivos   = clientes.stream().filter(c -> "ACTIVO".equalsIgnoreCase(c.getEstado())).count();
            long clientesInactivos = clientes.stream().filter(c -> "INACTIVO".equalsIgnoreCase(c.getEstado())).count();
            long suspendidos       = clientes.stream().filter(c -> "SUSPENDIDO".equalsIgnoreCase(c.getEstado())).count();
            int ingresosMes        = ingresoDAO.contarIngresosMes();
            List<Cita> citasHoy    = citaDAO.listarPorFecha(LocalDate.now());
            List<Membresia> vencidas = membresiaDAO.listarVencidas();
            List<Membresia> activas  = membresiaDAO.listarPorEstado("ACTIVA");
            long porVencer7 = activas.stream()
                    .filter(m -> !m.getFechaVencimiento().isAfter(LocalDate.now().plusDays(7)))
                    .count();

            contexto.append("ESTADÍSTICAS DEL GIMNASIO:\n");
            contexto.append(String.format("- Total clientes registrados: %d\n", clientes.size()));
            contexto.append(String.format("- Clientes activos: %d\n", clientesActivos));
            contexto.append(String.format("- Clientes inactivos: %d\n", clientesInactivos));
            contexto.append(String.format("- Clientes suspendidos: %d\n", suspendidos));
            contexto.append(String.format("- Total instructores: %d\n", instructores.size()));
            contexto.append(String.format("- Ingresos este mes: %d\n", ingresosMes));
            contexto.append(String.format("- Citas programadas hoy: %d\n", citasHoy.size()));
            contexto.append(String.format("- Membresías vencidas: %d\n", vencidas.size()));
            contexto.append(String.format("- Membresías por vencer (7 días): %d\n", porVencer7));
        }

        // ── BUSCAR CLIENTE ESPECÍFICO POR ID ──────────────────────────────
        if (preguntaLower.contains("cliente") && extraerIdDeTexto(pregunta) != null) {
            String idCliente = extraerIdDeTexto(pregunta);
            Cliente c = clienteDAO.buscarPorId(idCliente);
            if (c != null) {
                DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                contexto.append("INFORMACIÓN DEL CLIENTE:\n");
                contexto.append(String.format("- Nombre: %s %s\n", c.getNombre(), c.getApellidos()));
                contexto.append(String.format("- ID: %s\n", c.getNumeroIdentificacion()));
                contexto.append(String.format("- Correo: %s\n", c.getCorreo()));
                contexto.append(String.format("- Teléfono: %s\n", c.getTelefono()));
                contexto.append(String.format("- Estado: %s\n", c.getEstado()));
                contexto.append(String.format("- Dirección: %s\n", c.getDireccion()));
                if (c.getFechaNacimiento() != null)
                    contexto.append(String.format("- Fecha nacimiento: %s\n", c.getFechaNacimiento().format(fmt)));
                if (c.getFechaRegistro() != null)
                    contexto.append(String.format("- Registrado: %s\n", c.getFechaRegistro().format(fmt)));

                // Membresía activa
                List<Membresia> activas = membresiaDAO.listarPorEstado("ACTIVA");
                activas.stream()
                        .filter(m -> {
                            HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                            return h != null && idCliente.equals(h.getIdCliente());
                        })
                        .findFirst()
                        .ifPresentOrElse(m -> {
                            contexto.append(String.format("- Membresía activa: %s | Vence: %s\n",
                                    m.getTipoMembresia(), m.getFechaVencimiento().format(fmt)));
                        }, () -> contexto.append("- Sin membresía activa.\n"));

                // Últimas citas
                List<Cita> citas = citaDAO.listarPorCliente(idCliente);
                if (!citas.isEmpty()) {
                    contexto.append(String.format("- Citas registradas: %d\n", citas.size()));
                    citas.stream().limit(3).forEach(cita ->
                            contexto.append(String.format("  * Cita #%d | %s | %s | %s\n",
                                    cita.getIdCita(), cita.getFecha().format(fmt),
                                    cita.getIdInstructor(), cita.getEstado())));
                } else {
                    contexto.append("- Sin citas registradas.\n");
                }

                // Ingresos del mes
                List<RegistroIngreso> ingresos = ingresoDAO.listarPorCliente(idCliente);
                long ingresosMes = ingresos.stream()
                        .filter(r -> r.getFecha().getMonth() == LocalDate.now().getMonth()
                                && r.getFecha().getYear() == LocalDate.now().getYear())
                        .count();
                contexto.append(String.format("- Ingresos este mes: %d\n", ingresosMes));
            } else {
                contexto.append("No se encontró ningún cliente con ID: " + idCliente + "\n");
            }
        }

        // ── MEMBRESÍAS VENCIDAS O POR VENCER ──────────────────────────────
        if (preguntaLower.contains("venc") || preguntaLower.contains("membresía")
                || preguntaLower.contains("membresia") || preguntaLower.contains("semana")
                || preguntaLower.contains("próximas") || preguntaLower.contains("proximas")
                || preguntaLower.contains("renovar") || preguntaLower.contains("renovación")) {

            LocalDate hoy     = LocalDate.now();
            LocalDate en7dias = hoy.plusDays(7);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            List<Membresia> vencidas = membresiaDAO.listarVencidas();
            List<Membresia> activas  = membresiaDAO.listarPorEstado("ACTIVA");
            List<Membresia> porVencer = activas.stream()
                    .filter(m -> !m.getFechaVencimiento().isAfter(en7dias))
                    .collect(Collectors.toList());

            if (!vencidas.isEmpty()) {
                contexto.append(String.format("MEMBRESÍAS VENCIDAS: %d\n", vencidas.size()));
                for (Membresia m : vencidas) {
                    HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                    String idCliente = h != null ? h.getIdCliente() : "N/A";
                    contexto.append(String.format("- #%d | Cliente: %s | Tipo: %s | Venció: %s\n",
                            m.getIdMembresia(), idCliente, m.getTipoMembresia(),
                            m.getFechaVencimiento().format(fmt)));
                }
            }

            if (!porVencer.isEmpty()) {
                contexto.append("MEMBRESÍAS POR VENCER (próximos 7 días):\n");
                for (Membresia m : porVencer) {
                    HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                    String idCliente = h != null ? h.getIdCliente() : "N/A";
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, m.getFechaVencimiento());
                    contexto.append(String.format("- #%d | Cliente: %s | Tipo: %s | Vence: %s | En %d días\n",
                            m.getIdMembresia(), idCliente, m.getTipoMembresia(),
                            m.getFechaVencimiento().format(fmt), dias));
                }
            }

            if (vencidas.isEmpty() && porVencer.isEmpty()) {
                contexto.append("No hay membresías vencidas ni próximas a vencer.\n");
            }
        }

        // ── CITAS DEL DÍA ─────────────────────────────────────────────────
        if (preguntaLower.contains("cita") || preguntaLower.contains("agenda")
                || preguntaLower.contains("hoy") || preguntaLower.contains("programadas")) {
            List<Cita> citasHoy = citaDAO.listarPorFecha(LocalDate.now());
            DateTimeFormatter fmtH = DateTimeFormatter.ofPattern("HH:mm");
            if (!citasHoy.isEmpty()) {
                contexto.append(String.format("CITAS HOY (%d):\n", citasHoy.size()));
                for (Cita c : citasHoy) {
                    contexto.append(String.format(
                            "- Cita #%d | Cliente: %s | Instructor: %s | Hora: %s | Estado: %s\n",
                            c.getIdCita(), c.getIdCliente(), c.getIdInstructor(),
                            c.getHora().format(fmtH), c.getEstado()));
                }
            } else {
                contexto.append("No hay citas programadas para hoy.\n");
            }
        }

        // ── LISTADO DE CLIENTES ───────────────────────────────────────────
        if (preguntaLower.contains("cliente") && extraerIdDeTexto(pregunta) == null
                && !preguntaLower.contains("estadistica") && !preguntaLower.contains("estadística")) {
            List<Cliente> clientes = clienteDAO.listarTodos();
            if (!clientes.isEmpty()) {
                contexto.append("=== LISTA COMPLETA DE CLIENTES ===\n");
                contexto.append(String.format("Total: %d clientes\n\n", clientes.size()));
                int contador = 1;
                for (Cliente c : clientes) {
                    contexto.append(contador++).append(". ")
                            .append(c.getNombre()).append(" ").append(c.getApellidos())
                            .append(" | ID: ").append(c.getNumeroIdentificacion())
                            .append(" | Correo: ").append(c.getCorreo() != null ? c.getCorreo() : "Sin correo")
                            .append(" | Estado: ").append(c.getEstado())
                            .append("\n");
                }
                contexto.append("\n=== FIN DE LA LISTA ===");
            }
        }
        // ── PLANES Y MEMBRESÍAS ───────────────────────────────────────────
        if (preguntaLower.contains("plan") || preguntaLower.contains("precio")
                || preguntaLower.contains("costo") || preguntaLower.contains("valor")) {
            List<PlanMembresia> planes = planDAO.listarTodos();
            if (!planes.isEmpty()) {
                contexto.append("PLANES DE MEMBRESÍA:\n");
                for (PlanMembresia p : planes) {
                    contexto.append(String.format(
                            "- %s: Mensual $%,.0f COP | Semestral $%,.0f COP | Anual $%,.0f COP. Beneficios: %s\n",
                            p.getNombre(), p.getPrecioMensual(),
                            p.getPrecioSemestral(), p.getPrecioAnual(),
                            p.getBeneficios()));
                }
            }
        }

        // ── INSTRUCTORES ──────────────────────────────────────────────────
        if (preguntaLower.contains("instructor") || preguntaLower.contains("entrenador")
                || preguntaLower.contains("disponib")
                || preguntaLower.contains("diego") || preguntaLower.contains("laura")
                || preguntaLower.contains("pedro")) {
            List<Instructor> instructores = instructorDAO.listarDisponibles();
            DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter fmtHora  = DateTimeFormatter.ofPattern("HH:mm");
            if (!instructores.isEmpty()) {
                contexto.append("INSTRUCTORES Y DISPONIBILIDAD:\n");
                for (Instructor i : instructores) {
                    contexto.append(String.format("- %s | ID: %s | Horario: %s\n",
                            i.getNombre(), i.getNumeroIdentificacion(), i.getDisponibilidad()));
                    List<Cita> citasPendientes = citaDAO.listarPorInstructor(i.getNumeroIdentificacion())
                            .stream().filter(c -> c.getEstado().equals("PENDIENTE"))
                            .collect(Collectors.toList());
                    if (!citasPendientes.isEmpty()) {
                        contexto.append("  Horas ocupadas:\n");
                        for (Cita c : citasPendientes) {
                            contexto.append(String.format("  * %s a las %s (cita #%d)\n",
                                    c.getFecha().format(fmtFecha),
                                    c.getHora().format(fmtHora),
                                    c.getIdCita()));
                        }
                    } else {
                        contexto.append("  Sin citas agendadas.\n");
                    }
                }
            }
        }

        // ── EJERCICIOS ────────────────────────────────────────────────────
        if (preguntaLower.contains("ejercicio") || preguntaLower.contains("rutina")
                || preguntaLower.contains("entrenamiento")) {
            List<Ejercicio> ejercicios = ejercicioDAO.listarTodos();
            if (!ejercicios.isEmpty()) {
                contexto.append(String.format("EJERCICIOS DISPONIBLES (%d):\n", ejercicios.size()));
                ejercicios.stream().limit(10).forEach(e ->
                        contexto.append(String.format("- %s | Grupo: %s | Series: %d x %d reps | Nivel: %s\n",
                                e.getNombre(), e.getGrupoMuscular(),
                                e.getSeries(), e.getRepeticiones(), e.getNivel())));
            }
        }

        // ── INGRESOS Y ASISTENCIA ─────────────────────────────────────────
        if (preguntaLower.contains("ingreso") || preguntaLower.contains("asistencia")
                || preguntaLower.contains("acceso") || preguntaLower.contains("pico")) {
            int ingresosMes = ingresoDAO.contarIngresosMes();
            contexto.append("REGISTRO DE INGRESOS:\n");
            contexto.append(String.format("- Ingresos este mes: %d\n", ingresosMes));
        }

        return contexto.toString();
    }

    // ── EXTRAER ID NUMÉRICO DE UN TEXTO ──────────────────────────────────
    public String extraerIdDeTexto(String texto) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\\b(\\d{4,12})\\b");
        java.util.regex.Matcher m = p.matcher(texto);
        if (m.find()) return m.group(1);
        return null;
    }
}