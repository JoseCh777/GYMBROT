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
    /**
     * Cambia el rol de un administrador.
     *
     * @param numeroIdentificacion ID del administrador
     * @param nuevoRol Rol a asignar (superadmin, operador, etc.)
     * @return true si se cambió correctamente
     */
    public boolean cambiarRol(String numeroIdentificacion, String nuevoRol) {
        try {
            // 1. Validar rol
            String[] rolesPermitidos = {"superadmin", "operador", "recepcionista"};
            boolean rolValido = false;

            for (String rol : rolesPermitidos) {
                if (rol.equalsIgnoreCase(nuevoRol)) {
                    rolValido = true;
                    break;
                }
            }

            if (!rolValido) {
                System.err.println("Rol no válido: " + nuevoRol);
                System.err.println("Roles permitidos: superadmin, operador, recepcionista");
                return false;
            }

            // 2. Buscar el administrador
            Administrador admin = administradorDAO.buscarPorId(numeroIdentificacion);

            if (admin == null) {
                System.err.println("Administrador no encontrado");
                return false;
            }

            // 3. Cambiar rol
            admin.setRol(nuevoRol);

            // 4. Actualizar en BD
            if (!administradorDAO.actualizar(admin)) {
                System.err.println("Error al cambiar rol");
                return false;
            }

            System.out.println("✓ Rol cambiado a: " + nuevoRol);
            return true;

        } catch (Exception e) {
            System.err.println("Error en cambiarRol: " + e.getMessage());
            return false;
        }
    }

    /**
     * Cambia la contraseña de un administrador.
     *
     * @param numeroIdentificacion ID del administrador
     * @param nuevaContrasena Nueva contraseña en texto plano (se hasheará)
     * @return true si se cambió correctamente
     */
    public boolean cambiarContrasena(String numeroIdentificacion, String nuevaContrasena) {
        try {
            // 1. Validar que la contraseña no esté vacía
            if (nuevaContrasena == null || nuevaContrasena.trim().isEmpty()) {
                System.err.println("La contraseña no puede estar vacía");
                return false;
            }

            // 2. Validar longitud mínima
            if (nuevaContrasena.length() < 6) {
                System.err.println("La contraseña debe tener al menos 6 caracteres");
                return false;
            }

            // 3. Buscar el usuario
            Usuario usuario = usuarioDAO.buscarPorId(numeroIdentificacion);

            if (usuario == null) {
                System.err.println("Usuario no encontrado");
                return false;
            }

            // 4. Hashear la nueva contraseña (simulado - se implementará en AuthService)
            String contrasenaHash = hashearContrasena(nuevaContrasena);
            usuario.setContrasenaHash(contrasenaHash);

            // 5. Actualizar en BD
            if (!usuarioDAO.actualizar(usuario)) {
                System.err.println("Error al cambiar contraseña");
                return false;
            }

            System.out.println("✓ Contraseña actualizada exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en cambiarContrasena: " + e.getMessage());
            return false;
        }
    }

    /**
     * Método auxiliar para hashear contraseñas.
     * TODO: Mover a AuthService cuando se implemente.
     */
    private String hashearContrasena(String contrasena) {
        // Por ahora retorna la contraseña simple
        // Se implementará con BCrypt en AuthService
        return "HASH_" + contrasena;
    }
    /**
     * Busca un administrador por su correo electrónico.
     * Usado principalmente para el proceso de login.
     *
     * @param correo Correo del administrador
     * @return Administrador encontrado o null
     */
    public Administrador buscarPorCorreo(String correo) {
        try {
            return administradorDAO.buscarPorCorreo(correo);
        } catch (Exception e) {
            System.err.println("Error en buscarPorCorreo: " + e.getMessage());
            return null;
        }
    }

    /**
     * Valida las credenciales de un administrador.
     * Verifica que el correo exista y la contraseña coincida.
     *
     * @param correo Correo del administrador
     * @param contrasena Contraseña en texto plano
     * @return Administrador si las credenciales son válidas, null si no
     */
    public Administrador validarCredenciales(String correo, String contrasena) {
        try {
            // 1. Buscar administrador por correo
            Administrador admin = administradorDAO.buscarPorCorreo(correo);

            if (admin == null) {
                System.err.println("✗ Correo no registrado");
                return null;
            }

            // 2. Verificar estado del usuario
            if (!"activo".equals(admin.getEstado())) {
                System.err.println("✗ Usuario inactivo o suspendido");
                return null;
            }

            // 3. Verificar contraseña
            String contrasenaHash = hashearContrasena(contrasena);

            if (!contrasenaHash.equals(admin.getContrasenaHash())) {
                System.err.println("✗ Contraseña incorrecta");
                return null;
            }

            System.out.println("✓ Credenciales válidas");
            return admin;

        } catch (Exception e) {
            System.err.println("Error en validarCredenciales: " + e.getMessage());
            return null;
        }
    }

    /**
     * Verifica si un administrador tiene permisos de superadmin.
     *
     * @param numeroIdentificacion ID del administrador
     * @return true si es superadmin
     */
    public boolean esSuperadmin(String numeroIdentificacion) {
        try {
            Administrador admin = administradorDAO.buscarPorId(numeroIdentificacion);

            if (admin == null) {
                return false;
            }

            return "superadmin".equalsIgnoreCase(admin.getRol());

        } catch (Exception e) {
            System.err.println("Error en esSuperadmin: " + e.getMessage());
            return false;
        }
    }
}