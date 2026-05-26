package org.gymbrot.dao;

import org.gymbrot.model.Pago;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PagoDAO {

    private Connection getConexion() throws SQLException {
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

    // ── LISTAR POR CLIENTE ────────────────────────────────────────────────
    public List<Pago> listarPorCliente(String idCliente) {
        List<Pago> lista = new ArrayList<>();
        String sql = """
                SELECT * FROM PAGOS
                WHERE id_cliente = ?
                ORDER BY fecha_pago DESC
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar pagos por cliente: " + e.getMessage());
        }
        return lista;
    }
    // ── LISTAR TODOS ──────────────────────────────────────────────────────
    public List<Pago> listarTodos() {
        List<Pago> lista = new ArrayList<>();
        String sql = "SELECT * FROM PAGOS ORDER BY fecha_pago DESC";
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.err.println("Error al listar pagos: " + e.getMessage());
        }
        return lista;
    }

    // ── LISTAR TODOS CON CLIENTE (JOIN) ────────────────────────────────────
    public record PagoConCliente(
        int idPago,
        String clienteNombre,
        String clienteApellidos,
        String planNombre,
        String modalidad,
        double valor,
        String metodoPago,
        String estadoPago,
        String fechaPago,
        String referencia
    ) {}

    public List<PagoConCliente> listarTodosConCliente() {
        List<PagoConCliente> lista = new ArrayList<>();
        String sql = """
            SELECT p.id_pago, u.nombre, u.apellidos, pm.nombre AS plan_nombre,
                   m.modalidad_pago, p.valor, p.metodo_pago, p.estado_pago,
                   p.fecha_pago, p.referencia_transaccion
            FROM PAGOS p
            JOIN HISTORIAL_MEMBRESIAS hm ON hm.id_membresia = p.id_membresia
            JOIN MEMBRESIAS m ON m.id_membresia = p.id_membresia
            JOIN PLANES_MEMBRESIAS pm ON pm.id_plan = m.id_plan
            JOIN USUARIOS u ON u.numero_identificacion = p.id_cliente
            ORDER BY p.fecha_pago DESC
            """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new PagoConCliente(
                    rs.getInt("id_pago"),
                    rs.getString("nombre"),
                    rs.getString("apellidos"),
                    rs.getString("plan_nombre"),
                    rs.getString("modalidad_pago"),
                    rs.getDouble("valor"),
                    rs.getString("metodo_pago"),
                    rs.getString("estado_pago"),
                    rs.getDate("fecha_pago") != null ? rs.getDate("fecha_pago").toLocalDate().toString() : "",
                    rs.getString("referencia_transaccion")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar pagos con cliente: " + e.getMessage());
        }
        return lista;
    }
    // ── LISTAR POR MEMBRESIA ──────────────────────────────────────────────
    public List<Pago> listarPorMembresia(int idMembresia) {
        List<Pago> lista = new ArrayList<>();
        String sql = """
                SELECT * FROM PAGOS
                WHERE id_membresia = ?
                ORDER BY fecha_pago DESC
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idMembresia);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar pagos por membresía: " + e.getMessage());
        }
        return lista;
    }
    // ── MAPEAR ResultSet → Pago ───────────────────────────────────────────
    private Pago mapear(ResultSet rs) throws SQLException {
        Pago p = new Pago();
        p.setIdPago(rs.getInt("id_pago"));
        p.setIdMembresia(rs.getInt("id_membresia"));
        p.setIdCliente(rs.getString("id_cliente"));
        p.setFechaPago(rs.getDate("fecha_pago").toLocalDate());
        p.setValor(rs.getDouble("valor"));
        p.setMetodoPago(rs.getString("metodo_pago"));
        p.setEstadoPago(rs.getString("estado_pago"));
        p.setReferenciaTransaccion(rs.getString("referencia_transaccion"));
        p.setObservaciones(rs.getString("observaciones"));
        return p;
    }
}