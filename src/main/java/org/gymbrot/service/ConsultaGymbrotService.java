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

    // ── BUSCAR INFO RELEVANTE SEGUN LA PREGUNTA ───────────────────────────
    public String buscarInfoRelevante(String pregunta) {
        String preguntaLower = pregunta.toLowerCase();
        StringBuilder contexto = new StringBuilder();

        // ── ESTADÍSTICAS DE CLIENTES ──────────────────────────────────────
        if (preguntaLower.contains("estadística") || preguntaLower.contains("estadistica")
                || preguntaLower.contains("clientes activos") || preguntaLower.contains("total clientes")
                || preguntaLower.contains("cuantos clientes") || preguntaLower.contains("resumen")) {
            List<Cliente> todos = clienteDAO.listarTodos();
            long activos = todos.stream().filter(c -> "ACTIVO".equalsIgnoreCase(c.getEstado())).count();
            long inactivos = todos.stream().filter(c -> "INACTIVO".equalsIgnoreCase(c.getEstado())).count();
            long suspendidos = todos.stream().filter(c -> "SUSPENDIDO".equalsIgnoreCase(c.getEstado())).count();
            int ingresosMes = ingresoDAO.contarIngresosMes();
            contexto.append("ESTADÍSTICAS DE CLIENTES:\n");
            contexto.append(String.format("- Total clientes: %d\n", todos.size()));
            contexto.append(String.format("- Activos: %d\n", activos));
            contexto.append(String.format("- Inactivos: %d\n", inactivos));
            contexto.append(String.format("- Suspendidos: %d\n", suspendidos));
            contexto.append(String.format("- Ingresos este mes: %d\n", ingresosMes));
        }

        // ── MEMBRESÍAS VENCIDAS O POR VENCER ──────────────────────────────
        if (preguntaLower.contains("venc") || preguntaLower.contains("membresía")
                || preguntaLower.contains("membresia") || preguntaLower.contains("semana")
                || preguntaLower.contains("próximas") || preguntaLower.contains("proximas")) {
            List<Membresia> vencidas = membresiaDAO.listarVencidas();
            List<Membresia> activas = membresiaDAO.listarPorEstado("ACTIVA");
            LocalDate hoy = LocalDate.now();
            LocalDate en7dias = hoy.plusDays(7);
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            List<Membresia> porVencer = activas.stream()
                    .filter(m -> !m.getFechaVencimiento().isAfter(en7dias))
                    .collect(Collectors.toList());

            if (!vencidas.isEmpty()) {
                contexto.append(String.format("MEMBRESÍAS VENCIDAS: %d\n", vencidas.size()));
            }
            if (!porVencer.isEmpty()) {
                contexto.append("MEMBRESÍAS POR VENCER (próximos 7 días):\n");
                for (Membresia m : porVencer) {
                    HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                    String idCliente = h != null ? h.getIdCliente() : "N/A";
                    long dias = java.time.temporal.ChronoUnit.DAYS.between(hoy, m.getFechaVencimiento());
                    contexto.append(String.format("- ID Membresía #%d | Cliente: %s | Vence: %s | En %d días\n",
                            m.getIdMembresia(), idCliente,
                            m.getFechaVencimiento().format(fmt), dias));
                }
            }
            if (vencidas.isEmpty() && porVencer.isEmpty()) {
                contexto.append("No hay membresías vencidas ni próximas a vencer.\n");
            }
        }

        // ── CITAS DEL DÍA O SEMANA ────────────────────────────────────────
        if (preguntaLower.contains("cita") || preguntaLower.contains("agenda")
                || preguntaLower.contains("hoy") || preguntaLower.contains("semana")) {
            List<Cita> citasHoy = citaDAO.listarPorFecha(LocalDate.now());
            DateTimeFormatter fmtH = DateTimeFormatter.ofPattern("HH:mm");
            if (!citasHoy.isEmpty()) {
                contexto.append(String.format("CITAS HOY (%d):\n", citasHoy.size()));
                for (Cita c : citasHoy) {
                    contexto.append(String.format("- Cita #%d | Cliente: %s | Instructor: %s | Hora: %s | Estado: %s\n",
                            c.getIdCita(), c.getIdCliente(), c.getIdInstructor(),
                            c.getHora().format(fmtH), c.getEstado()));
                }
            } else {
                contexto.append("No hay citas programadas para hoy.\n");
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
            if (!instructores.isEmpty()) {
                contexto.append("INSTRUCTORES Y DISPONIBILIDAD:\n");
                DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                DateTimeFormatter fmtHora = DateTimeFormatter.ofPattern("HH:mm");
                for (Instructor i : instructores) {
                    contexto.append(String.format("- %s: %s\n", i.getNombre(), i.getDisponibilidad()));
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
                contexto.append("EJERCICIOS DISPONIBLES:\n");
                ejercicios.stream().limit(10).forEach(e ->
                        contexto.append(String.format("- %s (%s): Series %d x %d reps\n",
                                e.getNombre(), e.getGrupoMuscular(),
                                e.getSeries(), e.getRepeticiones())));
            }
        }

        return contexto.toString();
    }
}