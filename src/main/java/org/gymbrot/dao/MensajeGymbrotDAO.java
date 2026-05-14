package dao;

import model.MensajeGymbrot;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MensajeGymbrotDAO {

    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(MensajeGymbrot m) {
        String sql = """
            INSERT INTO MENSAJES_GYMBROT
                (id_sesion, id_cita, remitente, contenido,
                 timestamp_msg, tipo_intencion)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt      (1, m.getIdSesion());

            if (m.getIdCita() != null) ps.setInt(2, m.getIdCita());
            else                       ps.setNull(2, Types.INTEGER);

            ps.setString   (3, m.getRemitente());
            ps.setString   (4, m.getContenido());
            ps.setTimestamp(5, Timestamp.valueOf(m.getTimestampMsg()));
            ps.setString   (6, m.getTipoIntencion());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error al insertar mensaje Gymbrot: " + e.getMessage());
            return false;
        }
    }

    // ── LISTAR POR SESIÓN ─────────────────────────────────────────────────
    public List<MensajeGymbrot> listarPorSesion(int idSesion) {
        List<MensajeGymbrot> lista = new ArrayList<>();
        String sql = """
            SELECT id_mensaje, id_sesion, id_cita, remitente,
                   contenido, timestamp_msg, tipo_intencion
            FROM MENSAJES_GYMBROT
            WHERE id_sesion = ?
            ORDER BY timestamp_msg ASC
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idSesion);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) lista.add(mapearMensaje(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error al listar mensajes por sesión: " + e.getMessage());
        }
        return lista;
    }

    // ── MAPEO ─────────────────────────────────────────────────────────────
    private MensajeGymbrot mapearMensaje(ResultSet rs) throws SQLException {
        int idCitaRaw = rs.getInt("id_cita");
        Integer idCita = rs.wasNull() ? null : idCitaRaw;

        return new MensajeGymbrot(
                rs.getInt      ("id_mensaje"),
                rs.getInt      ("id_sesion"),
                idCita,
                rs.getString   ("remitente"),
                rs.getString   ("contenido"),
                rs.getTimestamp("timestamp_msg").toLocalDateTime(),
                rs.getString   ("tipo_intencion")
        );
    }

}