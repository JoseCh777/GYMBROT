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
     *
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
     *
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
     *
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

    /**
     * Programa una nueva cita entre un cliente y un instructor.
     * Valida que ambos existan, la fecha sea futura y no haya conflicto de horario.
     *
     * @param cita Cita a programar.
     * @return true si se programó exitosamente.
     */
    public boolean programarCita(Cita cita) {
        // 1. Validar cliente
        if (cita.getIdCliente() == null || cita.getIdCliente().trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return false;
        }

        // 2. Validar instructor
        if (cita.getIdInstructor() == null || cita.getIdInstructor().trim().isEmpty()) {
            System.err.println("✗ ID de instructor inválido.");
            return false;
        }

        // 3. Validar que existan en BD
        if (clienteDAO.buscarPorId(cita.getIdCliente()) == null) {
            System.err.println("✗ No se encontró el cliente.");
            return false;
        }
        if (instructorDAO.buscarPorId(cita.getIdInstructor()) == null) {
            System.err.println("✗ No se encontró el instructor.");
            return false;
        }

        // 4. Validar fecha futura
        if (cita.getFecha() == null || !cita.getFecha().isAfter(LocalDate.now())) {
            System.err.println("✗ La fecha debe ser posterior a hoy.");
            return false;
        }

        // 5. Validar hora
        if (cita.getHora() == null) {
            System.err.println("✗ La hora no puede estar vacía.");
            return false;
        }

        // 6. Verificar conflicto de horario del instructor
        List<Cita> citasInstructor = citaDAO.listarPorInstructor(cita.getIdInstructor());
        for (Cita c : citasInstructor) {
            if (c.getFecha().equals(cita.getFecha()) &&
                    c.getHora().equals(cita.getHora()) &&
                    c.getEstado().equals("PENDIENTE")) {
                System.err.println("✗ El instructor ya tiene una cita en ese horario.");
                return false;
            }
        }

        // 7. Establecer estado inicial
        cita.setEstado("PENDIENTE");

        // 8. Persistir
        boolean resultado = citaDAO.insertar(cita);
        if (resultado) {
            System.out.println("✓ Cita programada exitosamente.");
        }
        return resultado;
    }
    /**
     * Cancela una cita programada.
     * Solo se pueden cancelar citas en estado "programada".
     * @param idCita ID de la cita a cancelar.
     * @return true si se canceló exitosamente.
     */
    public boolean cancelarCita(int idCita) {
        // 1. Validar ID
        if (idCita <= 0) {
            System.err.println("✗ ID de cita inválido.");
            return false;
        }

        // 2. Verificar que exista
        Cita cita = citaDAO.buscarPorId(idCita);
        if (cita == null) {
            System.err.println("✗ No se encontró la cita con ID: " + idCita);
            return false;
        }

        // 3. Verificar estado
        if (!cita.getEstado().equals("PENDIENTE")) {
            System.err.println("✗ Solo se pueden cancelar citas programadas.");
            return false;
        }

        // 4. Cambiar estado y persistir
        cita.setEstado("CANCELADA");
        boolean resultado = citaDAO.actualizar(cita);
        if (resultado) {
            System.out.println("✓ Cita cancelada exitosamente.");
        }
        return resultado;
    }

    /**
     * Marca una cita como completada.
     * Solo se pueden completar citas en estado "programada".
     * @param idCita ID de la cita a completar.
     * @return true si se completó exitosamente.
     */
    public boolean completarCita(int idCita) {
        // 1. Validar ID
        if (idCita <= 0) {
            System.err.println("✗ ID de cita inválido.");
            return false;
        }

        // 2. Verificar que exista
        Cita cita = citaDAO.buscarPorId(idCita);
        if (cita == null) {
            System.err.println("✗ No se encontró la cita con ID: " + idCita);
            return false;
        }

        // 3. Verificar estado
        if (!cita.getEstado().equals("PENDIENTE")) {
            System.err.println("✗ Solo se pueden completar citas programadas.");
            return false;
        }

        // 4. Cambiar estado y persistir
        cita.setEstado("COMPLETADA");
        boolean resultado = citaDAO.actualizar(cita);
        if (resultado) {
            System.out.println("✓ Cita completada exitosamente.");
        }
        return resultado;
    }
}