package org.gymbrot.service;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.UsuarioDAO;
import org.gymbrot.model.Cliente;
import org.gymbrot.model.Usuario;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
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
        String sql = "{call PKG_GYMBROT.SP_REGISTRAR_CLIENTE(?,?,?,?,?,?,?,?,?,?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, usuario.getNumeroIdentificacion());
            cs.setString(2, usuario.getTipoIdentificacion() != null ? usuario.getTipoIdentificacion().toUpperCase() : null);
            cs.setString(3, usuario.getNombre());
            cs.setString(4, usuario.getApellidos());
            cs.setString(5, usuario.getTelefono());
            cs.setString(6, usuario.getCorreo());
            cs.setString(7, usuario.getContrasenaHash());
            cs.setString(8, cliente.getDireccion());
            if (cliente.getFechaNacimiento() != null) {
                cs.setDate(9, Date.valueOf(cliente.getFechaNacimiento()));
            } else {
                cs.setNull(9, Types.DATE);
            }
            if (templateHuella != null) {
                cs.setBytes(10, templateHuella);
            } else {
                cs.setNull(10, Types.BLOB);
            }
            cs.registerOutParameter(11, Types.INTEGER);
            cs.registerOutParameter(12, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(11);
            String mensaje = cs.getString(12);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en registrarCliente: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizarCliente(Usuario usuario, Cliente cliente) {
        String sql = "{call PKG_GYMBROT.SP_ACTUALIZAR_CLIENTE(?,?,?,?,?,?,?,?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, usuario.getNumeroIdentificacion());
            cs.setString(2, usuario.getNombre());
            cs.setString(3, usuario.getApellidos());
            cs.setString(4, usuario.getTelefono());
            cs.setString(5, usuario.getCorreo());
            cs.setString(6, cliente.getDireccion());
            if (cliente.getFechaNacimiento() != null) {
                cs.setDate(7, Date.valueOf(cliente.getFechaNacimiento()));
            } else {
                cs.setNull(7, Types.DATE);
            }
            if (cliente.getHuellaDactilar() != null) {
                cs.setBytes(8, cliente.getHuellaDactilar());
            } else {
                cs.setNull(8, Types.BLOB);
            }
            cs.registerOutParameter(9, Types.INTEGER);
            cs.registerOutParameter(10, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(9);
            String mensaje = cs.getString(10);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en actualizarCliente: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminarCliente(String numeroIdentificacion) {
        try {
            if (!clienteDAO.eliminar(numeroIdentificacion)) {
                System.err.println("Error al eliminar cliente");
                return false;
            }
            if (!usuarioDAO.eliminar(numeroIdentificacion)) {
                System.err.println("Error al eliminar usuario");
                return false;
            }
            System.out.println("Cliente eliminado exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en eliminarCliente: " + e.getMessage());
            return false;
        }
    }

    public Cliente buscarCliente(String numeroIdentificacion) {
        try {
            return clienteDAO.buscarPorId(numeroIdentificacion);
        } catch (Exception e) {
            System.err.println("Error en buscarCliente: " + e.getMessage());
            return null;
        }
    }

    public List<Cliente> listarClientes() {
        try {
            return clienteDAO.listarTodos();
        } catch (Exception e) {
            System.err.println("Error en listarClientes: " + e.getMessage());
            return List.of();
        }
    }

    public boolean cambiarEstado(String numeroIdentificacion, String nuevoEstado) {
        try {
            Usuario usuario = usuarioDAO.buscarPorId(numeroIdentificacion);
            if (usuario == null) {
                System.err.println("Usuario no encontrado");
                return false;
            }
            usuario.setEstado(nuevoEstado.toUpperCase());
            return usuarioDAO.actualizar(usuario);
        } catch (Exception e) {
            System.err.println("Error en cambiarEstado: " + e.getMessage());
            return false;
        }
    }

}
