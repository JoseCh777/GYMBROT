package org.gymbrot.service;

import org.gymbrot.dao.MembresiaDAO;
import org.gymbrot.dao.PlanMembresiaDAO;
import org.gymbrot.model.PlanMembresia;
import java.util.List;

/**
 * Service para gestión de planes de membresía.
 * Aplica validaciones de negocio antes de delegar al DAO.
 */
public class PlanMembresiaService {

    private PlanMembresiaDAO planDAO;
    private MembresiaDAO membresiaDAO;

    public PlanMembresiaService() {
        this.planDAO = new PlanMembresiaDAO();
        this.membresiaDAO = new MembresiaDAO();
    }

    /**
     * Lista todos los planes de membresía registrados.
     * @return Lista de planes, vacía si no hay ninguno.
     */
    public List<PlanMembresia> listarPlanes() {
        return planDAO.listarTodos();
    }

    /**
     * Busca un plan de membresía por su ID.
     * @param idPlan ID del plan.
     * @return Plan encontrado, o null si no existe.
     */
    public PlanMembresia buscarPlan(int idPlan) {
        if (idPlan <= 0) {
            System.err.println("✗ ID de plan inválido.");
            return null;
        }
        return planDAO.buscarPorId(idPlan);
    }

}