package org.gymbrot.service;

import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.dao.UsuarioDAO;
import org.gymbrot.model.Instructor;
import org.gymbrot.model.Usuario;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.List;

public class InstructorService {

    private UsuarioDAO usuarioDAO;
    private InstructorDAO instructorDAO;

    public InstructorService() {
        this.usuarioDAO = new UsuarioDAO();
        this.instructorDAO = new InstructorDAO();
    }

    public boolean registrarInstructor(Usuario usuario, Instructor instructor) {
        String sql = "{call PKG_GYMBROT_PROC.SP_REGISTRAR_INSTRUCTOR(?,?,?,?,?,?,?,?,?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, usuario.getNumeroIdentificacion());
            cs.setString(2, usuario.getTipoIdentificacion() != null ? usuario.getTipoIdentificacion().toUpperCase() : null);
            cs.setString(3, usuario.getNombre());
            cs.setString(4, usuario.getApellidos());
            cs.setString(5, usuario.getTelefono());
            cs.setString(6, usuario.getCorreo());
            cs.setString(7, usuario.getContrasenaHash());
            cs.setInt(8, instructor.getIdEspecialidad());
            cs.setString(9, instructor.getDisponibilidad());
            cs.registerOutParameter(10, Types.INTEGER);
            cs.registerOutParameter(11, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(10);
            String mensaje = cs.getString(11);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en registrarInstructor: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarInstructor(Usuario usuario, Instructor instructor) {
        String sql = "{call PKG_GYMBROT_PROC.SP_ACTUALIZAR_INSTRUCTOR(?,?,?,?,?,?,?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, usuario.getNumeroIdentificacion());
            cs.setString(2, usuario.getNombre());
            cs.setString(3, usuario.getApellidos());
            cs.setString(4, usuario.getTelefono());
            cs.setString(5, usuario.getCorreo());
            cs.setInt(6, instructor.getIdEspecialidad());
            cs.setString(7, instructor.getDisponibilidad());
            cs.registerOutParameter(8, Types.INTEGER);
            cs.registerOutParameter(9, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(8);
            String mensaje = cs.getString(9);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en actualizarInstructor: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarInstructor(String numeroIdentificacion) {
        try {
            if (!instructorDAO.eliminar(numeroIdentificacion)) {
                System.err.println("Error al eliminar instructor");
                return false;
            }
            if (!usuarioDAO.eliminar(numeroIdentificacion)) {
                System.err.println("Error al eliminar usuario");
                return false;
            }
            System.out.println("Instructor eliminado exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en eliminarInstructor: " + e.getMessage());
            return false;
        }
    }

    public List<Instructor> listarInstructores() {
        try {
            return instructorDAO.listarTodos();
        } catch (Exception e) {
            System.err.println("Error en listarInstructores: " + e.getMessage());
            return List.of();
        }
    }

    public List<Instructor> listarDisponibles() {
        try {
            return instructorDAO.listarDisponibles();
        } catch (Exception e) {
            System.err.println("Error en listarDisponibles: " + e.getMessage());
            return List.of();
        }
    }

}
