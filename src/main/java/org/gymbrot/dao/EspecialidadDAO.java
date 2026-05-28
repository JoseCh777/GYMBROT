package org.gymbrot.dao;

import org.gymbrot.model.Especialidad;
import org.gymbrot.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EspecialidadDAO {

    // ── CONEXIÓN (fresca en cada método, evita ORA-17008) ─────────────────
    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── LISTAR TODAS ──────────────────────────────────────────────────────
    public List<Especialidad> listarTodas() {
        List<Especialidad> especialidades = new ArrayList<>();
        String sql = "SELECT * FROM ESPECIALIDADES ORDER BY nombre";

        try (Connection conn = getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                especialidades.add(mapearEspecialidad(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error listar especialidades: " + e.getMessage());
        }
        return especialidades;
    }

    // ── BUSCAR POR ID ─────────────────────────────────────────────────────
    public Especialidad buscarPorId(int idEspecialidad) {
        String sql = "SELECT * FROM ESPECIALIDADES WHERE id_especialidad = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEspecialidad);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearEspecialidad(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error buscar especialidad: " + e.getMessage());
        }
        return null;
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(Especialidad especialidad) {
        String sql = "INSERT INTO ESPECIALIDADES (id_especialidad, nombre) VALUES (?, ?)";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, especialidad.getIdEspecialidad());
            pstmt.setString(2, especialidad.getNombre());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error insertar especialidad: " + e.getMessage());
            return false;
        }
    }

    // ── ACTUALIZAR ────────────────────────────────────────────────────────
    public boolean actualizar(Especialidad especialidad) {
        String sql = "UPDATE ESPECIALIDADES SET nombre = ? WHERE id_especialidad = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, especialidad.getNombre());
            pstmt.setInt(2, especialidad.getIdEspecialidad());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error actualizar especialidad: " + e.getMessage());
            return false;
        }
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────────
    public boolean eliminar(int idEspecialidad) {
        String sql = "DELETE FROM ESPECIALIDADES WHERE id_especialidad = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, idEspecialidad);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error eliminar especialidad: " + e.getMessage());
            return false;
        }
    }

    // ── MAPEO ─────────────────────────────────────────────────────────────
    private Especialidad mapearEspecialidad(ResultSet rs) throws SQLException {
        Especialidad especialidad = new Especialidad();
        especialidad.setIdEspecialidad(rs.getInt("id_especialidad"));
        especialidad.setNombre(rs.getString("nombre"));
        return especialidad;
    }
}