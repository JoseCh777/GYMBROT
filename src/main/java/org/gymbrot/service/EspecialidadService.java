package org.gymbrot.service;

import org.gymbrot.dao.EspecialidadDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.model.Especialidad;
import java.util.List;

/**
 * Service para gestión de especialidades de instructores.
 * Aplica validaciones de negocio antes de delegar al DAO.
 */
public class EspecialidadService {

    private EspecialidadDAO especialidadDAO;
    private InstructorDAO instructorDAO;

    public EspecialidadService() {
        this.especialidadDAO = new EspecialidadDAO();
        this.instructorDAO = new InstructorDAO();
    }

    /**
     * Lista todas las especialidades registradas.
     * @return Lista de especialidades, vacía si no hay ninguna.
     */
    public List<Especialidad> listarEspecialidades() {
        return especialidadDAO.listarTodas();
    }

    /**
     * Busca una especialidad por su ID.
     * @param idEspecialidad ID de la especialidad.
     * @return Especialidad encontrada, o null si no existe.
     */
    public Especialidad buscarEspecialidad(int idEspecialidad) {
        if (idEspecialidad <= 0) {
            System.err.println("✗ ID de especialidad inválido.");
            return null;
        }
        return especialidadDAO.buscarPorId(idEspecialidad);
    }
}