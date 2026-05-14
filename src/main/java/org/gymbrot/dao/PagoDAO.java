package org.gymbrot.dao;

import org.gymbrot.model.Pago;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PagoDAO {

    private Connection getConexion() {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(Pago p) {
        String sql = """
                INSERT INTO PAGOS
                    (id_membresia, id_cliente, fecha_pago, valor,
                     metodo_pago, estado_pago, referencia_transaccion, observaciones)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, p.getIdMembresia());
            ps.setString(2, p.getIdCliente());
            ps.setDate(3, Date.valueOf(p.getFechaPago()));
            ps.setDouble(4, p.getValor());
            ps.setString(5, p.getMetodoPago());
            ps.setString(6, p.getEstadoPago());
            ps.setString(7, p.getReferenciaTransaccion());
            ps.setString(8, p.getObservaciones());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar pago: " + e.getMessage());
            return false;
        }
    }
    // ── BUSCAR POR ID ─────────────────────────────────────────────────────
    public Pago buscarPorId(int id) {
        String sql = "SELECT * FROM PAGOS WHERE id_pago = ?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar pago: " + e.getMessage());
        }
        return null;
    }
}