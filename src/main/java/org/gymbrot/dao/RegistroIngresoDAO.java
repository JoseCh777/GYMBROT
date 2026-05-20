package org.gymbrot.dao;

import org.gymbrot.model.RegistroIngreso;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RegistroIngresoDAO {

    private Connection getConexion() throws SQLException  {
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
    // ── LISTAR POR CLIENTE ────────────────────────────────────────────────
    public List<RegistroIngreso> listarPorCliente(String idCliente) {
        List<RegistroIngreso> lista = new ArrayList<>();
        String sql = """
                SELECT * FROM REGISTROS_INGRESOS
                WHERE id_cliente = ?
                ORDER BY fecha DESC, hora_entrada DESC
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar ingresos por cliente: " + e.getMessage());
        }
        return lista;
    }

    // ── LISTAR POR FECHA ──────────────────────────────────────────────────
    public List<RegistroIngreso> listarPorFecha(java.time.LocalDate fecha) {
        List<RegistroIngreso> lista = new ArrayList<>();
        String sql = """
                SELECT * FROM REGISTROS_INGRESOS
                WHERE fecha = ?
                ORDER BY hora_entrada DESC
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapear(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error al listar ingresos por fecha: " + e.getMessage());
        }
        return lista;
    }
    // ── CONTAR INGRESOS MES ───────────────────────────────────────────────
    public int contarIngresosMes() {
        String sql = """
                SELECT COUNT(*) FROM REGISTROS_INGRESOS
                WHERE EXTRACT(MONTH FROM fecha) = EXTRACT(MONTH FROM SYSDATE)
                AND EXTRACT(YEAR FROM fecha) = EXTRACT(YEAR FROM SYSDATE)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error al contar ingresos del mes: " + e.getMessage());
        }
        return 0;
    }

    // ── MAPEAR ResultSet → RegistroIngreso ────────────────────────────────
    private RegistroIngreso mapear(ResultSet rs) throws SQLException {
        RegistroIngreso r = new RegistroIngreso();
        r.setIdIngreso(rs.getInt("id_ingreso"));
        r.setIdCliente(rs.getString("id_cliente"));
        r.setFecha(rs.getDate("fecha").toLocalDate());
        r.setHoraEntrada(rs.getTimestamp("hora_entrada").toLocalDateTime());
        r.setHoraSalida(rs.getTimestamp("hora_salida") != null ? rs.getTimestamp("hora_salida").toLocalDateTime() : null);
        r.setMetodoVerificacion(rs.getString("metodo_verificacion"));
        r.setEstadoVerificacion(rs.getString("estado_verificacion"));
        return r;
    }
}