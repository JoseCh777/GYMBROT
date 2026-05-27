package org.gymbrot.dao;

import org.gymbrot.model.Cliente;
import org.gymbrot.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteDAO {

    private Connection getConexion() {
        try {
            return DatabaseConnection.getInstance();
        } catch (SQLException e) {
            System.err.println("Error conexión: " + e.getMessage());
            return null;
        }
    }

    public boolean insertar(Cliente cliente) {
        String sql = "INSERT INTO CLIENTES (numero_identificacion, direccion, " +
                "fecha_nacimiento, huella_dactilar) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = getConexion().prepareStatement(sql)) {
            pstmt.setString(1, cliente.getNumeroIdentificacion());
            pstmt.setString(2, cliente.getDireccion());
            pstmt.setDate(3, Date.valueOf(cliente.getFechaNacimiento()));

            // Manejo de BLOB para huella dactilar
            if (cliente.getHuellaDactilar() != null) {
                pstmt.setBytes(4, cliente.getHuellaDactilar());
            } else {
                pstmt.setNull(4, Types.BLOB);
            }

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error insertar cliente: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Cliente cliente) {
        String sql = "UPDATE CLIENTES SET direccion=?, fecha_nacimiento=?, " +
                "huella_dactilar=? WHERE numero_identificacion=?";

        try (PreparedStatement pstmt = getConexion().prepareStatement(sql)) {
            pstmt.setString(1, cliente.getDireccion());
            pstmt.setDate(2, Date.valueOf(cliente.getFechaNacimiento()));

            if (cliente.getHuellaDactilar() != null) {
                pstmt.setBytes(3, cliente.getHuellaDactilar());
            } else {
                pstmt.setNull(3, Types.BLOB);
            }

            pstmt.setString(4, cliente.getNumeroIdentificacion());

            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error actualizar cliente: " + e.getMessage());
            return false;
        }
    }
    public boolean desactivar(String numeroIdentificacion) {
        String sql = "UPDATE USUARIOS SET estado = 'INACTIVO' WHERE numero_identificacion = ?";

        try (PreparedStatement pstmt = getConexion().prepareStatement(sql)) {
            pstmt.setString(1, numeroIdentificacion);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error desactivar cliente: " + e.getMessage());
            return false;
        }
    }

    public boolean eliminar(String numeroIdentificacion) {
        String sql = "DELETE FROM CLIENTES WHERE numero_identificacion = ?";

        try (PreparedStatement pstmt = getConexion().prepareStatement(sql)) {
            pstmt.setString(1, numeroIdentificacion);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error eliminar cliente: " + e.getMessage());
            return false;
        }
    }

    public Cliente buscarPorId(String numeroIdentificacion) {
        String sql = "SELECT c.*, u.tipo_identificacion, u.nombre, u.apellidos, " +
                "u.telefono, u.correo, u.contrasena_hash, u.foto_url, u.estado, " +
                "u.fecha_registro, u.tipo_usuario " +
                "FROM CLIENTES c " +
                "INNER JOIN USUARIOS u ON c.numero_identificacion = u.numero_identificacion " +
                "WHERE c.numero_identificacion = ?";

        try (PreparedStatement pstmt = getConexion().prepareStatement(sql)) {
            pstmt.setString(1, numeroIdentificacion);
            ResultSet rs = pstmt.executeQuery();

            if (rs.next()) {
                return mapearCliente(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error buscar cliente: " + e.getMessage());
        }
        return null;
    }

    public Optional<Cliente> buscarPorIdString(String numeroIdentificacion) {
        return Optional.ofNullable(buscarPorId(numeroIdentificacion));
    }

    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();

        // Datos de Usuario (herencia)
        cliente.setNumeroIdentificacion(rs.getString("numero_identificacion"));
        cliente.setTipoIdentificacion(rs.getString("tipo_identificacion"));
        cliente.setNombre(rs.getString("nombre"));
        cliente.setApellidos(rs.getString("apellidos"));
        cliente.setTelefono(rs.getString("telefono"));
        cliente.setCorreo(rs.getString("correo"));
        cliente.setContrasenaHash(rs.getString("contrasena_hash"));
        cliente.setFotoUrl(rs.getString("foto_url"));
        cliente.setEstado(rs.getString("estado"));

        Date fechaReg = rs.getDate("fecha_registro");
        if (fechaReg != null) {
            cliente.setFechaRegistro(fechaReg.toLocalDate());
        }

        cliente.setTipoUsuario(rs.getString("tipo_usuario"));

        // Datos específicos de Cliente
        cliente.setDireccion(rs.getString("direccion"));

        Date fechaNac = rs.getDate("fecha_nacimiento");
        if (fechaNac != null) {
            cliente.setFechaNacimiento(fechaNac.toLocalDate());
        }

        // Huella dactilar (BLOB)
        byte[] huella = rs.getBytes("huella_dactilar");
        cliente.setHuellaDactilar(huella);

        return cliente;
    }
    public List<Cliente> listarTodos() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT c.*, u.tipo_identificacion, u.nombre, u.apellidos, " +
                "u.telefono, u.correo, u.contrasena_hash, u.foto_url, u.estado, " +
                "u.fecha_registro, u.tipo_usuario " +
                "FROM CLIENTES c " +
                "INNER JOIN USUARIOS u ON c.numero_identificacion = u.numero_identificacion " +
                "WHERE u.estado = 'ACTIVO' " +
                "ORDER BY u.fecha_registro DESC";

        try (Statement stmt = getConexion().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error listar clientes: " + e.getMessage());
        }
        return clientes;
    }

    public List<Cliente> buscarPorEstado(String estado) {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT c.*, u.tipo_identificacion, u.nombre, u.apellidos, " +
                "u.telefono, u.correo, u.contrasena_hash, u.foto_url, u.estado, " +
                "u.fecha_registro, u.tipo_usuario " +
                "FROM CLIENTES c " +
                "INNER JOIN USUARIOS u ON c.numero_identificacion = u.numero_identificacion " +
                "WHERE u.estado = ? " +
                "ORDER BY u.fecha_registro DESC";

        try (PreparedStatement pstmt = getConexion().prepareStatement(sql)) {
            pstmt.setString(1, estado);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error buscar por estado: " + e.getMessage());
        }
        return clientes;
    }
    public List<Cliente> obtenerTemplatesHuella() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT c.*, u.tipo_identificacion, u.nombre, u.apellidos, " +
                "u.telefono, u.correo, u.contrasena_hash, u.foto_url, u.estado, " +
                "u.fecha_registro, u.tipo_usuario " +
                "FROM CLIENTES c " +
                "INNER JOIN USUARIOS u ON c.numero_identificacion = u.numero_identificacion " +
                "WHERE c.huella_dactilar IS NOT NULL " +
                "AND LOWER(u.estado) = 'activo'";

        try (Statement stmt = getConexion().createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error obtener templates: " + e.getMessage());
        }
        return clientes;
    }
}