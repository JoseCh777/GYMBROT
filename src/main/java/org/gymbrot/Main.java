package org.gymbrot;

import java.sql.Connection;

public static void main(String[] args) {
    Connection conn = util.DatabaseConnection.getInstance();
    if (conn != null) {
        System.out.println("¡Conexión OK!");
    }
}