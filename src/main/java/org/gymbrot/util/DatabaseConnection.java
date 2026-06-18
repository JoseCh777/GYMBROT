package org.gymbrot.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;

public class DatabaseConnection {

    private static final String URL      = getEnvOrThrow("GYMBROT_DB_URL");
    private static final String USER     = getEnvOrThrow("GYMBROT_DB_USER");
    private static final String PASSWORD = getEnvOrThrow("GYMBROT_DB_PASS");

    private static Connection instance = null;
    private static boolean conexionReportada = false;

    private DatabaseConnection() {}

    private static String getEnvOrThrow(String key) {
        String val = System.getenv(key);
        if (val == null || val.isBlank()) {
            throw new RuntimeException("Variable de entorno faltante: " + key);
        }
        return val;
    }

    public static Connection getInstance() throws SQLException {
        try {
            if (instance == null || instance.isClosed()) {
                Class.forName("oracle.jdbc.OracleDriver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                instance.setAutoCommit(true);
                instance.setNetworkTimeout(Executors.newSingleThreadExecutor(), 10000);
                if (!conexionReportada) {
                    conexionReportada = true;
                    System.out.println("Conexión exitosa a GYMBROT DB");
                }
            }
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver Oracle no encontrado: " + e.getMessage(), e);
        }
        return instance;
    }

    public static void cerrarConexion() {
        try {
            if (instance != null && !instance.isClosed()) {
                instance.close();
                instance = null;
                System.out.println("Conexión cerrada.");
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar: " + e.getMessage());
        }
    }
}