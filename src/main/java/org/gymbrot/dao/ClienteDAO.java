package org.gymbrot.dao;

import org.gymbrot.model.Cliente;
import org.gymbrot.util.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    private Connection conn;

    public ClienteDAO() {
        try {
            this.conn = DatabaseConnection.getInstance();
        } catch (SQLException e) {
            System.err.println("Error conexión: " + e.getMessage());
        }
    }

    public boolean insertar(Cliente cliente) {
        String sql = "INSERT INTO CLIENTES (numero_identificacion, direccion, " +
                "fecha_nacimiento, huella_dactilar) VALUES (?, ?, ?, ?)";

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
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
}