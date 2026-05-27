package org.gymbrot.dao;

import org.gymbrot.model.Notificacion;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificacionDAO {

    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(Notificacion n) {
        String sql = """
                INSERT INTO NOTIFICACIONES
                    (id_cliente, id_plantilla, tipo, asunto,
                     contenido, estado_envio, fecha_envio, origen)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, n.getIdCliente());
            ps.setInt(2, n.getIdPlantilla());
            ps.setString(3, n.getTipo());
            ps.setString(4, n.getAsunto());
            ps.setString(5, n.getContenido());
            ps.setString(6, n.getEstadoEnvio());
            ps.setTimestamp(7, n.getFechaEnvio() != null ? Timestamp.valueOf(n.getFechaEnvio()) : null);
            ps.setString(8, n.getOrigen());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar notificación: " + e.getMessage());
            return false;
        }
    }

    // ── LISTAR POR CLIENTE ────────────────────────────────────────────────
    public List<Notificacion> listarPorCliente(String idCliente) {
        List<Notificacion> lista = new ArrayList<>();
        String sql = """
                SELECT * FROM NOTIFICACIONES
                WHERE id_cliente = ?
                ORDER BY fecha_envio DESC
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar notificaciones por cliente: " + e.getMessage());
        }
        return lista;
    }

    // ── MARCAR ENVIADA ────────────────────────────────────────────────────
    public boolean marcarEnviada(int idNotificacion) {
        String sql = """
                UPDATE NOTIFICACIONES
                SET estado_envio = 'ENVIADO', fecha_envio = CURRENT_TIMESTAMP
                WHERE id_notificacion = ?
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idNotificacion);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al marcar notificación enviada: " + e.getMessage());
            return false;
        }
    }

    // ── LISTAR PENDIENTES ─────────────────────────────────────────────────
    public List<Notificacion> listarPendientes() {
        List<Notificacion> lista = new ArrayList<>();
        String sql = """
                SELECT * FROM NOTIFICACIONES
                WHERE estado_envio = 'PENDIENTE'
                ORDER BY fecha_envio ASC
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.err.println("Error al listar notificaciones pendientes: " + e.getMessage());
        }
        return lista;
    }
    // ── ELIMINAR TODAS LAS NOTIFICACIONES DE UN CLIENTE ────────────────────────
    public boolean eliminarPorCliente(String idCliente) {
        String sql = "DELETE FROM NOTIFICACIONES WHERE id_cliente = ?";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, idCliente);
            int filas = ps.executeUpdate();
            System.out.println("Notificaciones eliminadas del cliente " + idCliente + ": " + filas);
            return true;
        } catch (SQLException e) {
            System.err.println("Error al eliminar notificaciones del cliente: " + e.getMessage());
            return false;
        }
    }
    // ── MAPEAR ResultSet → Notificacion ───────────────────────────────────
    private Notificacion mapear(ResultSet rs) throws SQLException {
        Notificacion n = new Notificacion();
        n.setIdNotificacion(rs.getInt("id_notificacion"));
        n.setIdCliente(rs.getString("id_cliente"));
        n.setIdPlantilla(rs.getInt("id_plantilla"));
        n.setTipo(rs.getString("tipo"));
        n.setAsunto(rs.getString("asunto"));
        n.setContenido(rs.getString("contenido"));
        n.setEstadoEnvio(rs.getString("estado_envio"));
        n.setFechaEnvio(rs.getTimestamp("fecha_envio") != null ? rs.getTimestamp("fecha_envio").toLocalDateTime() : null);
        n.setOrigen(rs.getString("origen"));
        return n;
    }
}