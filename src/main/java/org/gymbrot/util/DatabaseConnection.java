package util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {

    private static final String URL      = "jdbc:oracle:thin:@localhost:1521/XEPDB1";
    private static final String USER     = "gymbrot";
    private static final String PASSWORD = "gymbrot123";

    private static Connection instance = null;

    private DatabaseConnection() {}

    public static Connection getInstance() {
        try {
            if (instance == null || instance.isClosed()) {
                Class.forName("oracle.jdbc.driver.OracleDriver");
                instance = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Conexión exitosa a GYMBROT DB");
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Driver Oracle no encontrado: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
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