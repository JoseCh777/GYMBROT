package org.gymbrot;

import org.gymbrot.service.EmailService;

public class PruebaChatbot {
    public static void main(String[] args) {
        EmailService email = new EmailService();
        boolean enviado = email.enviarCorreo(
            "mariojose2416@gmail.com",
            "Prueba GYMBROT",
            "<h1>Hola desde GYMBROT!</h1><p>Este es un mensaje de prueba del sistema de notificaciones.</p>"
        );
        System.out.println("Correo enviado: " + enviado);
    }
}