package dao;

import model.Progreso;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProgresoDAO {

    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(Progreso p) {
        String sql = """
            INSERT INTO PROGRESO
                (id_cliente, fecha_registro, peso, altura, imc,
                 porcentaje_grasa, masa_muscular, objetivo, observaciones)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, p.getIdCliente());
            ps.setDate  (2, Date.valueOf(p.getFechaRegistro()));
            ps.setDouble(3, p.getPeso());
            ps.setDouble(4, p.getAltura());
            ps.setDouble(5, p.getImc());
            ps.setDouble(6, p.getPorcentajeGrasa());
            ps.setDouble(7, p.getMasaMuscular());
            ps.setString(8, p.getObjetivo());
            ps.setString(9, p.getObservaciones());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar progreso: " + e.getMessage());
            return false;
        }
    }

    // ── LISTAR POR CLIENTE ────────────────────────────────────────────────
    public List<Progreso> listarPorCliente(String idCliente) {
        List<Progreso> lista = new ArrayList<>();
        String sql = """
            SELECT id_progreso, id_cliente, fecha_registro, peso, altura,
                   imc, porcentaje_grasa, masa_muscular, objetivo, observaciones
            FROM PROGRESO WHERE id_cliente = ? ORDER BY fecha_registro DESC
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearProgreso(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al listar progreso por cliente: " + e.getMessage());
        }
        return lista;
    }

    // ── ÚLTIMO PROGRESO ───────────────────────────────────────────────────
    public Progreso ultimoProgreso(String idCliente) {
        String sql = """
            SELECT id_progreso, id_cliente, fecha_registro, peso, altura,
                   imc, porcentaje_grasa, masa_muscular, objetivo, observaciones
            FROM PROGRESO WHERE id_cliente = ?
            ORDER BY fecha_registro DESC FETCH FIRST 1 ROWS ONLY
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearProgreso(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener último progreso: " + e.getMessage());
        }
        return null;
    }

    // ── MAPEO ─────────────────────────────────────────────────────────────
    private Progreso mapearProgreso(ResultSet rs) throws SQLException {
        return new Progreso(
                rs.getInt   ("id_progreso"),
                rs.getString("id_cliente"),
                rs.getDate  ("fecha_registro").toLocalDate(),
                rs.getDouble("peso"),
                rs.getDouble("altura"),
                rs.getDouble("imc"),
                rs.getDouble("porcentaje_grasa"),
                rs.getDouble("masa_muscular"),
                rs.getString("objetivo"),
                rs.getString("observaciones")
        );
    }
}