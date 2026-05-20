package org.gymbrot.service;

import org.gymbrot.dao.AdministradorDAO;
import org.gymbrot.dao.UsuarioDAO;
import org.gymbrot.model.Administrador;
import org.gymbrot.model.Usuario;

import java.util.List;

public class AdministradorService {

    private UsuarioDAO usuarioDAO;
    private AdministradorDAO administradorDAO;

    public AdministradorService() {
        this.usuarioDAO = new UsuarioDAO();
        this.administradorDAO = new AdministradorDAO();
    }

    /**
     * Busca un administrador por su número de identificación.
     *
     * @param numeroIdentificacion ID del administrador
     * @return Administrador encontrado o null
     */
    public Administrador buscarAdministrador(String numeroIdentificacion) {
        try {
            return administradorDAO.buscarPorId(numeroIdentificacion);
        } catch (Exception e) {
            System.err.println("Error en buscarAdministrador: " + e.getMessage());
            return null;
        }
    }

    /**
     * Actualiza los datos de un administrador existente.
     *
     * @param usuario Datos del usuario actualizados
     * @param administrador Datos del administrador actualizados
     * @return true si se actualizó correctamente
     */
    public boolean actualizarAdministrador(Usuario usuario, Administrador administrador) {
        try {
            // 1. Actualizar tabla USUARIOS
            if (!usuarioDAO.actualizar(usuario)) {
                System.err.println("Error al actualizar usuario");
                return false;
            }

            // 2. Actualizar tabla ADMINISTRADORES
            if (!administradorDAO.actualizar(administrador)) {
                System.err.println("Error al actualizar administrador");
                return false;
            }

            System.out.println("✓ Administrador actualizado exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en actualizarAdministrador: " + e.getMessage());
            return false;
        }
    }
}