package dao;

import model.Cita;
import util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class CitaDAO {


    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(Cita c) {
        String sql = """
            INSERT INTO CITAS
                (id_instructor, id_cliente, fecha, hora, tipo_cita, estado, notas)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getIdInstructor());
            ps.setString(2, c.getIdCliente());
            ps.setDate  (3, Date.valueOf(c.getFecha()));
            ps.setTime  (4, Time.valueOf(c.getHora()));
            ps.setString(5, c.getTipoCita());
            ps.setString(6, c.getEstado());
            ps.setString(7, c.getNotas());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar cita: " + e.getMessage());
            return false;
        }
    }

    // ── ACTUALIZAR ────────────────────────────────────────────────────────
    public boolean actualizar(Cita c) {
        String sql = """
            UPDATE CITAS
            SET id_instructor = ?, fecha = ?, hora = ?,
                tipo_cita = ?, estado = ?, notas = ?
            WHERE id_cita = ?
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, c.getIdInstructor());
            ps.setDate  (2, Date.valueOf(c.getFecha()));
            ps.setTime  (3, Time.valueOf(c.getHora()));
            ps.setString(4, c.getTipoCita());
            ps.setString(5, c.getEstado());
            ps.setString(6, c.getNotas());
            ps.setInt   (7, c.getIdCita());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar cita: " + e.getMessage());
            return false;
        }
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────────
    public boolean eliminar(int id) {
        String sql = "DELETE FROM CITAS WHERE id_cita = ?";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar cita: " + e.getMessage());
            return false;
        }
    }

    // ── BUSCAR POR ID ─────────────────────────────────────────────────────
    public Cita buscarPorId(int id) {
        String sql = """
            SELECT id_cita, id_instructor, id_cliente, fecha, hora,
                   tipo_cita, estado, notas
            FROM CITAS WHERE id_cita = ?
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearCita(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar cita: " + e.getMessage());
        }
        return null;
    }


}