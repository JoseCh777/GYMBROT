package org.gymbrot.dao;

import org.gymbrot.model.Membresia;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MembresiaDAO {

    private Connection getConexion() {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(Membresia m) {
        String sql = """
                INSERT INTO MEMBRESIAS
                    (id_plan, tipo_membresia, modalidad_pago, valor,
                     fecha_inicio, fecha_vencimiento, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, m.getIdPlan());
            ps.setString(2, m.getTipoMembresia());
            ps.setString(3, m.getModalidadPago());
            ps.setDouble(4, m.getValor());
            ps.setDate(5, Date.valueOf(m.getFechaInicio()));
            ps.setDate(6, Date.valueOf(m.getFechaVencimiento()));
            ps.setString(7, m.getEstado());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar membresía: " + e.getMessage());
            return false;
        }
    }
    // ── ACTUALIZAR ────────────────────────────────────────────────────────
    public boolean actualizar(Membresia m) {
        String sql = """
                UPDATE MEMBRESIAS
                SET id_plan = ?, tipo_membresia = ?, modalidad_pago = ?,
                    valor = ?, fecha_inicio = ?, fecha_vencimiento = ?, estado = ?
                WHERE id_membresia = ?
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, m.getIdPlan());
            ps.setString(2, m.getTipoMembresia());
            ps.setString(3, m.getModalidadPago());
            ps.setDouble(4, m.getValor());
            ps.setDate(5, Date.valueOf(m.getFechaInicio()));
            ps.setDate(6, Date.valueOf(m.getFechaVencimiento()));
            ps.setString(7, m.getEstado());
            ps.setInt(8, m.getIdMembresia());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar membresía: " + e.getMessage());
            return false;
        }
    }
}