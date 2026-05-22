package org.gymbrot;

import org.gymbrot.service.ChatbotService;
import org.gymbrot.model.SesionGymbrot;
import java.util.Scanner;

public class PruebaChatbot {
    public static void main(String[] args) {
        ChatbotService chatbot = new ChatbotService();
        Scanner scanner = new Scanner(System.in);

        SesionGymbrot sesion = chatbot.iniciarSesion("123456");
        System.out.println("Sesión iniciada: " + sesion.getIdSesion());
        System.out.println("¡Hola! Soy GymBrot. Escribe 'salir' para terminar.\n");

        while (true) {
            System.out.print("Tú: ");
            
            String mensaje = scanner.nextLine();
            if (mensaje.equalsIgnoreCase("salir")) break;

            String respuesta = chatbot.procesarMensaje(sesion.getIdSesion(), mensaje);
            System.out.println("GymBrot: " + respuesta + "\n");
        }

        chatbot.cerrarSesion(sesion.getIdSesion());
        System.out.println("Sesión cerrada.");
        scanner.close();
    }
}