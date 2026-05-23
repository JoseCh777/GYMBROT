package org.gymbrot.service;

import org.gymbrot.dao.CitaDAO;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.model.Cita;
import java.time.LocalDate;
import java.util.List;

public class CitaService {

    private CitaDAO citaDAO;
    private ClienteDAO clienteDAO;
    private InstructorDAO instructorDAO;

    public CitaService() {
        this.citaDAO = new CitaDAO();
        this.clienteDAO = new ClienteDAO();
        this.instructorDAO = new InstructorDAO();
    }

    // ── LISTAR POR CLIENTE ────────────────────────────────────────────────
    public List<Cita> listarPorCliente(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return null;
        }
        return citaDAO.listarPorCliente(idCliente);
    }

    // ── LISTAR POR INSTRUCTOR ─────────────────────────────────────────────
    public List<Cita> listarPorInstructor(String idInstructor) {
        if (idInstructor == null || idInstructor.trim().isEmpty()) {
            System.err.println("✗ ID de instructor inválido.");
            return null;
        }
        return citaDAO.listarPorInstructor(idInstructor);
    }

    // ── LISTAR POR FECHA ──────────────────────────────────────────────────
    public List<Cita> listarPorFecha(LocalDate fecha) {
        if (fecha == null) {
            System.err.println("✗ Fecha inválida.");
            return null;
        }
        return citaDAO.listarPorFecha(fecha);
    }

    // ── PROGRAMAR CITA (retorna boolean) ──────────────────────────────────
    public boolean programarCita(Cita cita) {
        return validarYProgramar(cita) && citaDAO.insertar(cita);
    }

    // ── PROGRAMAR CITA Y RETORNAR ID ──────────────────────────────────────
    public int programarCitaYRetornarId(Cita cita) {
        if (!validarYProgramar(cita)) return -1;
        int id = citaDAO.insertarYRetornarId(cita);
        if (id > 0) System.out.println("✓ Cita programada exitosamente.");
        return id;
    }

    // ── VALIDACIONES COMPARTIDAS ──────────────────────────────────────────
    private boolean validarYProgramar(Cita cita) {
        if (cita.getIdCliente() == null || cita.getIdCliente().trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return false;
        }
        if (cita.getIdInstructor() == null || cita.getIdInstructor().trim().isEmpty()) {
            System.err.println("✗ ID de instructor inválido.");
            return false;
        }
        if (clienteDAO.buscarPorId(cita.getIdCliente()) == null) {
            System.err.println("✗ No se encontró el cliente.");
            return false;
        }
        if (instructorDAO.buscarPorId(cita.getIdInstructor()) == null) {
            System.err.println("✗ No se encontró el instructor.");
            return false;
        }
        if (cita.getFecha() == null || !cita.getFecha().isAfter(LocalDate.now())) {
            System.err.println("✗ La fecha debe ser posterior a hoy.");
            return false;
        }
        if (cita.getHora() == null) {
            System.err.println("✗ La hora no puede estar vacía.");
            return false;
        }
        // Verificar conflicto de horario
        List<Cita> citasInstructor = citaDAO.listarPorInstructor(cita.getIdInstructor());
        for (Cita c : citasInstructor) {
            if (c.getFecha().equals(cita.getFecha()) &&
                    c.getHora().equals(cita.getHora()) &&
                    c.getEstado().equals("PENDIENTE")) {
                System.err.println("✗ El instructor ya tiene una cita en ese horario.");
                return false;
            }
        }
        cita.setEstado("PENDIENTE");
        return true;
    }

    // ── CANCELAR CITA ─────────────────────────────────────────────────────
    public boolean cancelarCita(int idCita) {
        if (idCita <= 0) {
            System.err.println("✗ ID de cita inválido.");
            return false;
        }
        Cita cita = citaDAO.buscarPorId(idCita);
        if (cita == null) {
            System.err.println("✗ No se encontró la cita con ID: " + idCita);
            return false;
        }
        if (!cita.getEstado().equals("PENDIENTE")) {
            System.err.println("✗ Solo se pueden cancelar citas en estado PENDIENTE.");
            return false;
        }
        cita.setEstado("CANCELADA");
        boolean resultado = citaDAO.actualizar(cita);
        if (resultado) System.out.println("✓ Cita cancelada exitosamente.");
        return resultado;
    }

    // ── COMPLETAR CITA ────────────────────────────────────────────────────
    public boolean completarCita(int idCita) {
        if (idCita <= 0) {
            System.err.println("✗ ID de cita inválido.");
            return false;
        }
        Cita cita = citaDAO.buscarPorId(idCita);
        if (cita == null) {
            System.err.println("✗ No se encontró la cita con ID: " + idCita);
            return false;
        }
        if (!cita.getEstado().equals("PENDIENTE")) {
            System.err.println("✗ Solo se pueden completar citas en estado PENDIENTE.");
            return false;
        }
        cita.setEstado("COMPLETADA");
        boolean resultado = citaDAO.actualizar(cita);
        if (resultado) System.out.println("✓ Cita completada exitosamente.");
        return resultado;
    }
}