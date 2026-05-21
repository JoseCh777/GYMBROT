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
    /**
     * Registra una nueva especialidad.
     * Valida que el nombre no esté vacío ni duplicado.
     * @param nombre Nombre de la nueva especialidad.
     * @return true si se registró exitosamente.
     */
    public boolean registrarEspecialidad(String nombre) {
        // 1. Validar nombre
        if (nombre == null || nombre.trim().isEmpty()) {
            System.err.println("✗ El nombre de la especialidad no puede estar vacío.");
            return false;
        }

        // 2. Verificar duplicado
        List<Especialidad> existentes = especialidadDAO.listarTodas();
        for (Especialidad e : existentes) {
            if (e.getNombre().equalsIgnoreCase(nombre.trim())) {
                System.err.println("✗ Ya existe una especialidad con ese nombre.");
                return false;
            }
        }

        // 3. Crear y persistir
        Especialidad nueva = new Especialidad();
        nueva.setNombre(nombre.trim());

        boolean resultado = especialidadDAO.insertar(nueva);
        if (resultado) {
            System.out.println("✓ Especialidad '" + nombre.trim() + "' registrada exitosamente.");
        }
        return resultado;
    }

    /**
     * Actualiza el nombre de una especialidad existente.
     * @param idEspecialidad ID de la especialidad a actualizar.
     * @param nuevoNombre Nuevo nombre.
     * @return true si se actualizó exitosamente.
     */
    public boolean actualizarEspecialidad(int idEspecialidad, String nuevoNombre) {
        // 1. Validaciones
        if (idEspecialidad <= 0) {
            System.err.println("✗ ID inválido.");
            return false;
        }
        if (nuevoNombre == null || nuevoNombre.trim().isEmpty()) {
            System.err.println("✗ El nombre no puede estar vacío.");
            return false;
        }

        // 2. Verificar que exista
        Especialidad existente = especialidadDAO.buscarPorId(idEspecialidad);
        if (existente == null) {
            System.err.println("✗ No se encontró la especialidad con ID: " + idEspecialidad);
            return false;
        }

        // 3. Actualizar y persistir
        existente.setNombre(nuevoNombre.trim());
        boolean resultado = especialidadDAO.actualizar(existente);
        if (resultado) {
            System.out.println("✓ Especialidad actualizada exitosamente.");
        }
        return resultado;
    }
    /**
     * Elimina una especialidad por su ID.
     * No permite eliminar si hay instructores asignados a ella.
     * @param idEspecialidad ID de la especialidad a eliminar.
     * @return true si se eliminó exitosamente.
     */
    public boolean eliminarEspecialidad(int idEspecialidad) {
        // 1. Validar ID
        if (idEspecialidad <= 0) {
            System.err.println("✗ ID inválido.");
            return false;
        }

        // 2. Verificar que exista
        Especialidad especialidad = especialidadDAO.buscarPorId(idEspecialidad);
        if (especialidad == null) {
            System.err.println("✗ No se encontró la especialidad con ID: " + idEspecialidad);
            return false;
        }

        // 3. Verificar que no tenga instructores asignados
        if (instructorDAO.instructorEspecialidad(idEspecialidad)) {
            System.err.println("✗ No se puede eliminar: hay instructores asignados a esta especialidad.");
            return false;
        }

        // 4. Eliminar
        boolean resultado = especialidadDAO.eliminar(idEspecialidad);
        if (resultado) {
            System.out.println("✓ Especialidad eliminada exitosamente.");
        }
        return resultado;
    }
}