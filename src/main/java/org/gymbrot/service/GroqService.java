package org.gymbrot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.gymbrot.model.MensajeGymbrot;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Properties;

public class GroqService {

    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = "llama-3.3-70b-versatile";
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();
    private final String apiKey;
    private final ConsultaGymbrotService consultaService = new ConsultaGymbrotService();

    public GroqService() {
        this.apiKey = cargarApiKey();
    }

    // ── CARGAR API KEY ────────────────────────────────────────────────────
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

    // ── ENVIAR MENSAJE SIN HISTORIAL ──────────────────────────────────────
    public String enviarMensaje(String mensajeUsuario) {
        return enviarMensaje(mensajeUsuario, null);
    }

    // ── ENVIAR MENSAJE CON HISTORIAL ──────────────────────────────────────
    // ✅ Cuando ChatbotService ya procesó una acción (agendar, cancelar, etc.)
    // le pasa contextoExtra con [SISTEMA: ...] y NO se consulta la BD de instructores
    // para evitar confusión con horas recién ocupadas
    public String enviarMensaje(String mensajeUsuario, List<MensajeGymbrot> historial) {
        try {
            // ✅ Solo consultar BD si NO hay un contexto de sistema ya procesado
            String contextoDB = "";
            boolean tieneContextoSistema = mensajeUsuario.contains("[SISTEMA:");

            if (!tieneContextoSistema) {
                // Consulta normal — buscar info relevante
                contextoDB = consultaService.buscarInfoRelevante(mensajeUsuario);
            } else {
                // Ya viene con contexto procesado — solo buscar planes si pregunta por eso
                // No buscar instructores para evitar mostrar horas recién ocupadas
                if (mensajeUsuario.toLowerCase().contains("plan")
                        || mensajeUsuario.toLowerCase().contains("precio")
                        || mensajeUsuario.toLowerCase().contains("membresia")) {
                    contextoDB = consultaService.buscarInfoRelevante("planes membresia precio");
                }
            }

            String systemPrompt = "Eres GYMBROT ADMIN, el asistente inteligente del sistema de gestión del gimnasio GYMBROT en Valledupar. " +
                    "Tu función es asistir al ADMINISTRADOR del gimnasio en procesos administrativos, operativos, analíticos y de comunicación. " +
                    "INFORMACION ACTUAL DE LA BASE DE DATOS: " +
                    (contextoDB.isEmpty() ? "No hay informacion especifica en este momento." : contextoDB) +
                    " CAPACIDADES: Gestión de clientes, usuarios, rutinas, ejercicios, citas, membresías, accesos biométricos y estadísticas. " +
                    "AUTOMATIZACIONES: Cuando se cree, modifique o elimine una cita, generar SMS y correo automáticamente. " +
                    "Cuando una membresía esté próxima a vencer, enviar recordatorio 7, 3 y 1 día antes. " +
                    "REGLAS IMPORTANTES: " +
                    "1. NUNCA inventes información. Usa solo datos de la base de datos. " +
                    "2. Solicita datos faltantes antes de ejecutar acciones. " +
                    "3. Antes de eliminar, solicita confirmación. " +
                    "4. Valida duplicados antes de crear registros. " +
                    "5. Valida disponibilidad antes de crear citas. " +
                    "6. Si el mensaje contiene [SISTEMA:], sigue exactamente esas instrucciones. " +
                    "FORMATO DE RESPUESTA: " +
                    "Operación: [Crear/Consultar/Modificar/Eliminar/Estadística/Notificación] " +
                    "Resultado: [Información o acción realizada] " +
                    "Automatizaciones ejecutadas: [SMS enviado / Correo enviado] " +
                    "Estado: [Éxito / Error / Información faltante] " +
                    "Recuerda toda la conversacion anterior. Responde siempre en español de manera clara y profesional.";

            StringBuilder mensajes = new StringBuilder();
            mensajes.append("{\"role\": \"system\", \"content\": \"")
                    .append(systemPrompt.replace("\"", "'").replace("\n", " "))
                    .append("\"}");

            if (historial != null && !historial.isEmpty()) {
                for (MensajeGymbrot msg : historial) {
                    String rol = msg.getRemitente().equals("CLIENTE") ? "user" : "assistant";
                    String contenido = msg.getContenido().replace("\"", "'").replace("\n", " ");
                    mensajes.append(", {\"role\": \"").append(rol)
                            .append("\", \"content\": \"").append(contenido).append("\"}");
                }
            }

            mensajes.append(", {\"role\": \"user\", \"content\": \"")
                    .append(mensajeUsuario.replace("\"", "'").replace("\n", " "))
                    .append("\"}");

            String body = "{\"model\": \"" + MODEL + "\", \"messages\": [" + mensajes + "], \"max_tokens\": 500}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode json = mapper.readTree(response.body());
            JsonNode choices = json.get("choices");
            if (choices == null || choices.size() == 0) return "Sin respuesta del bot.";
            return choices.get(0).get("message").get("content").asText();

        } catch (Exception e) {
            System.err.println("Error al conectar con Groq: " + e.getMessage());
            return "Lo siento, no puedo responder en este momento.";
        }
    }
}