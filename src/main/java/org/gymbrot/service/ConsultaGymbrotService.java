package org.gymbrot.service;

import org.gymbrot.dao.CitaDAO;
import org.gymbrot.dao.EjercicioDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.dao.PlanMembresiaDAO;
import org.gymbrot.model.Cita;
import org.gymbrot.model.Ejercicio;
import org.gymbrot.model.Instructor;
import org.gymbrot.model.PlanMembresia;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

public class ConsultaGymbrotService {

    private final PlanMembresiaDAO planDAO = new PlanMembresiaDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final EjercicioDAO ejercicioDAO = new EjercicioDAO();
    private final CitaDAO citaDAO = new CitaDAO();

    // ── BUSCAR INFO RELEVANTE SEGUN LA PREGUNTA ───────────────────────────
    public String buscarInfoRelevante(String pregunta) {
        String preguntaLower = pregunta.toLowerCase();
        StringBuilder contexto = new StringBuilder();

        // ── PLANES Y MEMBRESÍAS ───────────────────────────────────────────
        if (preguntaLower.contains("membresía") || preguntaLower.contains("membresia")
                || preguntaLower.contains("plan") || preguntaLower.contains("precio")
                || preguntaLower.contains("costo") || preguntaLower.contains("valor")) {
            List<PlanMembresia> planes = planDAO.listarTodos();
            if (!planes.isEmpty()) {
                contexto.append("PLANES DE MEMBRESÍA:\n");
                for (PlanMembresia p : planes) {
                    contexto.append(String.format(
                        "- %s: Mensual $%,.0f COP | Semestral $%,.0f COP | Anual $%,.0f COP. Beneficios: %s\n",
                        p.getNombre(), p.getPrecioMensual(),
                        p.getPrecioSemestral(), p.getPrecioAnual(),
                        p.getBeneficios()
                    ));
                }
            }
        }

        // ── INSTRUCTORES ──────────────────────────────────────────────────
        if (preguntaLower.contains("instructor") || preguntaLower.contains("entrenador")
                || preguntaLower.contains("profesor") || preguntaLower.contains("disponib")
                || preguntaLower.contains("diego") || preguntaLower.contains("laura")
                || preguntaLower.contains("pedro")) {

            List<Instructor> instructores = instructorDAO.listarDisponibles();
            if (!instructores.isEmpty()) {
                contexto.append("INSTRUCTORES Y DISPONIBILIDAD ACTUAL:\n");
                for (Instructor i : instructores) {
                    contexto.append(String.format("- %s: Horario general: %s\n",
                        i.getNombre(), i.getDisponibilidad()));

                    // ✅ Obtener citas PENDIENTE del instructor
                    List<Cita> citasPendientes = citaDAO.listarPorInstructor(i.getNumeroIdentificacion())
                        .stream()
                        .filter(c -> c.getEstado().equals("PENDIENTE"))
                        .collect(Collectors.toList());

                    if (!citasPendientes.isEmpty()) {
                        contexto.append("  Horas ya ocupadas (no disponibles):\n");
                        DateTimeFormatter fmtFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                        DateTimeFormatter fmtHora  = DateTimeFormatter.ofPattern("HH:mm");
                        for (Cita c : citasPendientes) {
                            contexto.append(String.format("  * %s a las %s (cita #%d)\n",
                                c.getFecha().format(fmtFecha),
                                c.getHora().format(fmtHora),
                                c.getIdCita()));
                        }
                    } else {
                        contexto.append("  Sin citas agendadas — disponible en todo su horario.\n");
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
                ejercicios.stream().limit(5).forEach(e ->
                    contexto.append(String.format("- %s (%s): Series %d x %d reps\n",
                        e.getNombre(), e.getGrupoMuscular(),
                        e.getSeries(), e.getRepeticiones()))
                );
            }
        }

        return contexto.toString();
    }
}