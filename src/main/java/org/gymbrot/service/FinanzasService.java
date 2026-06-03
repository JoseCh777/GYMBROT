package org.gymbrot.service;

import org.gymbrot.util.DatabaseConnection;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class FinanzasService {

    private Connection getConexion() throws SQLException {
        return DatabaseConnection.getInstance();
    }

    public record IngresoMes(String mes, double total) {}
    public record IngresoPlan(String plan, double total) {}
    public record MetodoPago(String metodo, double total, int cantidad) {}
    public record NuevosClientesMes(String mes, int cantidad) {}
    public record PagoVencido(int idPago, String cliente, String plan, double valor, String metodo, String fecha, String estado) {}

    public List<IngresoMes> ingresosPorMes() {
        List<IngresoMes> lista = new ArrayList<>();
        String sql = """
            SELECT TO_CHAR(fecha_pago, 'YYYY-MM') AS mes, SUM(valor) AS total
            FROM PAGOS
            WHERE estado_pago = 'EXITOSO'
              AND fecha_pago >= ADD_MONTHS(SYSDATE, -12)
            GROUP BY TO_CHAR(fecha_pago, 'YYYY-MM')
            ORDER BY mes
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new IngresoMes(rs.getString("mes"), rs.getDouble("total")));
            }
        } catch (SQLException e) {
            System.err.println("Error ingresosPorMes: " + e.getMessage());
        }
        return lista;
    }

    public List<IngresoPlan> ingresosPorPlan() {
        List<IngresoPlan> lista = new ArrayList<>();
        String sql = """
            SELECT pm.nombre AS plan, SUM(p.valor) AS total
            FROM PAGOS p
            JOIN MEMBRESIAS m ON m.id_membresia = p.id_membresia
            JOIN PLANES_MEMBRESIAS pm ON pm.id_plan = m.id_plan
            WHERE p.estado_pago = 'EXITOSO'
            GROUP BY pm.nombre
            ORDER BY total DESC
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new IngresoPlan(rs.getString("plan"), rs.getDouble("total")));
            }
        } catch (SQLException e) {
            System.err.println("Error ingresosPorPlan: " + e.getMessage());
        }
        return lista;
    }

    public List<MetodoPago> desgloseMetodoPago() {
        List<MetodoPago> lista = new ArrayList<>();
        String sql = """
            SELECT metodo_pago, SUM(valor) AS total, COUNT(*) AS cantidad
            FROM PAGOS
            WHERE estado_pago = 'EXITOSO'
            GROUP BY metodo_pago
            ORDER BY total DESC
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new MetodoPago(
                    rs.getString("metodo_pago"),
                    rs.getDouble("total"),
                    rs.getInt("cantidad")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error desgloseMetodoPago: " + e.getMessage());
        }
        return lista;
    }

    public List<NuevosClientesMes> nuevosClientesPorMes() {
        List<NuevosClientesMes> lista = new ArrayList<>();
        String sql = """
            SELECT TO_CHAR(fecha_registro, 'YYYY-MM') AS mes, COUNT(*) AS cantidad
            FROM USUARIOS
            WHERE tipo_usuario = 'CLIENTE'
              AND fecha_registro >= ADD_MONTHS(SYSDATE, -12)
            GROUP BY TO_CHAR(fecha_registro, 'YYYY-MM')
            ORDER BY mes
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new NuevosClientesMes(rs.getString("mes"), rs.getInt("cantidad")));
            }
        } catch (SQLException e) {
            System.err.println("Error nuevosClientesPorMes: " + e.getMessage());
        }
        return lista;
    }

    public List<PagoVencido> pagosVencidos() {
        List<PagoVencido> lista = new ArrayList<>();
        String sql = """
            SELECT p.id_pago, u.nombre || ' ' || u.apellidos AS cliente,
                   pm.nombre AS plan, p.valor, p.metodo_pago,
                   TO_CHAR(p.fecha_pago, 'YYYY-MM-DD') AS fecha, p.estado_pago
            FROM PAGOS p
            JOIN MEMBRESIAS m ON m.id_membresia = p.id_membresia
            JOIN PLANES_MEMBRESIAS pm ON pm.id_plan = m.id_plan
            JOIN USUARIOS u ON u.numero_identificacion = p.id_cliente
            JOIN HISTORIAL_MEMBRESIAS hm ON hm.id_membresia = p.id_membresia
            WHERE hm.activa = 1
              AND hm.fecha_fin < SYSDATE
            ORDER BY p.fecha_pago DESC
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                lista.add(new PagoVencido(
                    rs.getInt("id_pago"),
                    rs.getString("cliente"),
                    rs.getString("plan"),
                    rs.getDouble("valor"),
                    rs.getString("metodo_pago"),
                    rs.getString("fecha"),
                    rs.getString("estado_pago")
                ));
            }
        } catch (SQLException e) {
            System.err.println("Error pagosVencidos: " + e.getMessage());
        }
        return lista;
    }

    public double ingresosMesActual() {
        String sql = """
            SELECT COALESCE(SUM(valor), 0) FROM PAGOS
            WHERE estado_pago = 'EXITOSO'
              AND EXTRACT(MONTH FROM fecha_pago) = EXTRACT(MONTH FROM SYSDATE)
              AND EXTRACT(YEAR FROM fecha_pago) = EXTRACT(YEAR FROM SYSDATE)
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("Error ingresosMesActual: " + e.getMessage());
        }
        return 0;
    }

    public int contarMiembrosActivos() {
        String sql = """
            SELECT COUNT(*) FROM MEMBRESIAS
            WHERE estado = 'ACTIVA' AND fecha_vencimiento >= SYSDATE
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error contarMiembrosActivos: " + e.getMessage());
        }
        return 0;
    }

    public int contarInstructores() {
        String sql = "SELECT COUNT(*) FROM INSTRUCTORES";
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error contarInstructores: " + e.getMessage());
        }
        return 0;
    }

    public int contarCitasHoy() {
        String sql = """
            SELECT COUNT(*) FROM CITAS
            WHERE fecha = SYSDATE AND estado != 'CANCELADA'
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error contarCitasHoy: " + e.getMessage());
        }
        return 0;
    }

    public int contarCitasProximas() {
        String sql = """
            SELECT COUNT(*) FROM CITAS
            WHERE fecha > SYSDATE AND estado = 'PENDIENTE'
            """;
        try (Connection conn = getConexion();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error contarCitasProximas: " + e.getMessage());
        }
        return 0;
    }
}
