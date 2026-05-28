package org.gymbrot.dao;

import org.gymbrot.model.Usuario;
import org.gymbrot.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    // ── CONEXIÓN (fresca en cada método, evita ORA-17008) ─────────────────
    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(Usuario usuario) {
        String sql = "INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, " +
                "apellidos, telefono, correo, contrasena_hash, foto_url, estado, " +
                "fecha_registro, tipo_usuario) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario.getNumeroIdentificacion());
            pstmt.setString(2, usuario.getTipoIdentificacion());
            pstmt.setString(3, usuario.getNombre());
            pstmt.setString(4, usuario.getApellidos());
            pstmt.setString(5, usuario.getTelefono());
            pstmt.setString(6, usuario.getCorreo());
            pstmt.setString(7, usuario.getContrasenaHash());
            pstmt.setString(8, usuario.getFotoUrl());
            pstmt.setString(9, usuario.getEstado());
            pstmt.setDate(10, Date.valueOf(usuario.getFechaRegistro()));
            pstmt.setString(11, usuario.getTipoUsuario());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error insertar: " + e.getMessage());
            return false;
        }
    }

    // ── ACTUALIZAR ────────────────────────────────────────────────────────
    public boolean actualizar(Usuario usuario) {
        String sql = "UPDATE USUARIOS SET tipo_identificacion=?, nombre=?, apellidos=?, " +
                "telefono=?, correo=?, contrasena_hash=?, foto_url=?, estado=?, " +
                "tipo_usuario=? WHERE numero_identificacion=?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, usuario.getTipoIdentificacion());
            pstmt.setString(2, usuario.getNombre());
            pstmt.setString(3, usuario.getApellidos());
            pstmt.setString(4, usuario.getTelefono());
            pstmt.setString(5, usuario.getCorreo());
            pstmt.setString(6, usuario.getContrasenaHash());
            pstmt.setString(7, usuario.getFotoUrl());
            pstmt.setString(8, usuario.getEstado());
            pstmt.setString(9, usuario.getTipoUsuario());
            pstmt.setString(10, usuario.getNumeroIdentificacion());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error actualizar: " + e.getMessage());
            return false;
        }
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────────
    public boolean eliminar(String numeroIdentificacion) {
        String sql = "DELETE FROM USUARIOS WHERE numero_identificacion = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, numeroIdentificacion);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error eliminar: " + e.getMessage());
            return false;
        }
    }

    // ── BUSCAR POR ID ─────────────────────────────────────────────────────
    public Usuario buscarPorId(String numeroIdentificacion) {
        String sql = "SELECT * FROM USUARIOS WHERE numero_identificacion = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, numeroIdentificacion);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapearUsuario(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error buscar por ID: " + e.getMessage());
        }
        return null;
    }

    // ── BUSCAR POR CORREO ─────────────────────────────────────────────────
    public Usuario buscarPorCorreo(String correo) {
        String sql = "SELECT * FROM USUARIOS WHERE correo = ? OR numero_identificacion = ? OR UPPER(nombre) = UPPER(?)";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, correo);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapearUsuario(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error buscar por correo: " + e.getMessage());
        }
        return null;
    }

    // ── BUSCAR POR NOMBRE O CORREO ────────────────────────────────────────
    // ✅ Permite login con correo o número de identificación
    public Usuario buscarPorNombreOCorreo(String valor) {
        String sql = "SELECT * FROM USUARIOS WHERE correo = ? OR numero_identificacion = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, valor);
            pstmt.setString(2, valor);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) return mapearUsuario(rs);
            }

        } catch (SQLException e) {
            System.err.println("Error buscar por nombre o correo: " + e.getMessage());
        }
        return null;
    }

    // ── LISTAR TODOS ──────────────────────────────────────────────────────
    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM USUARIOS ORDER BY fecha_registro DESC";

        try (Connection conn = getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error listar: " + e.getMessage());
        }
        return usuarios;
    }

    // ── MAPEO ─────────────────────────────────────────────────────────────
    private Usuario mapearUsuario(ResultSet rs) throws SQLException {
        Usuario usuario = new Usuario();
        usuario.setNumeroIdentificacion(rs.getString("numero_identificacion"));
        usuario.setTipoIdentificacion(rs.getString("tipo_identificacion"));
        usuario.setNombre(rs.getString("nombre"));
        usuario.setApellidos(rs.getString("apellidos"));
        usuario.setTelefono(rs.getString("telefono"));
        usuario.setCorreo(rs.getString("correo"));
        usuario.setContrasenaHash(rs.getString("contrasena_hash"));
        usuario.setFotoUrl(rs.getString("foto_url"));
        usuario.setEstado(rs.getString("estado"));

        Date fecha = rs.getDate("fecha_registro");
        if (fecha != null) {
            usuario.setFechaRegistro(fecha.toLocalDate());
        }

        usuario.setTipoUsuario(rs.getString("tipo_usuario"));
        return usuario;
    }
}