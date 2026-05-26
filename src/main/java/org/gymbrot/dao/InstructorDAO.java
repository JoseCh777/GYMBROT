package org.gymbrot.dao;

import org.gymbrot.model.Instructor;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InstructorDAO {

    private Connection getConexion() {
        try {
            return DatabaseConnection.getInstance();
        } catch (Exception e) {
            System.err.println("Error conexión: " + e.getMessage());
            return null;
        }
    }

    public boolean insertar(Instructor instructor) {

        String sql = "INSERT INTO INSTRUCTORES " +
                "(numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {

            ps.setString(1, instructor.getNumeroIdentificacion());
            ps.setInt(2, instructor.getIdEspecialidad());
            ps.setString(3, instructor.getDisponibilidad());
            ps.setDate(4, Date.valueOf(instructor.getFechaContratacion()));

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println("Error insertar instructor: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Instructor instructor) {

        String sql = "UPDATE INSTRUCTORES SET " +
                "id_especialidad = ?, " +
                "disponibilidad = ?, " +
                "fecha_contratacion = ? " +
                "WHERE numero_identificacion = ?";

        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {

            ps.setInt(1, instructor.getIdEspecialidad());
            ps.setString(2, instructor.getDisponibilidad());
            ps.setDate(3, Date.valueOf(instructor.getFechaContratacion()));
            ps.setString(4, instructor.getNumeroIdentificacion());

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println("Error actualizar instructor: " + e.getMessage());
            return false;
        }
    }

    public boolean desactivar(String numeroIdentificacion) {
        String sql = "UPDATE USUARIOS SET estado = 'INACTIVO' WHERE numero_identificacion = ?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, numeroIdentificacion);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error desactivar instructor: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String numeroIdentificacion) {

        String sql = "DELETE FROM INSTRUCTORES WHERE numero_identificacion = ?";

        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {

            ps.setString(1, numeroIdentificacion);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {

            System.err.println("Error eliminar instructor: " + e.getMessage());
            return false;
        }
    }

    public Instructor buscarPorId(String numeroIdentificacion) {

        String sql = "SELECT i.*, " +
                "u.tipo_identificacion, " +
                "u.nombre, " +
                "u.apellidos, " +
                "u.telefono, " +
                "u.correo, " +
                "u.contrasena_hash, " +
                "u.foto_url, " +
                "u.estado, " +
                "u.fecha_registro, " +
                "u.tipo_usuario " +
                "FROM INSTRUCTORES i " +
                "INNER JOIN USUARIOS u " +
                "ON i.numero_identificacion = u.numero_identificacion " +
                "WHERE i.numero_identificacion = ?";

        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {

            ps.setString(1, numeroIdentificacion);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {
                    return mapearInstructor(rs);
                }
            }

        } catch (SQLException e) {

            System.err.println("Error buscar instructor: " + e.getMessage());
        }

        return null;
    }

    public List<Instructor> listarTodos() {

        List<Instructor> instructores = new ArrayList<>();

        String sql = "SELECT i.*, " +
                "u.tipo_identificacion, " +
                "u.nombre, " +
                "u.apellidos, " +
                "u.telefono, " +
                "u.correo, " +
                "u.contrasena_hash, " +
                "u.foto_url, " +
                "u.estado, " +
                "u.fecha_registro, " +
                "u.tipo_usuario " +
                "FROM INSTRUCTORES i " +
                "INNER JOIN USUARIOS u " +
                "ON i.numero_identificacion = u.numero_identificacion " +
                "WHERE u.estado = 'ACTIVO' " +
                "ORDER BY u.nombre";

        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                instructores.add(mapearInstructor(rs));
            }

        } catch (SQLException e) {

            System.err.println("Error listar instructores: " + e.getMessage());
        }

        return instructores;
    }

    public List<Instructor> listarDisponibles() {

        List<Instructor> instructores = new ArrayList<>();

        String sql = "SELECT i.*, " +
                "u.tipo_identificacion, " +
                "u.nombre, " +
                "u.apellidos, " +
                "u.telefono, " +
                "u.correo, " +
                "u.contrasena_hash, " +
                "u.foto_url, " +
                "u.estado, " +
                "u.fecha_registro, " +
                "u.tipo_usuario " +
                "FROM INSTRUCTORES i " +
                "INNER JOIN USUARIOS u " +
                "ON i.numero_identificacion = u.numero_identificacion " +
                "WHERE u.estado = 'ACTIVO' " +
                "ORDER BY u.nombre";

        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                instructores.add(mapearInstructor(rs));
            }

        } catch (SQLException e) {

            System.err.println("Error listar disponibles: " + e.getMessage());
        }

        return instructores;
    }

    public boolean instructorEspecialidad(int idEspecialidad) {

        String sql = "SELECT COUNT(*) FROM INSTRUCTORES WHERE id_especialidad = ?";

        try (PreparedStatement pstmt = getConexion().prepareStatement(sql)) {

            pstmt.setInt(1, idEspecialidad);

            try (ResultSet rs = pstmt.executeQuery()) {

                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }

        } catch (SQLException e) {

            System.err.println("Error verificar especialidad: " + e.getMessage());
        }

        return false;
    }

    private Instructor mapearInstructor(ResultSet rs) throws SQLException {

        Instructor instructor = new Instructor();

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
        instructor.setIdEspecialidad(rs.getInt("id_especialidad"));
        instructor.setDisponibilidad(rs.getString("disponibilidad"));

        Date fechaCont = rs.getDate("fecha_contratacion");

        if (fechaCont != null) {
            instructor.setFechaContratacion(fechaCont.toLocalDate());
        }

        return instructor;
    }
}