package org.gymbrot.service;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.ProgresoDAO;
import org.gymbrot.model.Progreso;
import java.time.LocalDate;
import java.util.List;

/**
 * Service para gestión del progreso físico de clientes.
 * Calcula IMC automáticamente y valida datos antes de persistir.
 */
public class ProgresoService {

    private ProgresoDAO progresoDAO;
    private ClienteDAO clienteDAO;

    public ProgresoService() {
        this.progresoDAO = new ProgresoDAO();
        this.clienteDAO = new ClienteDAO();
    }

    /**
     * Calcula el IMC dado peso y altura.
     * @param peso Peso en kilogramos.
     * @param altura Altura en metros.
     * @return IMC calculado, o -1 si los datos son inválidos.
     */
    public double calcularIMC(double peso, double altura) {
        if (peso <= 0 || altura <= 0) {
            System.err.println("✗ Peso y altura deben ser mayores a 0.");
            return -1;
        }
        return Math.round((peso / (altura * altura)) * 100.0) / 100.0;
    }

    /**
     * Retorna el último registro de progreso de un cliente.
     * @param idCliente ID del cliente.
     * @return Último progreso registrado, o null si no hay ninguno.
     */
    public Progreso ultimoProgreso(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return null;
        }
        return progresoDAO.ultimoProgreso(idCliente);
    }

    /**
     * Lista todo el historial de progreso de un cliente.
     * @param idCliente ID del cliente.
     * @return Lista de registros de progreso, vacía si no hay ninguno.
     */
    public List<Progreso> listarProgreso(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return null;
        }
        return progresoDAO.listarPorCliente(idCliente);
    }
}