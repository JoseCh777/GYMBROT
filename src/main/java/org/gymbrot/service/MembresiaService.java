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

    /**
     * Crea una nueva membresía y la asigna al cliente.
     *
     * @param membresia Datos de la membresía (sin idMembresia, se genera auto)
     * @param idCliente ID del cliente
     * @return true si se creó correctamente
     */
    public boolean crearMembresia(Membresia membresia, String idCliente) {
        try {
            // 1. Establecer valores por defecto
            membresia.setFechaInicio(LocalDate.now());
            membresia.setEstado("activa");

            // 2. Calcular fecha de vencimiento según modalidad
            LocalDate fechaVencimiento = calcularFechaVencimiento(
                    membresia.getFechaInicio(),
                    membresia.getModalidadPago()
            );
            membresia.setFechaVencimiento(fechaVencimiento);

            // 3. Insertar membresía (idMembresia se genera automáticamente)
            int idMembresiaGenerado = membresiaDAO.insertarYRetornarId(membresia);

            if (idMembresiaGenerado == -1) {
                System.err.println("Error al insertar membresía");
                return false;
            }

            // 4. Desactivar membresía actual del cliente (si existe)
            HistorialMembresia actual = historialDAO.buscarActiva(idCliente);
            if (actual != null) historialDAO.desactivar(actual.getIdHistorial());

            // 5. Crear registro en historial
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

    /**
     * Calcula la fecha de vencimiento según la modalidad de pago.
     */
    private LocalDate calcularFechaVencimiento(LocalDate fechaInicio, String modalidadPago) {
        switch (modalidadPago.toLowerCase()) {
            case "mensual":
                return fechaInicio.plusMonths(1);
            case "semestral":
                return fechaInicio.plusMonths(6);
            case "anual":
                return fechaInicio.plusYears(1);
            default:
                return fechaInicio.plusMonths(1);
        }
    }
    /**
     * Renueva una membresía vencida o próxima a vencer.
     *
     * @param idMembresia ID de la membresía a renovar
     * @param idCliente ID del cliente
     * @param pago Datos del pago de renovación
     * @return true si se renovó correctamente
     */
    public boolean renovarMembresia(int idMembresia, String idCliente, Pago pago) {
        try {
            // 1. Buscar la membresía actual
            Membresia membresiaActual = membresiaDAO.buscarPorId(idMembresia);

            if (membresiaActual == null) {
                System.err.println("Membresía no encontrada");
                return false;
            }

            // 2. Calcular nueva fecha de inicio y vencimiento
            LocalDate nuevaFechaInicio = LocalDate.now();

            // Si aún no ha vencido, partir desde la fecha de vencimiento actual
            if (membresiaActual.getFechaVencimiento().isAfter(LocalDate.now())) {
                nuevaFechaInicio = membresiaActual.getFechaVencimiento();
            }

            LocalDate nuevaFechaVencimiento = calcularFechaVencimiento(
                    nuevaFechaInicio,
                    membresiaActual.getModalidadPago()
            );

            // 3. Actualizar la membresía
            membresiaActual.setFechaInicio(nuevaFechaInicio);
            membresiaActual.setFechaVencimiento(nuevaFechaVencimiento);
            membresiaActual.setEstado("activa");

            if (!membresiaDAO.actualizar(membresiaActual)) {
                System.err.println("Error al renovar membresía");
                return false;
            }

            // 4. Registrar el pago
            pago.setIdMembresia(idMembresia);
            pago.setIdCliente(idCliente);
            pago.setFechaPago(LocalDate.now());
            pago.setEstadoPago("completado");

            if (!pagoDAO.insertar(pago)) {
                System.err.println("Advertencia: No se pudo registrar el pago");
            }

            // 5. Enviar notificación de renovación exitosa
            notifService.enviarPorPlantilla(idCliente, 3, null, null);

            System.out.println("✓ Membresía renovada hasta: " + nuevaFechaVencimiento);
            return true;

        } catch (Exception e) {
            System.err.println("Error en renovarMembresia: " + e.getMessage());
            return false;
        }
    }

    /**
     * Verifica membresías próximas a vencer y envía notificaciones.
     * Debe ejecutarse periódicamente (ej: diariamente).
     */
    public void verificarVencimientos() {
        try {
            List<Membresia> membresias = membresiaDAO.listarPorEstado("activa");
            LocalDate hoy = LocalDate.now();

            for (Membresia membresia : membresias) {
                long diasRestantes = ChronoUnit.DAYS.between(hoy, membresia.getFechaVencimiento());

                // Buscar el cliente asociado a esta membresía
                HistorialMembresia historial = historialDAO.buscarPorMembresia(membresia.getIdMembresia());

                if (historial == null) continue;

                String idCliente = historial.getIdCliente();

                // Notificar 7 días antes
                if (diasRestantes == 7) {
                    notifService.enviarPorPlantilla(idCliente, 1, null, null);
                    System.out.println("✓ Notificación enviada - Vence en 7 días");
                }

                // Notificar 1 día antes
                else if (diasRestantes == 1) {
                    notifService.enviarPorPlantilla(idCliente, 2, null, null);
                    System.out.println("✓ Notificación enviada - Vence mañana");
                }

                // Marcar como vencida
                else if (diasRestantes < 0) {
                    membresia.setEstado("vencida");
                    membresiaDAO.actualizar(membresia);

                    // Desactivar en historial
                    HistorialMembresia actual = historialDAO.buscarActiva(idCliente);
                    if (actual != null) historialDAO.desactivar(actual.getIdHistorial());

                    System.out.println("✓ Membresía marcada como vencida");
                }
            }

        } catch (Exception e) {
            System.err.println("Error en verificarVencimientos: " + e.getMessage());
        }
    }
    /**
     * Lista todas las membresías vencidas.
     *
     * @return Lista de membresías vencidas
     */
    public List<Membresia> listarVencidas() {
        try {
            return membresiaDAO.listarVencidas();
        } catch (Exception e) {
            System.err.println("Error en listarVencidas: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * Calcula los días restantes hasta el vencimiento de una membresía.
     *
     * @param idMembresia ID de la membresía
     * @return Días restantes (negativo si ya venció)
     */
    public int calcularDiasRestantes(int idMembresia) {
        try {
            Membresia membresia = membresiaDAO.buscarPorId(idMembresia);

            if (membresia == null) {
                System.err.println("Membresía no encontrada");
                return -999; // Valor especial para indicar error
            }

            LocalDate hoy = LocalDate.now();
            long dias = ChronoUnit.DAYS.between(hoy, membresia.getFechaVencimiento());

            return (int) dias;

        } catch (Exception e) {
            System.err.println("Error en calcularDiasRestantes: " + e.getMessage());
            return -999;
        }
    }

    /**
     * Obtiene la membresía activa de un cliente.
     *
     * @param idCliente ID del cliente
     * @return HistorialMembresia activo o null si no tiene
     */
    public HistorialMembresia obtenerMembresiaActiva(String idCliente) {
        try {
            return historialDAO.buscarActiva(idCliente);
        } catch (Exception e) {
            System.err.println("Error en obtenerMembresiaActiva: " + e.getMessage());
            return null;
        }
    }
}