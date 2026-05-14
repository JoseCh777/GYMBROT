package org.gymbrot.dao;

import org.gymbrot.model.Instructor;
import org.gymbrot.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InstructorDAO {

    private Connection conn;

    public InstructorDAO() {
        try {
            this.conn = DatabaseConnection.getInstance();
        } catch (SQLException e) {
            System.err.println("Error conexión: " + e.getMessage());
        }
    }

    public boolean insertar(Instructor instructor) {
        String sql = "INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, " +
                "disponibilidad, fecha_contratacion) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, instructor.getNumeroIdentificacion());
            pstmt.setInt(2, instructor.getIdEspecialidad());
            pstmt.setString(3, instructor.getDisponibilidad());
            pstmt.setDate(4, Date.valueOf(instructor.getFechaContratacion()));

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error insertar instructor: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Instructor instructor) {
        String sql = "UPDATE INSTRUCTORES SET id_especialidad=?, disponibilidad=?, " +
                "fecha_contratacion=? WHERE numero_identificacion=?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, instructor.getIdEspecialidad());
            pstmt.setString(2, instructor.getDisponibilidad());
            pstmt.setDate(3, Date.valueOf(instructor.getFechaContratacion()));
            pstmt.setString(4, instructor.getNumeroIdentificacion());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error actualizar instructor: " + e.getMessage());
            return false;
        }
    }
    public boolean eliminar(String numeroIdentificacion) {
        String sql = "DELETE FROM INSTRUCTORES WHERE numero_identificacion = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroIdentificacion);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error eliminar instructor: " + e.getMessage());
            return false;
        }
    }

    public Instructor buscarPorId(String numeroIdentificacion) {
        String sql = "SELECT i.*, u.tipo_identificacion, u.nombre, u.apellidos, " +
                "u.telefono, u.correo, u.contrasena_hash, u.foto_url, u.estado, " +
                "u.fecha_registro, u.tipo_usuario, e.nombre as nombre_especialidad " +
                "FROM INSTRUCTORES i " +
                "INNER JOIN USUARIOS u ON i.numero_identificacion = u.numero_identificacion " +
                "LEFT JOIN ESPECIALIDADES e ON i.id_especialidad = e.id_especialidad " +
                "WHERE i.numero_identificacion = ?";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroIdentificacion);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapearInstructor(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error buscar instructor: " + e.getMessage());
        }
        return null;
    }

    private Instructor mapearInstructor(ResultSet rs) throws SQLException {
        Instructor instructor = new Instructor();

        // Datos de Usuario (herencia)
        instructor.setNumeroIdentificacion(rs.getString("numero_identificacion"));
        instructor.setTipoIdentificacion(rs.getString("tipo_identificacion"));
        instructor.setNombre(rs.getString("nombre"));
        instructor.setApellidos(rs.getString("apellidos"));
        instructor.setTelefono(rs.getString("telefono"));
        instructor.setCorreo(rs.getString("correo"));
        instructor.setContrasenaHash(rs.getString("contrasena_hash"));
        instructor.setFotoUrl(rs.getString("foto_url"));
        instructor.setEstado(rs.getString("estado"));

        Date fechaReg = rs.getDate("fecha_registro");
        if (fechaReg != null) {
            instructor.setFechaRegistro(fechaReg.toLocalDate());
        }

        instructor.setTipoUsuario(rs.getString("tipo_usuario"));

        // Datos específicos de Instructor
        instructor.setIdEspecialidad(rs.getInt("id_especialidad"));
        instructor.setDisponibilidad(rs.getString("disponibilidad"));

        Date fechaCont = rs.getDate("fecha_contratacion");
        if (fechaCont != null) {
            instructor.setFechaContratacion(fechaCont.toLocalDate());
        }

        return instructor;
    }
}