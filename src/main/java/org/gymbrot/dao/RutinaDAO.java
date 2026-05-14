package dao;

import model.Rutina;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RutinaDAO {


    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(Rutina r) {
        String sql = """
            INSERT INTO RUTINAS
                (id_instructor, id_cliente, nombre, descripcion,
                 fecha_creacion, fecha_fin, dias_semana, objetivo)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, r.getIdInstructor());
            ps.setString(2, r.getIdCliente());
            ps.setString(3, r.getNombre());
            ps.setString(4, r.getDescripcion());
            ps.setDate  (5, Date.valueOf(r.getFechaCreacion()));
            ps.setDate  (6, r.getFechaFin() != null ? Date.valueOf(r.getFechaFin()) : null);
            ps.setString(7, r.getDiasSemana());
            ps.setString(8, r.getObjetivo());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar rutina: " + e.getMessage());
            return false;
        }
    }

    // ── ACTUALIZAR ────────────────────────────────────────────────────────
    public boolean actualizar(Rutina r) {
        String sql = """
            UPDATE RUTINAS
            SET id_instructor = ?, nombre = ?, descripcion = ?,
                fecha_fin = ?, dias_semana = ?, objetivo = ?
            WHERE id_rutina = ?
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, r.getIdInstructor());
            ps.setString(2, r.getNombre());
            ps.setString(3, r.getDescripcion());
            ps.setDate  (4, r.getFechaFin() != null ? Date.valueOf(r.getFechaFin()) : null);
            ps.setString(5, r.getDiasSemana());
            ps.setString(6, r.getObjetivo());
            ps.setInt   (7, r.getIdRutina());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar rutina: " + e.getMessage());
            return false;
        }
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────────
    public boolean eliminar(int id) {
        String sql = "DELETE FROM RUTINAS WHERE id_rutina = ?";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar rutina: " + e.getMessage());
            return false;
        }
    }