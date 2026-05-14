package dao;

import model.RutinaEjercicio;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RutinaEjercicioDAO {

    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(RutinaEjercicio re) {
        String sql = """
            INSERT INTO RUTINA_EJERCICIO
                (id_rutina, id_ejercicio, orden, dia_semana, notas_instructor)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt   (1, re.getIdRutina());
            ps.setInt   (2, re.getIdEjercicio());
            ps.setInt   (3, re.getOrden());
            ps.setString(4, re.getDiaSemana());
            ps.setString(5, re.getNotasInstructor());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar rutina-ejercicio: " + e.getMessage());
            return false;
        }
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────────
    public boolean eliminar(int idRutina, int idEjercicio) {
        String sql = "DELETE FROM RUTINA_EJERCICIO WHERE id_rutina = ? AND id_ejercicio = ?";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idRutina);
            ps.setInt(2, idEjercicio);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar rutina-ejercicio: " + e.getMessage());
            return false;
        }
    }

    // ── LISTAR POR RUTINA ─────────────────────────────────────────────────
    public List<RutinaEjercicio> listarPorRutina(int idRutina) {
        List<RutinaEjercicio> lista = new ArrayList<>();
        String sql = """
            SELECT id_rutina, id_ejercicio, orden, dia_semana, notas_instructor
            FROM RUTINA_EJERCICIO
            WHERE id_rutina = ?
            ORDER BY dia_semana, orden
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idRutina);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new RutinaEjercicio(
                            rs.getInt   ("id_rutina"),
                            rs.getInt   ("id_ejercicio"),
                            rs.getInt   ("orden"),
                            rs.getString("dia_semana"),
                            rs.getString("notas_instructor")
                    ));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error al listar por rutina: " + e.getMessage());
        }
        return lista;
    }

    // ── ACTUALIZAR ORDEN ──────────────────────────────────────────────────
    public boolean actualizarOrden(int idRutina, int idEjercicio, int orden) {
        String sql = """
            UPDATE RUTINA_EJERCICIO
            SET orden = ?
            WHERE id_rutina = ? AND id_ejercicio = ?
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, orden);
            ps.setInt(2, idRutina);
            ps.setInt(3, idEjercicio);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar orden: " + e.getMessage());
            return false;
        }
    }

}