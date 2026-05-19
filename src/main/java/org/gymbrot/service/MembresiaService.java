package org.gymbrot.service;

import org.gymbrot.dao.HistorialMembresiaDAO;
import org.gymbrot.dao.MembresiaDAO;
import org.gymbrot.dao.PagoDAO;
import org.gymbrot.model.HistorialMembresia;
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
            historialDAO.desactivarActual(idCliente);

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
}