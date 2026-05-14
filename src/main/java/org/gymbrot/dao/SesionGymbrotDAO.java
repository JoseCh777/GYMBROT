package dao;

import model.SesionGymbrot;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SesionGymbrotDAO {

    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── CREAR SESIÓN ──────────────────────────────────────────────────────
    public int crearSesion(SesionGymbrot s) {
        String sql = """
            INSERT INTO SESIONES_GYMBROT
                (id_cliente, fecha, hora_inicio, hora_fin, contexto_activo)
            VALUES (?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString   (1, s.getIdCliente());
            ps.setDate     (2, Date.valueOf(s.getFecha()));
            ps.setTimestamp(3, s.getHoraInicio() != null
                    ? Timestamp.valueOf(s.getHoraInicio()) : null);
            ps.setTimestamp(4, s.getHoraFin() != null
                    ? Timestamp.valueOf(s.getHoraFin()) : null);
            ps.setString   (5, s.getContextoActivo());

            ps.executeUpdate();

            try (ResultSet generados = ps.getGeneratedKeys()) {
                if (generados.next()) return generados.getInt(1);
            }

        } catch (SQLException e) {
            System.err.println("Error al crear sesión Gymbrot: " + e.getMessage());
        }
        return -1;
    }

    // ── CERRAR SESIÓN ─────────────────────────────────────────────────────
    public boolean cerrarSesion(int idSesion) {
        String sql = """
            UPDATE SESIONES_GYMBROT
            SET hora_fin = CURRENT_TIMESTAMP
            WHERE id_sesion = ?
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idSesion);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al cerrar sesión Gymbrot: " + e.getMessage());
            return false;
        }
    }

    // ── BUSCAR SESIÓN ACTIVA ──────────────────────────────────────────────
    public SesionGymbrot buscarSesionActiva(String idCliente) {
        String sql = """
            SELECT id_sesion, id_cliente, fecha, hora_inicio, hora_fin, contexto_activo
            FROM SESIONES_GYMBROT
            WHERE id_cliente = ? AND hora_fin IS NULL
            ORDER BY hora_inicio DESC FETCH FIRST 1 ROWS ONLY
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapearSesion(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error al buscar sesión activa: " + e.getMessage());
        }
        return null;
    }

    // ── ACTUALIZAR CONTEXTO ───────────────────────────────────────────────
    public boolean actualizarContexto(int idSesion, String contexto) {
        String sql = """
            UPDATE SESIONES_GYMBROT
            SET contexto_activo = ?
            WHERE id_sesion = ?
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, contexto);
            ps.setInt   (2, idSesion);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al actualizar contexto: " + e.getMessage());
            return false;
        }
    }

    // ── MAPEO ─────────────────────────────────────────────────────────────
    private SesionGymbrot mapearSesion(ResultSet rs) throws SQLException {
        Timestamp horaFin = rs.getTimestamp("hora_fin");
        return new SesionGymbrot(
                rs.getInt      ("id_sesion"),
                rs.getString   ("id_cliente"),
                rs.getDate     ("fecha").toLocalDate(),
                rs.getTimestamp("hora_inicio").toLocalDateTime(),
                horaFin != null ? horaFin.toLocalDateTime() : null,
                rs.getString   ("contexto_activo")
        );
    }

}