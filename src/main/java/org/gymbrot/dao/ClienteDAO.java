package org.gymbrot.dao;

import org.gymbrot.model.Cliente;
import org.gymbrot.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClienteDAO {

    // ── CONEXIÓN (fresca en cada método, evita ORA-17008) ─────────────────
    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(Cliente cliente) {
        String sql = "INSERT INTO CLIENTES (numero_identificacion, direccion, " +
                "fecha_nacimiento, huella_dactilar) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, cliente.getNumeroIdentificacion());

            if (cliente.getDireccion() != null && !cliente.getDireccion().isEmpty()) {
                pstmt.setString(2, cliente.getDireccion());
            } else {
                pstmt.setNull(2, Types.VARCHAR);
            }

            if (cliente.getFechaNacimiento() != null) {
                pstmt.setDate(3, Date.valueOf(cliente.getFechaNacimiento()));
            } else {
                pstmt.setNull(3, Types.DATE);
            }

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

    // ── ACTUALIZAR ────────────────────────────────────────────────────────
    public boolean actualizar(Cliente cliente) {
        String sql = "UPDATE CLIENTES SET direccion=?, fecha_nacimiento=?, " +
                "huella_dactilar=? WHERE numero_identificacion=?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            if (cliente.getDireccion() != null) {
                pstmt.setString(1, cliente.getDireccion());
            } else {
                pstmt.setNull(1, Types.VARCHAR);
            }

            if (cliente.getFechaNacimiento() != null) {
                pstmt.setDate(2, Date.valueOf(cliente.getFechaNacimiento()));
            } else {
                pstmt.setNull(2, Types.DATE);
            }

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

    // ── DESACTIVAR ────────────────────────────────────────────────────────
    public boolean desactivar(String numeroIdentificacion) {
        String sql = "UPDATE USUARIOS SET estado = 'INACTIVO' WHERE numero_identificacion = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, numeroIdentificacion);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error desactivar cliente: " + e.getMessage());
            return false;
        }
    }

    // ── ELIMINAR ──────────────────────────────────────────────────────────
    public boolean eliminar(String numeroIdentificacion) {
        String sql = "DELETE FROM CLIENTES WHERE numero_identificacion = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, numeroIdentificacion);
            return pstmt.executeUpdate() > 0;

        } catch (SQLException e) {
            System.err.println("Error eliminar cliente: " + e.getMessage());
            return false;
        }
    }

    // ── BUSCAR POR ID ─────────────────────────────────────────────────────
    public Cliente buscarPorId(String numeroIdentificacion) {
        String sql = "SELECT u.numero_identificacion, u.tipo_identificacion, u.nombre, " +
                "u.apellidos, u.telefono, u.correo, u.contrasena_hash, u.foto_url, " +
                "u.estado, u.fecha_registro, u.tipo_usuario, " +
                "c.direccion, c.fecha_nacimiento, c.huella_dactilar " +
                "FROM USUARIOS u " +
                "LEFT JOIN CLIENTES c ON u.numero_identificacion = c.numero_identificacion " +
                "WHERE u.tipo_usuario = 'CLIENTE' " +
                "AND u.numero_identificacion = ?";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, numeroIdentificacion);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapearCliente(rs);
                }
            }

        } catch (SQLException e) {
            System.err.println("Error buscar cliente: " + e.getMessage());
        }
        return null;
    }

    // ── BUSCAR POR ID STRING ──────────────────────────────────────────────
    public Optional<Cliente> buscarPorIdString(String numeroIdentificacion) {
        return Optional.ofNullable(buscarPorId(numeroIdentificacion));
    }

    // ── LISTAR TODOS ──────────────────────────────────────────────────────
    // LEFT JOIN: devuelve todos los usuarios CLIENTE aunque no tengan
    // fila en CLIENTES (c.* será null en ese caso, mapearCliente lo maneja).
    public List<Cliente> listarTodos() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT u.numero_identificacion, u.tipo_identificacion, u.nombre, " +
                "u.apellidos, u.telefono, u.correo, u.contrasena_hash, u.foto_url, " +
                "u.estado, u.fecha_registro, u.tipo_usuario, " +
                "c.direccion, c.fecha_nacimiento, c.huella_dactilar " +
                "FROM USUARIOS u " +
                "LEFT JOIN CLIENTES c ON u.numero_identificacion = c.numero_identificacion " +
                "WHERE u.tipo_usuario = 'CLIENTE' " +
                "ORDER BY u.fecha_registro DESC";

        try (Connection conn = getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error listar clientes: " + e.getMessage());
        }
        return clientes;
    }

    // ── BUSCAR POR ESTADO ─────────────────────────────────────────────────
    // LEFT JOIN: devuelve clientes con ese estado aunque no tengan fila en CLIENTES.
    public List<Cliente> buscarPorEstado(String estado) {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT u.numero_identificacion, u.tipo_identificacion, u.nombre, " +
                "u.apellidos, u.telefono, u.correo, u.contrasena_hash, u.foto_url, " +
                "u.estado, u.fecha_registro, u.tipo_usuario, " +
                "c.direccion, c.fecha_nacimiento, c.huella_dactilar " +
                "FROM USUARIOS u " +
                "LEFT JOIN CLIENTES c ON u.numero_identificacion = c.numero_identificacion " +
                "WHERE u.tipo_usuario = 'CLIENTE' " +
                "AND u.estado = ? " +
                "ORDER BY u.fecha_registro DESC";

        try (Connection conn = getConexion();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, estado);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    clientes.add(mapearCliente(rs));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error buscar por estado: " + e.getMessage());
        }
        return clientes;
    }

    // ── OBTENER TEMPLATES HUELLA ──────────────────────────────────────────
    // Aquí el INNER JOIN sigue siendo correcto: solo interesan clientes
    // que SÍ tienen huella guardada en CLIENTES.
    public List<Cliente> obtenerTemplatesHuella() {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT u.numero_identificacion, u.tipo_identificacion, u.nombre, " +
                "u.apellidos, u.telefono, u.correo, u.contrasena_hash, u.foto_url, " +
                "u.estado, u.fecha_registro, u.tipo_usuario, " +
                "c.direccion, c.fecha_nacimiento, c.huella_dactilar " +
                "FROM USUARIOS u " +
                "INNER JOIN CLIENTES c ON u.numero_identificacion = c.numero_identificacion " +
                "WHERE c.huella_dactilar IS NOT NULL " +
                "AND u.estado = 'ACTIVO'";

        try (Connection conn = getConexion();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                clientes.add(mapearCliente(rs));
            }

        } catch (SQLException e) {
            System.err.println("Error obtener templates: " + e.getMessage());
        }
        return clientes;
    }

    // ── MAPEO ─────────────────────────────────────────────────────────────
    private Cliente mapearCliente(ResultSet rs) throws SQLException {
        Cliente cliente = new Cliente();

        // Datos de Usuario
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

        // Datos específicos de Cliente (pueden ser null si no hay fila en CLIENTES)
        cliente.setDireccion(rs.getString("direccion"));

        Date fechaNac = rs.getDate("fecha_nacimiento");
        if (fechaNac != null) {
            cliente.setFechaNacimiento(fechaNac.toLocalDate());
        }

        byte[] huella = rs.getBytes("huella_dactilar");
        cliente.setHuellaDactilar(huella);

        return cliente;
    }
}
