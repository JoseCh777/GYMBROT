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
    /**
     * Registra un nuevo plan de membresía.
     * Valida que el nombre no esté vacío y los precios sean positivos.
     * @param plan Plan a registrar.
     * @return true si se registró exitosamente.
     */
    public boolean registrarPlan(PlanMembresia plan) {
        // 1. Validar nombre
        if (plan.getNombre() == null || plan.getNombre().trim().isEmpty()) {
            System.err.println("✗ El nombre del plan no puede estar vacío.");
            return false;
        }

        // 2. Validar precios
        if (plan.getPrecioMensual() <= 0 || plan.getPrecioSemestral() <= 0 || plan.getPrecioAnual() <= 0) {
            System.err.println("✗ Los precios deben ser mayores a 0.");
            return false;
        }

        // 3. Validar coherencia de precios (anual < semestral*2 < mensual*12)
        if (plan.getPrecioAnual() >= plan.getPrecioMensual() * 12) {
            System.err.println("✗ El precio anual debe ser menor al equivalente mensual.");
            return false;
        }

        // 4. Verificar nombre duplicado
        List<PlanMembresia> existentes = planDAO.listarTodos();
        for (PlanMembresia p : existentes) {
            if (p.getNombre().equalsIgnoreCase(plan.getNombre().trim())) {
                System.err.println("✗ Ya existe un plan con ese nombre.");
                return false;
            }
        }

        // 5. Persistir
        boolean resultado = planDAO.insertar(plan);
        if (resultado) {
            System.out.println("✓ Plan '" + plan.getNombre() + "' registrado exitosamente.");
        }
        return resultado;
    }

    /**
     * Actualiza un plan de membresía existente.
     * @param plan Plan con los datos actualizados.
     * @return true si se actualizó exitosamente.
     */
    public boolean actualizarPlan(PlanMembresia plan) {
        // 1. Validar ID
        if (plan.getIdPlan() <= 0) {
            System.err.println("✗ ID de plan inválido.");
            return false;
        }

        // 2. Verificar que exista
        if (planDAO.buscarPorId(plan.getIdPlan()) == null) {
            System.err.println("✗ No se encontró el plan con ID: " + plan.getIdPlan());
            return false;
        }

        // 3. Validar precios
        if (plan.getPrecioMensual() <= 0 || plan.getPrecioSemestral() <= 0 || plan.getPrecioAnual() <= 0) {
            System.err.println("✗ Los precios deben ser mayores a 0.");
            return false;
        }

        // 4. Persistir
        boolean resultado = planDAO.actualizar(plan);
        if (resultado) {
            System.out.println("✓ Plan actualizado exitosamente.");
        }
        return resultado;
    }

}