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
            return contrasenaPlana.equals(hash);
        }
    }
    /**
     * Autentica un usuario por correo y contraseña.
     * Verifica estado activo y guarda la sesión si el login es exitoso.
     * @param correo Correo del usuario.
     * @param contrasenaPlana Contraseña en texto plano.
     * @return Usuario autenticado, o null si falla.
     */
    public Usuario login(String correo, String contrasenaPlana) {
        // 1. Validar campos
        if (correo == null || correo.trim().isEmpty()) {
            System.err.println("✗ El correo no puede estar vacío.");
            return null;
        }
        if (contrasenaPlana == null || contrasenaPlana.trim().isEmpty()) {
            System.err.println("✗ La contraseña no puede estar vacía.");
            return null;
        }

        // 2. Buscar usuario por correo
        Usuario usuario = usuarioDAO.buscarPorCorreo(correo.trim());
        if (usuario == null) {
            System.err.println("✗ No se encontró usuario con ese correo.");
            return null;
        }

        // 3. Verificar estado activo
        if (!usuario.getEstado().equals("ACTIVO")) {
            System.err.println("✗ El usuario está " + usuario.getEstado() + ".");
            return null;
        }

        // 4. Verificar contraseña
        if (!validarContrasena(contrasenaPlana, usuario.getContrasenaHash())) {
            System.err.println("✗ Contraseña incorrecta.");
            return null;
        }

        // 5. Si es administrador, guardar en sesión
        if (usuario.getTipoUsuario().equals("administrador")) {
            Administrador admin = administradorDAO.buscarPorCorreo(correo.trim());
            if (admin != null) {
                SessionManager.setAdminActual(admin);
            }
        }

        System.out.println("✓ Login exitoso: " + usuario.getNombre() + " (" + usuario.getTipoUsuario() + ")");
        return usuario;
    }

    /**
     * Cierra la sesión activa del administrador.
     */
    public void cerrarSesion() {
        SessionManager.cerrarSesion();
        System.out.println("✓ Sesión cerrada exitosamente.");
    }

}
