package org.gymbrot.service;

import org.gymbrot.dao.CitaDAO;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.model.Cita;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Service para gestión de citas entre clientes e instructores.
 * Valida disponibilidad y gestiona estados de citas.
 */
public class CitaService {

    private CitaDAO citaDAO;
    private ClienteDAO clienteDAO;
    private InstructorDAO instructorDAO;

    public CitaService() {
        this.citaDAO = new CitaDAO();
        this.clienteDAO = new ClienteDAO();
        this.instructorDAO = new InstructorDAO();
    }

    /**
     * Lista todas las citas de un cliente.
     * @param idCliente ID del cliente.
     * @return Lista de citas, vacía si no hay ninguna.
     */
    public List<Cita> listarPorCliente(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return null;
        }
        return citaDAO.listarPorCliente(idCliente);
    }

    /**
     * Lista todas las citas de un instructor.
     * @param idInstructor ID del instructor.
     * @return Lista de citas, vacía si no hay ninguna.
     */
    public List<Cita> listarPorInstructor(String idInstructor) {
        if (idInstructor == null || idInstructor.trim().isEmpty()) {
            System.err.println("✗ ID de instructor inválido.");
            return null;
        }
        return citaDAO.listarPorInstructor(idInstructor);
    }

    /**
     * Lista todas las citas programadas para una fecha.
     * @param fecha Fecha a consultar.
     * @return Lista de citas del día.
     */
    public List<Cita> listarPorFecha(LocalDate fecha) {
        if (fecha == null) {
            System.err.println("✗ Fecha inválida.");
            return null;
        }
        return citaDAO.listarPorFecha(fecha);
    }
