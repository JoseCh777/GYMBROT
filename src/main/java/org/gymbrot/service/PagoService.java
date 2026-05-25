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
    /**
     * Lista todos los pagos asociados a una membresía.
     * @param idMembresia ID de la membresía.
     * @return Lista de pagos, vacía si no hay ninguno.
     */
    public List<Pago> listarPagosPorMembresia(int idMembresia) {
        if (idMembresia <= 0) {
            System.err.println("✗ ID de membresía inválido.");
            return null;
        }
        return pagoDAO.listarPorMembresia(idMembresia);
    }

    /**
     * Registra un nuevo pago para una membresía activa.
     * Valida que la membresía exista, esté activa y el valor sea correcto.
     * @param pago Pago a registrar.
     * @return true si se registró exitosamente.
     */
    public boolean registrarPago(Pago pago) {
        // 1. Validar cliente
        if (pago.getIdCliente() == null || pago.getIdCliente().trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return false;
        }

        // 2. Validar membresía
        if (pago.getIdMembresia() <= 0) {
            System.err.println("✗ ID de membresía inválido.");
            return false;
        }

        // 3. Verificar que la membresía exista y esté activa
        Membresia membresia = membresiaDAO.buscarPorId(pago.getIdMembresia());
        if (membresia == null) {
            System.err.println("✗ No se encontró la membresía con ID: " + pago.getIdMembresia());
            return false;
        }
        if (!membresia.getEstado().equals("ACTIVA")) {
            System.err.println("✗ La membresía no está activa.");
            return false;
        }

        // 4. Validar valor
        if (pago.getValor() <= 0) {
            System.err.println("✗ El valor del pago debe ser mayor a 0.");
            return false;
        }

        // 5. Verificar que el cliente tenga historial activo
        HistorialMembresia historial = historialDAO.buscarActiva(pago.getIdCliente());
        if (historial == null) {
            System.err.println("✗ El cliente no tiene una membresía activa registrada.");
            return false;
        }

        // 6. Establecer fecha y estado
        pago.setFechaPago(LocalDate.now());
        pago.setEstadoPago("EXITOSO");

        // 7. Persistir
        boolean resultado = pagoDAO.insertar(pago);
        if (resultado) {
            System.out.println("✓ Pago registrado exitosamente.");
        }
        return resultado;
    }

}
