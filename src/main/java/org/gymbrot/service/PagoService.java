package org.gymbrot.service;

import org.gymbrot.dao.HistorialMembresiaDAO;
import org.gymbrot.dao.MembresiaDAO;
import org.gymbrot.dao.PagoDAO;
import org.gymbrot.model.HistorialMembresia;
import org.gymbrot.model.Membresia;
import org.gymbrot.model.Pago;
import java.time.LocalDate;
import java.util.List;

/**
 * Service para gestión de pagos de membresías.
 * Valida membresías activas y coordina el registro de pagos.
 */
public class PagoService {

    private PagoDAO pagoDAO;
    private MembresiaDAO membresiaDAO;
    private HistorialMembresiaDAO historialDAO;

    public PagoService() {
        this.pagoDAO = new PagoDAO();
        this.membresiaDAO = new MembresiaDAO();
        this.historialDAO = new HistorialMembresiaDAO();
    }

    /**
     * Busca un pago por su ID.
     * @param idPago ID del pago.
     * @return Pago encontrado, o null si no existe.
     */
    public Pago buscarPago(int idPago) {
        if (idPago <= 0) {
            System.err.println("✗ ID de pago inválido.");
            return null;
        }
        return pagoDAO.buscarPorId(idPago);
    }

    /**
     * Lista todos los pagos de un cliente.
     * @param idCliente ID del cliente.
     * @return Lista de pagos, vacía si no hay ninguno.
     */
    public List<Pago> listarPagosPorCliente(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return null;
        }
        return pagoDAO.listarPorCliente(idCliente);
    }
}