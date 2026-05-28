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
    public String enviarMensaje(String mensajeUsuario, List<MensajeGymbrot> historial) {
        try {
            String contextoDB = "";
            boolean tieneContextoSistema = mensajeUsuario.contains("[SISTEMA:");

            if (!tieneContextoSistema) {
                contextoDB = consultaService.buscarInfoRelevante(mensajeUsuario);
            } else {
                if (mensajeUsuario.toLowerCase().contains("plan")
                        || mensajeUsuario.toLowerCase().contains("precio")
                        || mensajeUsuario.toLowerCase().contains("membresia")) {
                    contextoDB = consultaService.buscarInfoRelevante("planes membresia precio");
                }
            }

            String systemPrompt =
                    "Eres GYMBROT ADMIN, el asistente inteligente del sistema de gestión del gimnasio GYMBROT en Valledupar. " +
                            "Tu función es asistir al ADMINISTRADOR del gimnasio en tareas administrativas, operativas, analíticas y de comunicación. " +
                            "INFORMACION ACTUAL DE LA BASE DE DATOS: " +
                            (contextoDB.isEmpty() ? "No hay informacion especifica en este momento." : contextoDB) +
                            " CAPACIDADES DEL ADMINISTRADOR: " +
                            "1. Gestión de clientes: crear, modificar, consultar estado, historial, progreso físico, ingresos y membresías. " +
                            "2. Gestión de citas: crear, modificar, cancelar, validar disponibilidad y detectar conflictos de horario. " +
                            "3. Gestión de membresías: consultar activas, vencidas, por vencer, crear nuevas y actualizar existentes. " +
                            "4. Gestión de rutinas y ejercicios: crear, modificar y recomendar por objetivos del cliente. " +
                            "5. Estadísticas: clientes activos/inactivos, ingresos del mes, asistencia, horarios pico, membresías por vencer, retención. " +
                            "6. Notificaciones: enviar SMS y correos a clientes de forma manual o automática. " +
                            "AUTOMATIZACIONES: " +
                            "- Cita creada/modificada/cancelada → SMS + correo automático con detalles completos. " +
                            "- Membresía por vencer → recordatorio automático 7, 3 y 1 día antes con opciones de renovación. " +
                            "- Cliente nuevo → SMS y correo de bienvenida automático. " +
                            "REGLAS IMPORTANTES: " +
                            "1. NUNCA inventes datos. Usa SOLO la información de la base de datos. " +
                            "2. Si faltan datos para ejecutar una acción, solicítalos antes de proceder. " +
                            "3. Antes de eliminar cualquier registro, pide confirmación explícita. " +
                            "4. Valida duplicados antes de crear clientes o registros. " +
                            "5. Valida disponibilidad del instructor antes de crear citas. " +
                            "6. Si el mensaje contiene [SISTEMA:], esas instrucciones son la VERDAD ABSOLUTA del sistema. " +
                            "IGNORA cualquier conocimiento previo del historial y sigue EXACTAMENTE esas instrucciones. " +
                            "Confírmaselas al administrador sin cuestionarlas, contradecirlas ni modificarlas. " +
                            "El bloque [SISTEMA:] representa el resultado real ejecutado por el backend. " +
                            "FORMATO DE RESPUESTA OBLIGATORIO: " +
                            "Operación: [Crear / Consultar / Modificar / Cancelar / Estadística / Notificación] " +
                            "Resultado: [Descripción clara de lo encontrado o ejecutado] " +
                            "Automatizaciones ejecutadas: [SMS enviado / Correo enviado / Ninguna] " +
                            "Estado: [Éxito / Error / Información faltante] " +
                            "Recuerda toda la conversación anterior. Responde siempre en español de manera clara, directa y profesional.";

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

            String body = "{\"model\": \"" + MODEL + "\", " +
                    "\"temperature\": 0, " +
                    "\"messages\": [" + mensajes + "], " +
                    "\"max_tokens\": 2000}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("=== GROQ RESPONSE ===");
            System.out.println(response.body());
            System.out.println("=====================");

            JsonNode json = mapper.readTree(response.body());
            JsonNode choices = json.get("choices");
            if (choices == null || choices.isEmpty()) return "Sin respuesta del bot.";
            return choices.get(0).get("message").get("content").asText();

        } catch (Exception e) {
            System.err.println("Error al conectar con Groq: " + e.getMessage());
            return "Lo siento, no puedo responder en este momento.";
        }
    }
}