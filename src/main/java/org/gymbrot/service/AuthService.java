package org.gymbrot.service;

import org.gymbrot.dao.AdministradorDAO;
import org.gymbrot.dao.UsuarioDAO;
import org.gymbrot.model.Administrador;
import org.gymbrot.model.Usuario;
import org.gymbrot.util.SessionManager;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Service de autenticación.
 * Maneja login, logout y hashing de contraseñas con BCrypt.
 */
public class AuthService {

    private UsuarioDAO usuarioDAO;
    private AdministradorDAO administradorDAO;

    public AuthService() {
        this.usuarioDAO = new UsuarioDAO();
        this.administradorDAO = new AdministradorDAO();
    }

    /**
     * Genera un hash BCrypt a partir de una contraseña en texto plano.
     * @param contrasenaPlana Contraseña sin hashear.
     * @return Hash BCrypt listo para guardar en BD.
     */
    public String hashContrasena(String contrasenaPlana) {
        return BCrypt.hashpw(contrasenaPlana, BCrypt.gensalt());
    }

    /**
     * Verifica si una contraseña en texto plano coincide con un hash BCrypt.
     * @param contrasenaPlana Contraseña ingresada por el usuario.
     * @param hash Hash almacenado en BD.
     * @return true si coinciden.
     */
    public boolean validarContrasena(String contrasenaPlana, String hash) {
        if (contrasenaPlana == null || hash == null) return false;
        try {
            return BCrypt.checkpw(contrasenaPlana, hash);
        } catch (Exception e) {
            System.err.println("✗ Error al validar contraseña: " + e.getMessage());
            return false;
        }
    }

}