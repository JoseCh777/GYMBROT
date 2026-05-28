package org.gymbrot.dao;

import org.gymbrot.model.Administrador;
import org.gymbrot.util.DatabaseConnection;
import java.sql.*;

public class AdministradorDAO {

    // ── CONEXIÓN (fresca en cada método, evita ORA-17008) ─────────────────
    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── BUSCAR POR ID ─────────────────────────────────────────────────────
    public Administrador buscarPorId(String numeroIdentificacion) {
        String sql = "SELECT a.*, u.tipo_identificacion, u.nombre, u.apellidos, " +
                "u.telefono, u.correo, u.contrasena_hash, u.foto_url, u.estado, " +
                "u.fecha_registro, u.tipo_usuario " +
                "FROM ADMINISTRADORES a " +
                "INNER JOIN USUARIOS u ON a.numero_identificacion = u.numero_identificacion " +
                "WHERE a.numero_identificacion = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, numeroIdentificacion);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearAdministrador(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error buscar admin por ID: " + e.getMessage());
        }
        return null;
    }

    // ── BUSCAR POR CORREO ─────────────────────────────────────────────────
    public Administrador buscarPorCorreo(String correo) {
        String sql = "SELECT a.*, u.tipo_identificacion, u.nombre, u.apellidos, " +
                "u.telefono, u.correo, u.contrasena_hash, u.foto_url, u.estado, " +
                "u.fecha_registro, u.tipo_usuario " +
                "FROM ADMINISTRADORES a " +
                "INNER JOIN USUARIOS u ON a.numero_identificacion = u.numero_identificacion " +
                "WHERE u.correo = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, correo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearAdministrador(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error buscar admin por correo: " + e.getMessage());
        }
        return null;
    }

    // ── ACTUALIZAR ────────────────────────────────────────────────────────
    public boolean actualizar(Administrador administrador) {
        String sql = "UPDATE ADMINISTRADORES SET rol=? WHERE numero_identificacion=?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, administrador.getRol());
            pstmt.setString(2, administrador.getNumeroIdentificacion());
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error actualizar admin: " + e.getMessage());
            return false;
        }
    }

    // ── MAPEO ─────────────────────────────────────────────────────────────
    private Administrador mapearAdministrador(ResultSet rs) throws SQLException {
        Administrador admin = new Administrador();

        admin.setNumeroIdentificacion(rs.getString("numero_identificacion"));
        admin.setTipoIdentificacion(rs.getString("tipo_identificacion"));
        admin.setNombre(rs.getString("nombre"));
        admin.setApellidos(rs.getString("apellidos"));
        admin.setTelefono(rs.getString("telefono"));
        admin.setCorreo(rs.getString("correo"));
        admin.setContrasenaHash(rs.getString("contrasena_hash"));
        admin.setFotoUrl(rs.getString("foto_url"));
        admin.setEstado(rs.getString("estado"));

        Date fechaReg = rs.getDate("fecha_registro");
        if (fechaReg != null) {
            admin.setFechaRegistro(fechaReg.toLocalDate());
        }

        admin.setTipoUsuario(rs.getString("tipo_usuario"));
        admin.setRol(rs.getString("rol"));

        return admin;
    }
}