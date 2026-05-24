package org.gymbrot.service;

import org.gymbrot.dao.EjercicioDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.dao.PlanMembresiaDAO;
import org.gymbrot.model.Ejercicio;
import org.gymbrot.model.Instructor;
import org.gymbrot.model.PlanMembresia;

import java.util.List;

public class ConsultaGymbrotService {

    private final PlanMembresiaDAO planDAO = new PlanMembresiaDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final EjercicioDAO ejercicioDAO = new EjercicioDAO();

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
                || preguntaLower.contains("profesor")) {
            List<Instructor> instructores = instructorDAO.listarDisponibles();
            if (!instructores.isEmpty()) {
                contexto.append("INSTRUCTORES DISPONIBLES:\n");
                for (Instructor i : instructores) {
                    contexto.append(String.format("- %s: Disponibilidad %s\n",
                        i.getNombre(), i.getDisponibilidad()));
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