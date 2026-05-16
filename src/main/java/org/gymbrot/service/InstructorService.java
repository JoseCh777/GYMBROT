package org.gymbrot.service;

import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.dao.UsuarioDAO;
import org.gymbrot.model.Instructor;
import org.gymbrot.model.Usuario;

import java.time.LocalDate;
import java.util.List;

public class InstructorService {

    private UsuarioDAO usuarioDAO;
    private InstructorDAO instructorDAO;

    public InstructorService() {
        this.usuarioDAO = new UsuarioDAO();
        this.instructorDAO = new InstructorDAO();
    }

    /**
     * Registra un nuevo instructor en el sistema.
     *
     * @param usuario Datos del usuario
     * @param instructor Datos específicos del instructor
     * @return true si se registró correctamente
     */
    public boolean registrarInstructor(Usuario usuario, Instructor instructor) {
        try {
            // 1. Establecer valores por defecto
            usuario.setFechaRegistro(LocalDate.now());
            usuario.setEstado("activo");
            usuario.setTipoUsuario("instructor");

            // 2. Insertar en tabla USUARIOS
            if (!usuarioDAO.insertar(usuario)) {
                System.err.println("Error al insertar usuario");
                return false;
            }

            // 3. Establecer la misma identificación
            instructor.setNumeroIdentificacion(usuario.getNumeroIdentificacion());

            // 4. Insertar en tabla INSTRUCTORES
            if (!instructorDAO.insertar(instructor)) {
                System.err.println("Error al insertar instructor");
                // Rollback
                usuarioDAO.eliminar(usuario.getNumeroIdentificacion());
                return false;
            }

            System.out.println("✓ Instructor registrado exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en registrarInstructor: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza los datos de un instructor existente.
     *
     * @param usuario Datos del usuario actualizados
     * @param instructor Datos del instructor actualizados
     * @return true si se actualizó correctamente
     */
    public boolean actualizarInstructor(Usuario usuario, Instructor instructor) {
        try {
            // 1. Actualizar tabla USUARIOS
            if (!usuarioDAO.actualizar(usuario)) {
                System.err.println("Error al actualizar usuario");
                return false;
            }

            // 2. Actualizar tabla INSTRUCTORES
            if (!instructorDAO.actualizar(instructor)) {
                System.err.println("Error al actualizar instructor");
                return false;
            }

            System.out.println("✓ Instructor actualizado exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en actualizarInstructor: " + e.getMessage());
            return false;
        }
    }

}