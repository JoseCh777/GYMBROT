package org.gymbrot.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL      = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    private static final String USER     = "gymbrot";
    private static final String PASSWORD = "gymbrot123";

    private DatabaseConnection() {}

    // Cada llamada devuelve una conexión nueva e independiente.
    // El llamador la cierra con try-with-resources → autoCommit=true por defecto.
    public static Connection getInstance() throws SQLException {
        try {
            Class.forName("oracle.jdbc.OracleDriver");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            conn.setAutoCommit(true); // ← CLAVE: cada operación se persiste sola
            return conn;
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver Oracle no encontrado: " + e.getMessage(), e);
        }
    }

    // Mantenemos el método por compatibilidad pero ya no hace falta llamarlo
    public static void cerrarConexion() {
        // Las conexiones se cierran solas con try-with-resources en cada DAO
    }
}