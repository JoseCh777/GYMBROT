package org.gymbrot.service;

import org.gymbrot.dao.*;
import org.gymbrot.model.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MembresiaScheduler {

    private static final Logger LOGGER = Logger.getLogger(MembresiaScheduler.class.getName());
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final MembresiaDAO          membresiaDAO  = new MembresiaDAO();
    private final HistorialMembresiaDAO historialDAO  = new HistorialMembresiaDAO();
    private final ClienteDAO            clienteDAO    = new ClienteDAO();
    private final NotificacionService   notifService  = new NotificacionService();
    private final EmailService          emailService  = new EmailService();

    // ── INICIAR SCHEDULER ─────────────────────────────────────────────────
    public void iniciar() {
        // MODO PRUEBA: ejecuta en 10 segundos
        scheduler.scheduleAtFixedRate(
                this::verificarMembresias,
                10,
                TimeUnit.DAYS.toSeconds(1),
                TimeUnit.SECONDS
        );
        LOGGER.info("MembresiaScheduler iniciado en modo prueba - ejecuta en 10 segundos.");
    }

    // ── DETENER SCHEDULER ─────────────────────────────────────────────────
    public void detener() {
        scheduler.shutdown();
        LOGGER.info("MembresiaScheduler detenido.");
    }

    // ── VERIFICAR MEMBRESÍAS ──────────────────────────────────────────────
    private void verificarMembresias() {
        LOGGER.info("Ejecutando chequeo de membresias: " + LocalDate.now());
        try {
            LocalDate hoy = LocalDate.now();
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            List<Membresia> activas = membresiaDAO.listarPorEstado("ACTIVA");

            for (Membresia m : activas) {
                HistorialMembresia h = historialDAO.buscarPorMembresia(m.getIdMembresia());
                if (h == null) continue;

                Cliente cliente = clienteDAO.buscarPorId(h.getIdCliente());
                if (cliente == null) continue;

                long diasRestantes = java.time.temporal.ChronoUnit.DAYS
                        .between(hoy, m.getFechaVencimiento());

                if (diasRestantes == 7 || diasRestantes == 3 || diasRestantes == 1) {
                    String nombreLimpio = cliente.getNombre().split(" ")[0];
                    String fechaFmt    = m.getFechaVencimiento().format(fmt);
                    String diasTexto   = diasRestantes == 1 ? "1 dia" : diasRestantes + " dias";

                    LOGGER.info("Enviando recordatorio a " + cliente.getNombre() +
                            " — membresia vence en " + diasTexto);

                    // SMS
                    notifService.enviarSmsDirecto(
                            "GYMBROT: Hola " + nombreLimpio + ", tu membresia " +
                                    m.getTipoMembresia() + " vence el " + fechaFmt +
                                    " (en " + diasTexto + "). Renovela para seguir entrenando.");

                    // Correo
                    if (cliente.getCorreo() != null) {
                        emailService.enviarCorreo(
                                cliente.getCorreo(),
                                "Tu membresia vence pronto - GYMBROT",
                                "<html><body style='font-family:Arial;'>" +
                                        "<div style='max-width:600px;margin:0 auto;border:1px solid #ddd;border-radius:10px;'>" +
                                        "<div style='background:#ffaa00;color:white;padding:20px;text-align:center;border-radius:8px 8px 0 0;'>" +
                                        "<h2 style='margin:0;'>GYMBROT</h2></div>" +
                                        "<div style='padding:20px;'>" +
                                        "<p>Hola <b>" + nombreLimpio + "</b>,</p>" +
                                        "<p>Tu membresia <b>" + m.getTipoMembresia() +
                                        "</b> vence el <b>" + fechaFmt +
                                        "</b> (en " + diasTexto + ").</p>" +
                                        "<p>Renovela para seguir disfrutando de todos los servicios de GYMBROT.</p>" +
                                        "<p>Contactanos para mas informacion.</p>" +
                                        "<p><b>GYMBROT Valledupar</b></p></div>" +
                                        "<div style='background:#f4f4f4;padding:10px;text-align:center;font-size:12px;border-radius:0 0 8px 8px;'>" +
                                        "<p style='margin:0;'>© 2026 GYMBROT Valledupar</p></div></div></body></html>");
                    }
                }

                // Marcar como VENCIDA si ya expiro
                if (diasRestantes < 0) {
                    m.setEstado("VENCIDA");
                    membresiaDAO.actualizar(m);
                    LOGGER.info("Membresia #" + m.getIdMembresia() + " marcada como VENCIDA.");
                }
            }

        } catch (Exception e) {
            LOGGER.severe("Error en verificarMembresias: " + e.getMessage());
        }
    }

    // ── CALCULAR DELAY HASTA LAS 8AM ──────────────────────────────────────
    private long calcularDelayHasta8am() {
        LocalTime ahora  = LocalTime.now();
        LocalTime target = LocalTime.of(8, 0);
        long segundos;
        if (ahora.isBefore(target)) {
            segundos = ahora.until(target, java.time.temporal.ChronoUnit.SECONDS);
        } else {
            segundos = ahora.until(target, java.time.temporal.ChronoUnit.SECONDS) + 86400;
        }
        return segundos;
    }
}