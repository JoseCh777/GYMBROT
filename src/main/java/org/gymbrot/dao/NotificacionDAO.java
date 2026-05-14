package org.gymbrot.dao;

import org.gymbrot.model.Notificacion;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificacionDAO {

    private Connection getConexion() {
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
}