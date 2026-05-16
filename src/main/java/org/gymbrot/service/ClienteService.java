package org.gymbrot.service;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.UsuarioDAO;
import org.gymbrot.model.Cliente;
import org.gymbrot.model.Usuario;
import com.digitalpersona.onetouch.DPFPTemplate;

import java.time.LocalDate;
import java.util.List;

public class ClienteService {

    private UsuarioDAO usuarioDAO;
    private ClienteDAO clienteDAO;
    private HuellaService huellaService;

    public ClienteService() {
        this.usuarioDAO = new UsuarioDAO();
        this.clienteDAO = new ClienteDAO();
        this.huellaService = new HuellaService();
    }

    /**
     * Registra un nuevo cliente con sus datos de usuario y huella dactilar.
     *
     * @param usuario Datos del usuario (herencia)
     * @param cliente Datos específicos del cliente
     * @param template Template de huella dactilar (puede ser null)
     * @return true si se registró correctamente
     */
    public boolean registrarCliente(Usuario usuario, Cliente cliente, DPFPTemplate template) {
        try {
            // 1. Establecer valores por defecto
            usuario.setFechaRegistro(LocalDate.now());
            usuario.setEstado("activo");
            usuario.setTipoUsuario("cliente");

            // 2. Insertar en tabla USUARIOS
            if (!usuarioDAO.insertar(usuario)) {
                System.err.println("Error al insertar usuario");
                return false;
            }

            // 3. Establecer la misma identificación para cliente
            cliente.setNumeroIdentificacion(usuario.getNumeroIdentificacion());

            // 4. Guardar huella dactilar si existe
            if (template != null) {
                boolean huellaSaved = huellaService.guardarHuella(
                        cliente.getNumeroIdentificacion(),
                        template
                );
                if (!huellaSaved) {
                    System.err.println("Advertencia: No se pudo guardar la huella");
                }
            }

            // 5. Insertar en tabla CLIENTES
            if (!clienteDAO.insertar(cliente)) {
                System.err.println("Error al insertar cliente");
                // TODO: Rollback - eliminar usuario creado
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

            // 2. Actualizar tabla CLIENTES
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
}
