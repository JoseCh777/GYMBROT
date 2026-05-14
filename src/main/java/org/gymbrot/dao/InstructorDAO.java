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
}