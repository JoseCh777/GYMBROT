package org.gymbrot.service;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.EjercicioDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.dao.RutinaDAO;
import org.gymbrot.dao.RutinaEjercicioDAO;
import org.gymbrot.model.Rutina;
import org.gymbrot.model.RutinaEjercicio;
import java.time.LocalDate;
import java.util.List;

/**
 * Service para gestión de rutinas de ejercicio.
 * Coordina RutinaDAO, RutinaEjercicioDAO, ClienteDAO e InstructorDAO.
 */
public class RutinaService {

    private RutinaDAO rutinaDAO;
    private RutinaEjercicioDAO rutinaEjercicioDAO;
    private ClienteDAO clienteDAO;
    private InstructorDAO instructorDAO;
    private EjercicioDAO ejercicioDAO;

    public RutinaService() {
        this.rutinaDAO = new RutinaDAO();
        this.rutinaEjercicioDAO = new RutinaEjercicioDAO();
        this.clienteDAO = new ClienteDAO();
        this.instructorDAO = new InstructorDAO();
        this.ejercicioDAO = new EjercicioDAO();
    }

    /**
     * Lista todas las rutinas de un cliente.
     * @param idCliente ID del cliente.
     * @return Lista de rutinas, vacía si no hay ninguna.
     */
    public List<Rutina> listarPorCliente(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return null;
        }
        return rutinaDAO.buscarPorCliente(idCliente);
    }

    /**
     * Lista todas las rutinas creadas por un instructor.
     * @param idInstructor ID del instructor.
     * @return Lista de rutinas, vacía si no hay ninguna.
     */
    public List<Rutina> listarPorInstructor(String idInstructor) {
        if (idInstructor == null || idInstructor.trim().isEmpty()) {
            System.err.println("✗ ID de instructor inválido.");
            return null;
        }
        return rutinaDAO.buscarPorInstructor(idInstructor);
    }

    /**
     * Lista todos los ejercicios de una rutina.
     * @param idRutina ID de la rutina.
     * @return Lista de ejercicios de la rutina.
     */
    public List<RutinaEjercicio> listarEjercicios(int idRutina) {
        if (idRutina <= 0) {
            System.err.println("✗ ID de rutina inválido.");
            return null;
        }
        return rutinaEjercicioDAO.listarPorRutina(idRutina);
    }
}