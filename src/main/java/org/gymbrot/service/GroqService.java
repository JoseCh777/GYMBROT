package org.gymbrot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

public class GroqService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;

    public GroqService() {
        this.apiKey = cargarApiKey();
    }

    // ── CARGAR API KEY DESDE config.properties ────────────────────────────
    private String cargarApiKey() {
        try {
            Properties props = new Properties();
            props.load(new FileInputStream("config.properties"));
            return props.getProperty("GROQ_API_KEY");
        } catch (Exception e) {
            System.err.println("Error al cargar config.properties: " + e.getMessage());
            return "";
        }
    }

    // ── ENVIAR MENSAJE AL CHATBOT ─────────────────────────────────────────
    public String enviarMensaje(String mensajeUsuario) {
        try {
            String body = """
                    {
                        "model": "%s",
                        "messages": [
                            {
                                "role": "system",
                                "content": "Eres GymBrot, el asistente virtual del gimnasio GYMBROT en Valledupar. Ayudas a los clientes con información sobre membresías, horarios, rutinas e instructores. Responde siempre en español de manera amable y profesional."
                            },
                            {
                                "role": "user",
                                "content": "%s"
                            }
                        ],
                        "max_tokens": 500
                    }
                    """.formatted(MODEL, mensajeUsuario.replace("\"", "'"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());
           System.out.println("Respuesta Groq: " + response.body());
            JsonNode choices = json.get("choices");
            if (choices == null || choices.size() == 0) return "Sin respuesta del bot.";
         return choices.get(0).get("message").get("content").asText();

        } catch (Exception e) {
            System.err.println("Error al conectar con Groq: " + e.getMessage());
            return "Lo siento, no puedo responder en este momento.";
        }
    }
}