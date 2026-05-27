package org.gymbrot.service;

import org.gymbrot.dao.CitaDAO;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.model.Cita;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
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

    public List<Cita> listarTodas() {
        return citaDAO.listarTodas();
    }

    public List<Cita> listarPorCliente(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) return null;
        return citaDAO.listarPorCliente(idCliente);
    }

    public List<Cita> listarPorInstructor(String idInstructor) {
        if (idInstructor == null || idInstructor.trim().isEmpty()) return null;
        return citaDAO.listarPorInstructor(idInstructor);
    }

    public List<Cita> listarPorFecha(LocalDate fecha) {
        if (fecha == null) return null;
        return citaDAO.listarPorFecha(fecha);
    }

    public boolean programarCita(Cita cita) {
        String sql = "{call PKG_GYMBROT.SP_AGENDAR_CITA(?,?,?,?,?,?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, cita.getIdInstructor());
            cs.setString(2, cita.getIdCliente());
            cs.setDate(3, Date.valueOf(cita.getFecha()));
            cs.setTimestamp(4, Timestamp.valueOf(cita.getFecha().atTime(cita.getHora())));
            cs.setString(5, cita.getTipoCita().toUpperCase());
            cs.setString(6, cita.getNotas());
            cs.registerOutParameter(7, Types.INTEGER);
            cs.registerOutParameter(8, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(7);
            String mensaje = cs.getString(8);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en programarCita: " + e.getMessage());
            return false;
        }
    }

    public boolean cancelarCita(int idCita) {
        String sql = "{call PKG_GYMBROT.SP_CANCELAR_CITA(?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, idCita);
            cs.registerOutParameter(2, Types.INTEGER);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(2);
            String mensaje = cs.getString(3);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en cancelarCita: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarCita(Cita cita) {
        try {
            return citaDAO.actualizar(cita);
        } catch (Exception e) {
            System.err.println("Error en actualizarCita: " + e.getMessage());
            return false;
        }
    }

    public boolean completarCita(int idCita) {
        try {
            Cita cita = citaDAO.buscarPorId(idCita);
            if (cita == null) {
                System.err.println("Cita no encontrada");
                return false;
            }
            cita.setEstado("COMPLETADA");
            return citaDAO.actualizar(cita);
        } catch (Exception e) {
            System.err.println("Error en completarCita: " + e.getMessage());
            return false;
        }
    }

}
