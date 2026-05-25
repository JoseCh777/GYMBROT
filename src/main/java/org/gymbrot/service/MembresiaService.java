package org.gymbrot.service;

import org.gymbrot.dao.HistorialMembresiaDAO;
import org.gymbrot.dao.MembresiaDAO;
import org.gymbrot.dao.PagoDAO;
import org.gymbrot.model.HistorialMembresia;
import org.gymbrot.model.Membresia;
import org.gymbrot.model.Pago;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
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

    public boolean crearMembresia(Membresia membresia, String idCliente) {
        String sql = "{call PKG_GYMBROT.SP_RENOVAR_MEMBRESIA(?,?,?,?,?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, idCliente);
            cs.setInt(2, membresia.getIdPlan());
            cs.setString(3, membresia.getModalidadPago() != null ? membresia.getModalidadPago().toUpperCase() : "MENSUAL");
            cs.setString(4, "EFECTIVO");
            cs.setDouble(5, membresia.getValor());
            cs.registerOutParameter(6, Types.INTEGER);
            cs.registerOutParameter(7, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(6);
            String mensaje = cs.getString(7);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en crearMembresia: " + e.getMessage());
            return false;
        }
    }

    public boolean renovarMembresia(int idMembresia, String idCliente, Pago pago) {
        try {
            Membresia membresiaActual = membresiaDAO.buscarPorId(idMembresia);
            if (membresiaActual == null) {
                System.err.println("Membresia no encontrada");
                return false;
            }

            LocalDate nuevaFechaInicio = LocalDate.now();
            if (membresiaActual.getFechaVencimiento().isAfter(LocalDate.now())) {
                nuevaFechaInicio = membresiaActual.getFechaVencimiento();
            }

            LocalDate nuevaFechaVencimiento = calcularFechaVencimiento(nuevaFechaInicio, membresiaActual.getModalidadPago());

            membresiaActual.setFechaInicio(nuevaFechaInicio);
            membresiaActual.setFechaVencimiento(nuevaFechaVencimiento);
            membresiaActual.setEstado("ACTIVA");

            if (!membresiaDAO.actualizar(membresiaActual)) {
                System.err.println("Error al renovar membresia");
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
            System.out.println("Membresia renovada hasta: " + nuevaFechaVencimiento);
            return true;

        } catch (Exception e) {
            System.err.println("Error en renovarMembresia: " + e.getMessage());
            return false;
        }
    }

    private LocalDate calcularFechaVencimiento(LocalDate fechaInicio, String modalidadPago) {
        if (modalidadPago == null) return fechaInicio.plusMonths(1);
        return switch (modalidadPago.toUpperCase()) {
            case "MENSUAL" -> fechaInicio.plusMonths(1);
            case "SEMESTRAL" -> fechaInicio.plusMonths(6);
            case "ANUAL" -> fechaInicio.plusYears(1);
            default -> fechaInicio.plusMonths(1);
        };
    }

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
                } else if (diasRestantes == 1) {
                    notifService.enviarPorPlantilla(idCliente, 2, null, null);
                } else if (diasRestantes < 0) {
                    membresia.setEstado("VENCIDA");
                    membresiaDAO.actualizar(membresia);
                    HistorialMembresia actual = historialDAO.buscarActiva(idCliente);
                    if (actual != null) historialDAO.desactivar(actual.getIdHistorial());
                }
            }
        } catch (Exception e) {
            System.err.println("Error en verificarVencimientos: " + e.getMessage());
        }
    }

    public List<Membresia> listarVencidas() {
        try {
            return membresiaDAO.listarVencidas();
        } catch (Exception e) {
            return List.of();
        }
    }

    public HistorialMembresia obtenerMembresiaActiva(String idCliente) {
        try {
            return historialDAO.buscarActiva(idCliente);
        } catch (Exception e) {
            return null;
        }
    }

}
