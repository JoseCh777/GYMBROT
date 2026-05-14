package org.gymbrot.dao;

import org.gymbrot.model.HistorialMembresia;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistorialMembresiaDAO {

    private Connection getConexion() {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(HistorialMembresia h) {
        String sql = """
                INSERT INTO HISTORIAL_MEMBRESIAS
                    (id_cliente, id_membresia, fecha_asignacion, fecha_fin, activa)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, h.getIdCliente());
            ps.setInt(2, h.getIdMembresia());
            ps.setDate(3, Date.valueOf(h.getFechaAsignacion()));
            ps.setDate(4, h.getFechaFin() != null ? Date.valueOf(h.getFechaFin()) : null);
            ps.setInt(5, h.getActiva());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar historial: " + e.getMessage());
            return false;
        }
    }
}