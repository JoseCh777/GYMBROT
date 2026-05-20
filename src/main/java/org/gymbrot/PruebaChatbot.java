package org.gymbrot;

import org.gymbrot.service.ChatbotService;
import org.gymbrot.model.SesionGymbrot;

public class PruebaChatbot {
    public static void main(String[] args) {
        ChatbotService chatbot = new ChatbotService();
        
        SesionGymbrot sesion = chatbot.iniciarSesion("123456");
        System.out.println("Sesión iniciada: " + sesion.getIdSesion());
        
        String respuesta = chatbot.procesarMensaje(sesion.getIdSesion(), "Hola, ¿cuáles son los horarios del gimnasio?");
        System.out.println("GymBrot: " + respuesta);
        
        chatbot.cerrarSesion(sesion.getIdSesion());
        System.out.println("Sesión cerrada.");
    }
}