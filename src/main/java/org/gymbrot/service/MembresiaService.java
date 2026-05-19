package org.gymbrot.service;

import org.gymbrot.dao.HistorialMembresiaDAO;
import org.gymbrot.dao.MembresiaDAO;
import org.gymbrot.dao.PagoDAO;
import org.gymbrot.model.HistorialMembresia;
import org.gymbrot.model.Membresia;
import org.gymbrot.model.Pago;

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
     * @param membresia Datos de la membresía
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

            // 3. Insertar membresía
            if (!membresiaDAO.insertar(membresia)) {
                System.err.println("Error al insertar membresía");
                return false;
            }

            // 4. Desactivar membresía actual del cliente (si existe)
            historialDAO.desactivarActual(idCliente);

            // 5. Crear registro en historial
            HistorialMembresia historial = new HistorialMembresia();
            historial.setIdCliente(idCliente);
            historial.setIdMembresia(membresia.getIdMembresia());
            historial.setFechaAsignacion(LocalDate.now());
            historial.setActiva(true);

            if (!historialDAO.insertar(historial)) {
                System.err.println("Error al crear historial");
                return false;
            }

            System.out.println("✓ Membresía creada exitosamente");
            return true;

        } catch (Exception e) {
            System.err.println("Error en crearMembresia: " + e.getMessage());
            return false;
        }
    }

    /**
     * Calcula la fecha de vencimiento según la modalidad de pago.
     */
    private LocalDate calcularFechaVencimiento(LocalDate inicio, String modalidad) {
        switch (modalidad.toLowerCase()) {
            case "mensual":
                return inicio.plusMonths(1);
            case "semestral":
                return inicio.plusMonths(6);
            case "anual":
                return inicio.plusYears(1);
            default:
                return inicio.plusMonths(1); // Por defecto mensual
        }
    }
}