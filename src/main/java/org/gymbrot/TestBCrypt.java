package org.gymbrot;

import org.mindrot.jbcrypt.BCrypt;

public class TestBCrypt {
    public static void main(String[] args) {
        String password = "123";
        
        // El hash de la base de datos
        String hashBD = "$2a$10$/jmr3jek/gWb1kVOBcl49.L6rFt6Z8ffy97nlGUz3hlkbUwXSF1cW";
        
        System.out.println("Contraseña: " + password);
        System.out.println("Hash BD: " + hashBD);
        
        // Verificar
        boolean isValid = BCrypt.checkpw(password, hashBD);
        System.out.println("¿Contraseña válida? " + isValid);
    }
}