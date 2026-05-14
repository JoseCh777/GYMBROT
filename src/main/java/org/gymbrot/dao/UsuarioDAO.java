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
}