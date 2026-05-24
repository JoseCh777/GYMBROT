package org.gymbrot.service;

import org.gymbrot.dao.HistorialMembresiaDAO;
import org.gymbrot.dao.MembresiaDAO;
import org.gymbrot.dao.PagoDAO;
import org.gymbrot.model.HistorialMembresia;
import org.gymbrot.model.Pago;
import org.gymbrot.model.Membresia;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class MembresiaService {

    private MembresiaDAO membresiaDAO;
    private HistorialMembresiaDAO historialDAO;
    private PagoDAO pagoDAO;
    private NotificacionService notifService;

    public MembresiaService() {
        this.membresiaDAO = new MembresiaDAO();
        this.historialDAO = new HistorialMembresiaDAO();
        this.pagoDAO = new PagoDAO();
        this.notifService = new NotificacionService();
    }

    // ── CREAR MEMBRESÍA ───────────────────────────────────────────────────
    public boolean crearMembresia(Membresia membresia, String idCliente) {
        try {
            membresia.setFechaInicio(LocalDate.now());
            membresia.setEstado("ACTIVA");

            LocalDate fechaVencimiento = calcularFechaVencimiento(
                    membresia.getFechaInicio(),
                    membresia.getModalidadPago()
            );
            membresia.setFechaVencimiento(fechaVencimiento);

            int idMembresiaGenerado = membresiaDAO.insertarYRetornarId(membresia);

            if (idMembresiaGenerado == -1) {
                System.err.println("Error al insertar membresía");
                return false;
            }

            HistorialMembresia actual = historialDAO.buscarActiva(idCliente);
            if (actual != null) historialDAO.desactivar(actual.getIdHistorial());

            HistorialMembresia historial = new HistorialMembresia();
            historial.setIdCliente(idCliente);
            historial.setIdMembresia(idMembresiaGenerado);
            historial.setFechaAsignacion(LocalDate.now());
            historial.setActiva(true);

            if (!historialDAO.insertar(historial)) {
                System.err.println("Error al crear historial");
                return false;
            }

            System.out.println("✓ Membresía creada exitosamente - ID: " + idMembresiaGenerado);
            return true;

        } catch (Exception e) {
            System.err.println("Error en crearMembresia: " + e.getMessage());
            return false;
        }
    }

    // ── CALCULAR FECHA VENCIMIENTO ────────────────────────────────────────
    private LocalDate calcularFechaVencimiento(LocalDate fechaInicio, String modalidadPago) {
        switch (modalidadPago.toLowerCase()) {
            case "mensual":   return fechaInicio.plusMonths(1);
            case "semestral": return fechaInicio.plusMonths(6);
            case "anual":     return fechaInicio.plusYears(1);
            default:          return fechaInicio.plusMonths(1);
        }
    }

    // ── RENOVAR MEMBRESÍA ─────────────────────────────────────────────────
    public boolean renovarMembresia(int idMembresia, String idCliente, Pago pago) {
        try {
            Membresia membresiaActual = membresiaDAO.buscarPorId(idMembresia);

            if (membresiaActual == null) {
                System.err.println("Membresía no encontrada");
                return false;
            }

            LocalDate nuevaFechaInicio = LocalDate.now();
            if (membresiaActual.getFechaVencimiento().isAfter(LocalDate.now())) {
                nuevaFechaInicio = membresiaActual.getFechaVencimiento();
            }

            LocalDate nuevaFechaVencimiento = calcularFechaVencimiento(
                    nuevaFechaInicio,
                    membresiaActual.getModalidadPago()
            );

            membresiaActual.setFechaInicio(nuevaFechaInicio);
            membresiaActual.setFechaVencimiento(nuevaFechaVencimiento);
            membresiaActual.setEstado("ACTIVA");

            if (!membresiaDAO.actualizar(membresiaActual)) {
                System.err.println("Error al renovar membresía");
                return false;
            }

            pago.setIdMembresia(idMembresia);
            pago.setIdCliente(idCliente);
            pago.setFechaPago(LocalDate.now());
            pago.setEstadoPago("EXITOSO");

            if (!pagoDAO.insertar(pago)) {
                System.err.println("Advertencia: No se pudo registrar el pago");
            }

            notifService.enviarPorPlantilla(idCliente, 3, null, null);

            System.out.println("✓ Membresía renovada hasta: " + nuevaFechaVencimiento);
            return true;

        } catch (Exception e) {
            System.err.println("Error en renovarMembresia: " + e.getMessage());
            return false;
        }
    }

    // ── VERIFICAR VENCIMIENTOS ────────────────────────────────────────────
    public void verificarVencimientos() {
        try {
            List<Membresia> membresias = membresiaDAO.listarPorEstado("ACTIVA");
            LocalDate hoy = LocalDate.now();

            for (Membresia membresia : membresias) {
                long diasRestantes = ChronoUnit.DAYS.between(hoy, membresia.getFechaVencimiento());
                HistorialMembresia historial = historialDAO.buscarPorMembresia(membresia.getIdMembresia());

                if (historial == null) continue;
                String idCliente = historial.getIdCliente();

                if (diasRestantes == 7) {
                    notifService.enviarPorPlantilla(idCliente, 1, null, null);
                    System.out.println("✓ Notificación enviada - Vence en 7 días");
                } else if (diasRestantes == 1) {
                    notifService.enviarPorPlantilla(idCliente, 2, null, null);
                    System.out.println("✓ Notificación enviada - Vence mañana");
                } else if (diasRestantes < 0) {
                    membresia.setEstado("VENCIDA");
                    membresiaDAO.actualizar(membresia);
                    HistorialMembresia actual = historialDAO.buscarActiva(idCliente);
                    if (actual != null) historialDAO.desactivar(actual.getIdHistorial());
                    System.out.println("✓ Membresía marcada como VENCIDA");
                }
            }

        } catch (Exception e) {
            System.err.println("Error en verificarVencimientos: " + e.getMessage());
        }
    }

    // ── LISTAR VENCIDAS ───────────────────────────────────────────────────
    public List<Membresia> listarVencidas() {
        try {
            return membresiaDAO.listarVencidas();
        } catch (Exception e) {
            System.err.println("Error en listarVencidas: " + e.getMessage());
            return List.of();
        }
    }

    // ── CALCULAR DÍAS RESTANTES ───────────────────────────────────────────
    public int calcularDiasRestantes(int idMembresia) {
        try {
            Membresia membresia = membresiaDAO.buscarPorId(idMembresia);
            if (membresia == null) {
                System.err.println("Membresía no encontrada");
                return -999;
            }
            long dias = ChronoUnit.DAYS.between(LocalDate.now(), membresia.getFechaVencimiento());
            return (int) dias;
        } catch (Exception e) {
            System.err.println("Error en calcularDiasRestantes: " + e.getMessage());
            return -999;
        }
    }

    // ── OBTENER MEMBRESÍA ACTIVA ──────────────────────────────────────────
    public HistorialMembresia obtenerMembresiaActiva(String idCliente) {
        try {
            return historialDAO.buscarActiva(idCliente);
        } catch (Exception e) {
            System.err.println("Error en obtenerMembresiaActiva: " + e.getMessage());
            return null;
        }
    }
}