package org.gymbrot.dao;

import org.gymbrot.model.Cita;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
            ps.setDate(3, Date.valueOf(c.getFecha()));
            LocalDateTime fechaHora = LocalDateTime.of(c.getFecha(), c.getHora());
            ps.setTimestamp(4, Timestamp.valueOf(fechaHora));
            ps.setString(5, c.getTipoCita());
            ps.setString(6, c.getEstado());
            ps.setString(7, c.getNotas());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar cita: " + e.getMessage());
            return false;
        }
    }

    // ── INSERTAR Y RETORNAR ID ────────────────────────────────────────────
    public int insertarYRetornarId(Cita c) {
        String sql = """
            INSERT INTO CITAS
                (id_instructor, id_cliente, fecha, hora, tipo_cita, estado, notas)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, new String[]{"ID_CITA"})) {

            ps.setString(1, c.getIdInstructor());
            ps.setString(2, c.getIdCliente());
            ps.setDate(3, Date.valueOf(c.getFecha()));
            LocalDateTime fechaHora = LocalDateTime.of(c.getFecha(), c.getHora());
            ps.setTimestamp(4, Timestamp.valueOf(fechaHora));
            ps.setString(5, c.getTipoCita());
            ps.setString(6, c.getEstado());
            ps.setString(7, c.getNotas());
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) return rs.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error al insertar cita y retornar ID: " + e.getMessage());
        }
        return -1;
    }

    // ── ACTUALIZAR SOLO ESTADO (para cancelar/completar sin tocar hora) ───
    // citaDAO.actualizar() reconstruye el Timestamp de hora y puede fallar en
    // Oracle si la zona horaria difiere. Este método hace solo UPDATE estado.
    public boolean actualizarEstado(int idCita, String nuevoEstado) {
        String sql = "UPDATE CITAS SET estado = ? WHERE id_cita = ?";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, nuevoEstado);
            ps.setInt(2, idCita);
            boolean ok = ps.executeUpdate() > 0;
            System.out.println("[CitaDAO.actualizarEstado] id=" + idCita
                    + " estado=" + nuevoEstado + " resultado=" + ok);
            return ok;

        } catch (SQLException e) {
            System.err.println("Error al actualizar estado de cita: " + e.getMessage());
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
            ps.setDate(2, Date.valueOf(c.getFecha()));
            LocalDateTime fechaHora = LocalDateTime.of(c.getFecha(), c.getHora());
            ps.setTimestamp(3, Timestamp.valueOf(fechaHora));
            ps.setString(4, c.getTipoCita());
            ps.setString(5, c.getEstado());
            ps.setString(6, c.getNotas());
            ps.setInt(7, c.getIdCita());

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

    // ── ELIMINAR TODAS LAS CITAS DE UN CLIENTE ──────────────────────────────
    public boolean eliminarPorCliente(String idCliente) {
        String sql = "DELETE FROM CITAS WHERE id_cliente = ?";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idCliente);
            int filas = ps.executeUpdate();
            System.out.println("Citas eliminadas del cliente " + idCliente + ": " + filas);
            return true;

        } catch (SQLException e) {
            System.err.println("Error al eliminar citas del cliente: " + e.getMessage());
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
                if (rs.next()) return mapearCita(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar cita: " + e.getMessage());
        }
        return null;
    }

    // ── LISTAR POR CLIENTE ────────────────────────────────────────────────
    public List<Cita> listarPorCliente(String idCliente) {
        List<Cita> lista = new ArrayList<>();
        String sql = """
            SELECT id_cita, id_instructor, id_cliente, fecha, hora,
                   tipo_cita, estado, notas
            FROM CITAS WHERE id_cliente = ? ORDER BY id_cita ASC
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearCita(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al listar citas por cliente: " + e.getMessage());
        }
        return lista;
    }

    // ── LISTAR POR FECHA ──────────────────────────────────────────────────
    public List<Cita> listarPorFecha(LocalDate fecha) {
        List<Cita> lista = new ArrayList<>();
        String sql = """
            SELECT id_cita, id_instructor, id_cliente, fecha, hora,
                   tipo_cita, estado, notas
            FROM CITAS WHERE fecha = ? ORDER BY hora
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, Date.valueOf(fecha));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearCita(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al listar citas por fecha: " + e.getMessage());
        }
        return lista;
    }

    // ── LISTAR POR INSTRUCTOR ─────────────────────────────────────────────
    public List<Cita> listarPorInstructor(String idInstructor) {
        List<Cita> lista = new ArrayList<>();
        String sql = """
            SELECT id_cita, id_instructor, id_cliente, fecha, hora,
                   tipo_cita, estado, notas
            FROM CITAS WHERE id_instructor = ? ORDER BY fecha, hora
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idInstructor);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearCita(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al listar citas por instructor: " + e.getMessage());
        }
        return lista;
    }

    // ── MAPEO ─────────────────────────────────────────────────────────────
    private Cita mapearCita(ResultSet rs) throws SQLException {
        Timestamp horaTs = rs.getTimestamp("hora");
        LocalTime hora = (horaTs != null) ? horaTs.toLocalDateTime().toLocalTime() : LocalTime.MIDNIGHT;

        return new Cita(
                rs.getInt("id_cita"),
                rs.getString("id_instructor"),
                rs.getString("id_cliente"),
                rs.getDate("fecha").toLocalDate(),
                hora,
                rs.getString("tipo_cita"),
                rs.getString("estado"),
                rs.getString("notas")
        );
    }
}