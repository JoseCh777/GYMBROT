package org.gymbrot.service;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.ProgresoDAO;
import org.gymbrot.model.Progreso;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.List;

public class ProgresoService {

    private ProgresoDAO progresoDAO;
    private ClienteDAO clienteDAO;

    public ProgresoService() {
        this.progresoDAO = new ProgresoDAO();
        this.clienteDAO = new ClienteDAO();
    }

    public Progreso ultimoProgreso(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) return null;
        return progresoDAO.ultimoProgreso(idCliente);
    }

    public List<Progreso> listarProgreso(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) return null;
        return progresoDAO.listarPorCliente(idCliente);
    }

    public boolean registrarProgreso(Progreso progreso) {
        String sql = "{call PKG_GYMBROT.SP_REGISTRAR_PROGRESO(?,?,?,?,?,?,?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, progreso.getIdCliente());
            cs.setDouble(2, progreso.getPeso());
            cs.setDouble(3, progreso.getAltura());
            if (progreso.getPorcentajeGrasa() > 0) {
                cs.setDouble(4, progreso.getPorcentajeGrasa());
            } else {
                cs.setNull(4, Types.DOUBLE);
            }
            if (progreso.getMasaMuscular() > 0) {
                cs.setDouble(5, progreso.getMasaMuscular());
            } else {
                cs.setNull(5, Types.DOUBLE);
            }
            cs.setString(6, progreso.getObjetivo());
            cs.setString(7, progreso.getObservaciones());
            cs.registerOutParameter(8, Types.INTEGER);
            cs.registerOutParameter(9, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(8);
            String mensaje = cs.getString(9);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en registrarProgreso: " + e.getMessage());
            return false;
        }
    }

}
