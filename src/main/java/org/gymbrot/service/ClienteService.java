package org.gymbrot.service;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.UsuarioDAO;
import org.gymbrot.model.Cliente;
import org.gymbrot.model.Usuario;

import java.time.LocalDate;
import java.util.List;

public class ClienteService {

    private UsuarioDAO usuarioDAO;
    private ClienteDAO clienteDAO;

    public ClienteService() {
        this.usuarioDAO = new UsuarioDAO();
        this.clienteDAO = new ClienteDAO();
    }

    public boolean registrarCliente(Usuario usuario, Cliente cliente, byte[] templateHuella) {
        try {
            usuario.setFechaRegistro(LocalDate.now());
            usuario.setEstado("activo");
            usuario.setTipoUsuario("cliente");

            if (!usuarioDAO.insertar(usuario)) {
                System.err.println("Error al insertar usuario");
                return false;
            }

            cliente.setNumeroIdentificacion(usuario.getNumeroIdentificacion());

            if (templateHuella != null) {
                cliente.setHuellaDactilar(templateHuella);
            }

            if (!clienteDAO.insertar(cliente)) {
                System.err.println("Error al insertar cliente");
                usuarioDAO.eliminar(usuario.getNumeroIdentificacion());
                return false;
            }

            System.out.println("✓ Cliente registrado exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en registrarCliente: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza los datos de un cliente existente.
     *
     * @param usuario Datos del usuario actualizados
     * @param cliente Datos del cliente actualizados
     * @return true si se actualizó correctamente
     */
    public boolean actualizarCliente(Usuario usuario, Cliente cliente) {
        try {
            // 1. Actualizar tabla USUARIOS
            if (!usuarioDAO.actualizar(usuario)) {
                System.err.println("Error al actualizar usuario");
                return false;
            }

            if (!clienteDAO.actualizar(cliente)) {
                System.err.println("Error al actualizar cliente");
                return false;
            }

            System.out.println("✓ Cliente actualizado exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en actualizarCliente: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarCliente(String numeroIdentificacion) {
     *
     * @param numeroIdentificacion ID del cliente
     * @return true si se eliminó correctamente
     */
    public boolean eliminarCliente(String numeroIdentificacion) {
        try {
            // 1. Eliminar de tabla CLIENTES
            if (!clienteDAO.eliminar(numeroIdentificacion)) {
                System.err.println("Error al eliminar cliente");
                return false;
            }

            // 2. Eliminar de tabla USUARIOS
            if (!usuarioDAO.eliminar(numeroIdentificacion)) {
                System.err.println("Error al eliminar usuario");
                return false;
            }

            System.out.println("✓ Cliente eliminado exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en eliminarCliente: " + e.getMessage());
            return false;
        }
    }

    /**
     * Busca un cliente por su número de identificación.
     *
     * @param numeroIdentificacion ID del cliente
     * @return Cliente encontrado o null
     */
    public Cliente buscarCliente(String numeroIdentificacion) {
        try {
            return clienteDAO.buscarPorId(numeroIdentificacion);
        } catch (Exception e) {
            System.err.println("Error en buscarCliente: " + e.getMessage());
            return null;
        }
    }

    /**
     * Lista todos los clientes registrados en el sistema.
     *
     * @return Lista de clientes
     */
    public List<Cliente> listarClientes() {
        try {
            return clienteDAO.listarTodos();
        } catch (Exception e) {
            System.err.println("Error en listarClientes: " + e.getMessage());
            return List.of(); // Lista vacía
        }
    }
    /**
     * Cambia el estado de un cliente (activo, inactivo, suspendido).
     *
     * @param numeroIdentificacion ID del cliente
     * @param nuevoEstado Estado a establecer
     * @return true si se cambió correctamente
     */
    public boolean cambiarEstado(String numeroIdentificacion, String nuevoEstado) {
        try {
            // 1. Buscar el usuario actual
            Usuario usuario = usuarioDAO.buscarPorId(numeroIdentificacion);

            if (usuario == null) {
                System.err.println("Usuario no encontrado");
                return false;
            }

            // 2. Validar estado
            if (!nuevoEstado.equals("activo") &&
                    !nuevoEstado.equals("inactivo") &&
                    !nuevoEstado.equals("suspendido")) {
                System.err.println("Estado no válido: " + nuevoEstado);
                return false;
            }

            // 3. Cambiar estado
            usuario.setEstado(nuevoEstado);

            // 4. Actualizar en BD
            if (!usuarioDAO.actualizar(usuario)) {
                System.err.println("Error al cambiar estado");
                return false;
            }

            System.out.println("✓ Estado cambiado a: " + nuevoEstado);
            return true;

        } catch (Exception e) {
            System.err.println("Error en cambiarEstado: " + e.getMessage());
            return false;
        }
    }

}
