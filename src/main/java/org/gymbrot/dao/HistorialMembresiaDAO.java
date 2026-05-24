package org.gymbrot.dao;

import org.gymbrot.model.HistorialMembresia;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class HistorialMembresiaDAO {

    private Connection getConexion() throws SQLException {
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
            ps.setInt(5, h.isActiva() ? 1 : 0);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar historial: " + e.getMessage());
            return false;
        }
    }
    // ── LISTAR POR CLIENTE ────────────────────────────────────────────────
    public List<HistorialMembresia> listarPorCliente(String idCliente) {
        List<HistorialMembresia> lista = new ArrayList<>();
        String sql = """
                SELECT * FROM HISTORIAL_MEMBRESIAS
                WHERE id_cliente = ?
                ORDER BY fecha_asignacion DESC
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar historial por cliente: " + e.getMessage());
        }
        return lista;
    }
    // ── BUSCAR ACTIVA POR CLIENTE ─────────────────────────────────────────
    public HistorialMembresia buscarActiva(String idCliente) {
        String sql = """
                SELECT * FROM HISTORIAL_MEMBRESIAS
                WHERE id_cliente = ? AND activa = 1
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar historial activa: " + e.getMessage());
        }
        return null;
    }

    public HistorialMembresia buscarPorMembresia(int idMembresia) {
        String sql = """
            SELECT * FROM HISTORIAL_MEMBRESIAS
            WHERE id_membresia = ? AND activa = 1
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idMembresia);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar por membresía: " + e.getMessage());
        }
        return null;
    }

    // ── DESACTIVAR ────────────────────────────────────────────────────────
    public boolean desactivar(int idHistorial) {
        String sql = """
                UPDATE HISTORIAL_MEMBRESIAS
                SET activa = 0, fecha_fin = SYSDATE
                WHERE id_historial = ?
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idHistorial);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al desactivar historial: " + e.getMessage());
            return false;
        }
    }
    // ── MAPEAR ResultSet → HistorialMembresia ─────────────────────────────
    private HistorialMembresia mapear(ResultSet rs) throws SQLException {
        HistorialMembresia h = new HistorialMembresia();
        h.setIdHistorial(rs.getInt("id_historial"));
        h.setIdCliente(rs.getString("id_cliente"));
        h.setIdMembresia(rs.getInt("id_membresia"));
        h.setFechaAsignacion(rs.getDate("fecha_asignacion").toLocalDate());
        h.setFechaFin(rs.getDate("fecha_fin") != null ? rs.getDate("fecha_fin").toLocalDate() : null);
        h.setActiva(rs.getInt("activa") == 1);
        return h;
    }
}