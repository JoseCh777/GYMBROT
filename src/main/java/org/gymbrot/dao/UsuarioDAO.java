package org.gymbrot.dao;

import org.gymbrot.model.Usuario;
import org.gymbrot.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UsuarioDAO {

    private Connection conn;

    public UsuarioDAO() {
        try {
            this.conn = DatabaseConnection.getInstance();
        } catch (SQLException e) {
            System.err.println("Error conexión: " + e.getMessage());
        }
    }

    public boolean insertar(Usuario usuario) {
        String sql = "INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, " +
                "apellidos, telefono, correo, contrasena_hash, foto_url, estado, " +
                "fecha_registro, tipo_usuario) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    public boolean actualizar(Usuario usuario) {
        String sql = "UPDATE USUARIOS SET tipo_identificacion=?, nombre=?, apellidos=?, " +
                "telefono=?, correo=?, contrasena_hash=?, foto_url=?, estado=?, " +
                "tipo_usuario=? WHERE numero_identificacion=?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

    public boolean eliminar(String numeroIdentificacion) {
        String sql = "DELETE FROM USUARIOS WHERE numero_identificacion = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroIdentificacion);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error eliminar: " + e.getMessage());
            return false;
        }
    }

    public Usuario buscarPorId(String numeroIdentificacion) {
        String sql = "SELECT * FROM USUARIOS WHERE numero_identificacion = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroIdentificacion);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapearUsuario(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error buscar por ID: " + e.getMessage());
        }
        return null;
    }

    public Usuario buscarPorCorreo(String correo) {
        String sql = "SELECT * FROM USUARIOS WHERE correo = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, correo);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapearUsuario(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error buscar por correo: " + e.getMessage());
        }
        return null;
    }

    public Usuario buscarPorNombreOCorreo(String login) {
        String sql = "SELECT * FROM USUARIOS WHERE (LOWER(nombre) = LOWER(?) OR LOWER(correo) = LOWER(?)) AND estado = 'ACTIVO'";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, login);
            pstmt.setString(2, login);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapearUsuario(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error buscar por nombre/correo: " + e.getMessage());
        }
        return null;
    }

    public List<Usuario> listarTodos() {
        List<Usuario> usuarios = new ArrayList<>();
        String sql = "SELECT * FROM USUARIOS ORDER BY fecha_registro DESC";

        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                usuarios.add(mapearUsuario(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error listar: " + e.getMessage());
        }
        return usuarios;
    }

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