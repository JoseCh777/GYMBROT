package org.gymbrot;

import org.gymbrot.util.DatabaseConnection;

import java.sql.Connection;

public class Main {

    public static void main(String[] args) {

        try {

            Connection conn = DatabaseConnection.getInstance();

            if (conn != null) {
                System.out.println("¡Conexión OK!");
            }

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}