package org.gymbrot.dao;

import org.gymbrot.model.RegistroIngreso;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RegistroIngresoDAO {

    private Connection getConexion() {
        return DatabaseConnection.getInstance();
    }

    // ── REGISTRAR ENTRADA ─────────────────────────────────────────────────
    public boolean registrarEntrada(RegistroIngreso r) {
        String sql = """
                INSERT INTO REGISTROS_INGRESOS
                    (id_cliente, fecha, hora_entrada, metodo_verificacion, estado_verificacion)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, r.getIdCliente());
            ps.setDate(2, Date.valueOf(r.getFecha()));
            ps.setTimestamp(3, Timestamp.valueOf(r.getHoraEntrada()));
            ps.setString(4, r.getMetodoVerificacion());
            ps.setString(5, r.getEstadoVerificacion());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al registrar entrada: " + e.getMessage());
            return false;
        }
    }

    // ── REGISTRAR SALIDA ──────────────────────────────────────────────────
    public boolean registrarSalida(int idIngreso) {
        String sql = """
                UPDATE REGISTROS_INGRESOS
                SET hora_salida = CURRENT_TIMESTAMP
                WHERE id_ingreso = ?
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idIngreso);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al registrar salida: " + e.getMessage());
            return false;
        }
    }

}