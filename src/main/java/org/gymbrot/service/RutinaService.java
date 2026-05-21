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
    /**
     * Crea una nueva rutina para un cliente.
     * Valida que cliente e instructor existan y la fecha sea válida.
     * @param rutina Rutina a crear.
     * @return true si se creó exitosamente.
     */
    public boolean crearRutina(Rutina rutina) {
        // 1. Validar cliente
        if (rutina.getIdCliente() == null || rutina.getIdCliente().trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return false;
        }

        // 2. Validar instructor
        if (rutina.getIdInstructor() == null || rutina.getIdInstructor().trim().isEmpty()) {
            System.err.println("✗ ID de instructor inválido.");
            return false;
        }

        // 3. Validar nombre
        if (rutina.getNombre() == null || rutina.getNombre().trim().isEmpty()) {
            System.err.println("✗ El nombre de la rutina no puede estar vacío.");
            return false;
        }

        // 4. Verificar que existan en BD
        if (clienteDAO.buscarPorId(rutina.getIdCliente()) == null) {
            System.err.println("✗ No se encontró el cliente.");
            return false;
        }
        if (instructorDAO.buscarPorId(rutina.getIdInstructor()) == null) {
            System.err.println("✗ No se encontró el instructor.");
            return false;
        }

        // 5. Validar fecha fin posterior a hoy
        if (rutina.getFechaFin() != null && !rutina.getFechaFin().isAfter(LocalDate.now())) {
            System.err.println("✗ La fecha fin debe ser posterior a hoy.");
            return false;
        }

        // 6. Establecer fecha de creación
        rutina.setFechaCreacion(LocalDate.now());

        // 7. Persistir
        boolean resultado = rutinaDAO.insertar(rutina);
        if (resultado) {
            System.out.println("✓ Rutina '" + rutina.getNombre() + "' creada exitosamente.");
        }
        return resultado;
    }

    /**
     * Actualiza una rutina existente.
     * @param rutina Rutina con datos actualizados.
     * @return true si se actualizó exitosamente.
     */
    public boolean actualizarRutina(Rutina rutina) {
        // 1. Validar ID
        if (rutina.getIdRutina() <= 0) {
            System.err.println("✗ ID de rutina inválido.");
            return false;
        }

        // 2. Validar nombre
        if (rutina.getNombre() == null || rutina.getNombre().trim().isEmpty()) {
            System.err.println("✗ El nombre de la rutina no puede estar vacío.");
            return false;
        }

        // 3. Validar fecha fin
        if (rutina.getFechaFin() != null && !rutina.getFechaFin().isAfter(LocalDate.now())) {
            System.err.println("✗ La fecha fin debe ser posterior a hoy.");
            return false;
        }

        // 4. Persistir
        boolean resultado = rutinaDAO.actualizar(rutina);
        if (resultado) {
            System.out.println("✓ Rutina actualizada exitosamente.");
        }
        return resultado;
    }
    /**
     * Elimina una rutina y todos sus ejercicios asociados.
     * @param idRutina ID de la rutina a eliminar.
     * @return true si se eliminó exitosamente.
     */
    public boolean eliminarRutina(int idRutina) {
        // 1. Validar ID
        if (idRutina <= 0) {
            System.err.println("✗ ID de rutina inválido.");
            return false;
        }

        // 2. Eliminar ejercicios asociados primero
        List<RutinaEjercicio> ejercicios = rutinaEjercicioDAO.listarPorRutina(idRutina);
        for (RutinaEjercicio re : ejercicios) {
            rutinaEjercicioDAO.eliminar(idRutina, re.getIdEjercicio());
        }

        // 3. Eliminar rutina
        boolean resultado = rutinaDAO.eliminar(idRutina);
        if (resultado) {
            System.out.println("✓ Rutina eliminada exitosamente.");
        }
        return resultado;
    }

    /**
     * Agrega un ejercicio a una rutina existente.
     * Valida que la rutina y el ejercicio existan.
     * @param rutinaEjercicio Relación rutina-ejercicio a agregar.
     * @return true si se agregó exitosamente.
     */
    public boolean agregarEjercicio(RutinaEjercicio rutinaEjercicio) {
        // 1. Validar IDs
        if (rutinaEjercicio.getIdRutina() <= 0 || rutinaEjercicio.getIdEjercicio() <= 0) {
            System.err.println("✗ IDs de rutina o ejercicio inválidos.");
            return false;
        }

        // 2. Verificar que el ejercicio exista
        List<Ejercicio> ejercicios = ejercicioDAO.listarTodos();
        boolean existe = ejercicios.stream()
                .anyMatch(e -> e.getIdEjercicio() == rutinaEjercicio.getIdEjercicio());
        if (!existe) {
            System.err.println("✗ No se encontró el ejercicio.");
            return false;
        }

        // 3. Verificar que no esté ya agregado en ese día
        List<RutinaEjercicio> existentes = rutinaEjercicioDAO.listarPorRutina(rutinaEjercicio.getIdRutina());
        for (RutinaEjercicio re : existentes) {
            if (re.getIdEjercicio() == rutinaEjercicio.getIdEjercicio() &&
                    re.getDiaSemana().equalsIgnoreCase(rutinaEjercicio.getDiaSemana())) {
                System.err.println("✗ El ejercicio ya está agregado para ese día.");
                return false;
            }
        }

        // 4. Persistir
        boolean resultado = rutinaEjercicioDAO.insertar(rutinaEjercicio);
        if (resultado) {
            System.out.println("✓ Ejercicio agregado a la rutina exitosamente.");
        }
        return resultado;
    }

    /**
     * Quita un ejercicio de una rutina.
     * @param idRutina ID de la rutina.
     * @param idEjercicio ID del ejercicio a quitar.
     * @return true si se quitó exitosamente.
     */
    public boolean quitarEjercicio(int idRutina, int idEjercicio) {
        // 1. Validar IDs
        if (idRutina <= 0 || idEjercicio <= 0) {
            System.err.println("✗ IDs inválidos.");
            return false;
        }

        // 2. Persistir
        boolean resultado = rutinaEjercicioDAO.eliminar(idRutina, idEjercicio);
        if (resultado) {
            System.out.println("✓ Ejercicio quitado de la rutina exitosamente.");
        }
        return resultado;
    }
}