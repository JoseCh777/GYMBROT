package org.gymbrot.dao;

import org.gymbrot.model.Membresia;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MembresiaDAO {

    private Connection getConexion() throws SQLException  {
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
    // ── BUSCAR POR ID ─────────────────────────────────────────────────────
    public Membresia buscarPorId(int id) {
        String sql = "SELECT * FROM MEMBRESIAS WHERE id_membresia = ?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar membresía: " + e.getMessage());
        }
        return null;
    }

    public int insertarYRetornarId(Membresia m) throws SQLException {
        String sql = """
            INSERT INTO MEMBRESIAS
                (id_plan, tipo_membresia, modalidad_pago, valor,
                 fecha_inicio, fecha_vencimiento, estado)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        Connection conn = getConexion();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, m.getIdPlan());
            ps.setString(2, m.getTipoMembresia());
            ps.setString(3, m.getModalidadPago());
            ps.setDouble(4, m.getValor());
            ps.setDate(5, Date.valueOf(m.getFechaInicio()));
            ps.setDate(6, Date.valueOf(m.getFechaVencimiento()));
            ps.setString(7, m.getEstado());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error al insertar membresía: " + e.getMessage());
            return -1;
        }
        // Consultar el ID recién generado (Oracle identity column)
        try (PreparedStatement ps = conn.prepareStatement("SELECT MAX(id_membresia) FROM MEMBRESIAS");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                int id = rs.getInt(1);
                System.out.println("Membresia insertada con ID: " + id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("Error al obtener ID de membresía: " + e.getMessage());
        }
        return -1;
    }

    public boolean membresiaPlan(int idPlan) {
        String sql = "SELECT COUNT(*) FROM MEMBRESIAS WHERE id_plan = ? AND estado = 'ACTIVA'";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idPlan);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("Error verificar plan: " + e.getMessage());
        }
        return false;
    }
    // ── LISTAR VENCIDAS ───────────────────────────────────────────────────
    // Filtra SOLO por fecha para no depender del estado guardado en BD,
    // que puede estar desactualizado si el trigger no se disparó.
    // Excluye estado 'CANCELADA' para no mezclar membresías canceladas.
    public List<Membresia> listarVencidas() {
        List<Membresia> lista = new ArrayList<>();
        String sql = """
                SELECT * FROM MEMBRESIAS
                WHERE TRUNC(fecha_vencimiento) < TRUNC(SYSDATE)
                  AND estado != 'CANCELADA'
                ORDER BY fecha_vencimiento DESC
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.err.println("Error al listar vencidas: " + e.getMessage());
        }
        return lista;
    }

    // ── LISTAR POR ESTADO ─────────────────────────────────────────────────
    // Para estado = 'ACTIVA': además de filtrar por estado, valida que la
    // fecha de vencimiento sea hoy o futura, cubriendo el caso en que el
    // trigger no actualizó el estado de una membresía ya vencida.
    public List<Membresia> listarPorEstado(String estado) {
        List<Membresia> lista = new ArrayList<>();
        String sql;
        if ("ACTIVA".equalsIgnoreCase(estado)) {
            sql = """
                    SELECT * FROM MEMBRESIAS
                    WHERE estado = 'ACTIVA'
                      AND TRUNC(fecha_vencimiento) >= TRUNC(SYSDATE)
                    ORDER BY fecha_vencimiento ASC
                    """;
        } else {
            sql = "SELECT * FROM MEMBRESIAS WHERE estado = ? ORDER BY fecha_vencimiento";
        }
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            if (!"ACTIVA".equalsIgnoreCase(estado)) {
                ps.setString(1, estado);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar por estado: " + e.getMessage());
        }
        return lista;
    }
    // ── MAPEAR ResultSet → Membresia ──────────────────────────────────────
    private Membresia mapear(ResultSet rs) throws SQLException {
        Membresia m = new Membresia();
        m.setIdMembresia(rs.getInt("id_membresia"));
        m.setIdPlan(rs.getInt("id_plan"));
        m.setTipoMembresia(rs.getString("tipo_membresia"));
        m.setModalidadPago(rs.getString("modalidad_pago"));
        m.setValor(rs.getDouble("valor"));
        m.setFechaInicio(rs.getDate("fecha_inicio").toLocalDate());
        m.setFechaVencimiento(rs.getDate("fecha_vencimiento").toLocalDate());
        m.setEstado(rs.getString("estado"));
        return m;
    }
}