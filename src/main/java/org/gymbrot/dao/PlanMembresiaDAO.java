package org.gymbrot.dao;

import org.gymbrot.model.PlanMembresia;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanMembresiaDAO {

    private Connection getConexion() {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(PlanMembresia p) {
        String sql = """
                INSERT INTO PLANES_MEMBRESIAS
                    (nombre, descripcion, precio_mensual,
                     precio_semestral, precio_anual, beneficios)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setDouble(3, p.getPrecioMensual());
            ps.setDouble(4, p.getPrecioSemestral());
            ps.setDouble(5, p.getPrecioAnual());
            ps.setString(6, p.getBeneficios());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar plan: " + e.getMessage());
            return false;
        }
    }
    // ── ACTUALIZAR ────────────────────────────────────────────────────────
    public boolean actualizar(PlanMembresia p) {
        String sql = """
                UPDATE PLANES_MEMBRESIAS
                SET nombre = ?, descripcion = ?, precio_mensual = ?,
                    precio_semestral = ?, precio_anual = ?, beneficios = ?
                WHERE id_plan = ?
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setDouble(3, p.getPrecioMensual());
            ps.setDouble(4, p.getPrecioSemestral());
            ps.setDouble(5, p.getPrecioAnual());
            ps.setString(6, p.getBeneficios());
            ps.setInt(7, p.getIdPlan());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar plan: " + e.getMessage());
            return false;
        }
    }
}