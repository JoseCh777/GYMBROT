package dao;

import model.Ejercicio;
import util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EjercicioDAO {

    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(Ejercicio e) {
        String sql = """
            INSERT INTO EJERCICIOS
                (nombre, descripcion, grupo_muscular, series,
                 repeticiones, duracion_minutos, nivel, recurso_url)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getNombre());
            ps.setString(2, e.getDescripcion());
            ps.setString(3, e.getGrupoMuscular());
            ps.setInt   (4, e.getSeries());
            ps.setInt   (5, e.getRepeticiones());
            ps.setInt   (6, e.getDuracionMinutos());
            ps.setString(7, e.getNivel());
            ps.setString(8, e.getRecursoUrl());

            return ps.executeUpdate() > 0;

        } catch (SQLException ex) {
            System.err.println("Error al insertar ejercicio: " + ex.getMessage());
            return false;
        }
    }

    // ── ACTUALIZAR ────────────────────────────────────────────────────────
    public boolean actualizar(Ejercicio e) {
        String sql = """
            UPDATE EJERCICIOS
            SET nombre = ?, descripcion = ?, grupo_muscular = ?,
                series = ?, repeticiones = ?, duracion_minutos = ?,
                nivel = ?, recurso_url = ?
            WHERE id_ejercicio = ?
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, e.getNombre());
            ps.setString(2, e.getDescripcion());
            ps.setString(3, e.getGrupoMuscular());
            ps.setInt   (4, e.getSeries());
            ps.setInt   (5, e.getRepeticiones());
            ps.setInt   (6, e.getDuracionMinutos());
            ps.setString(7, e.getNivel());
            ps.setString(8, e.getRecursoUrl());
            ps.setInt   (9, e.getIdEjercicio());

            return ps.executeUpdate() > 0;

        } catch (SQLException ex) {
            System.err.println("Error al actualizar ejercicio: " + ex.getMessage());
            return false;
        }
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────────
    public boolean eliminar(int id) {
        String sql = "DELETE FROM EJERCICIOS WHERE id_ejercicio = ?";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al eliminar ejercicio: " + e.getMessage());
            return false;
        }
    }

    // ── LISTAR TODOS ──────────────────────────────────────────────────────
    public List<Ejercicio> listarTodos() {
        List<Ejercicio> lista = new ArrayList<>();
        String sql = """
            SELECT id_ejercicio, nombre, descripcion, grupo_muscular,
                   series, repeticiones, duracion_minutos, nivel, recurso_url
            FROM EJERCICIOS ORDER BY nombre
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) lista.add(mapearEjercicio(rs));

        } catch (SQLException e) {
            System.err.println("Error al listar ejercicios: " + e.getMessage());
        }
        return lista;
    }

    // ── BUSCAR POR GRUPO MUSCULAR ─────────────────────────────────────────
    public List<Ejercicio> buscarPorGrupoMuscular(String grupo) {
        List<Ejercicio> lista = new ArrayList<>();
        String sql = """
            SELECT id_ejercicio, nombre, descripcion, grupo_muscular,
                   series, repeticiones, duracion_minutos, nivel, recurso_url
            FROM EJERCICIOS WHERE grupo_muscular = ? ORDER BY nombre
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, grupo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearEjercicio(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar por grupo muscular: " + e.getMessage());
        }
        return lista;
    }

    // ── BUSCAR POR NIVEL ──────────────────────────────────────────────────
    public List<Ejercicio> buscarPorNivel(String nivel) {
        List<Ejercicio> lista = new ArrayList<>();
        String sql = """
            SELECT id_ejercicio, nombre, descripcion, grupo_muscular,
                   series, repeticiones, duracion_minutos, nivel, recurso_url
            FROM EJERCICIOS WHERE nivel = ? ORDER BY nombre
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nivel);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearEjercicio(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar por nivel: " + e.getMessage());
        }
        return lista;
    }

    // ── MAPEO ─────────────────────────────────────────────────────────────
    private Ejercicio mapearEjercicio(ResultSet rs) throws SQLException {
        return new Ejercicio(
                rs.getInt   ("id_ejercicio"),
                rs.getString("nombre"),
                rs.getString("descripcion"),
                rs.getString("grupo_muscular"),
                rs.getInt   ("series"),
                rs.getInt   ("repeticiones"),
                rs.getInt   ("duracion_minutos"),
                rs.getString("nivel"),
                rs.getString("recurso_url")
        );
    }
}