package org.gymbrot.service;

import org.gymbrot.dao.RegistroIngresoDAO;
import org.gymbrot.model.*;
import org.gymbrot.util.DatabaseConnection;
import com.digitalpersona.onetouch.DPFPSample;

import java.sql.*;
import java.time.LocalDate;
import java.util.List;

public class RegistroIngresoService {

    private RegistroIngresoDAO ingresoDAO;
    private HuellaService huellaService;

    public RegistroIngresoService() {
        this.ingresoDAO = new RegistroIngresoDAO();
        this.huellaService = new HuellaService();
    }

    public boolean registrarEntradaPorHuella(DPFPSample sample) {
        try {
            String idCliente = huellaService.verificar(sample);
            if (idCliente == null) {
                System.err.println("Huella no reconocida");
                return false;
            }
            System.out.println("Cliente identificado: " + idCliente);

            String sql = "{call PKG_GYMBROT_PROC.SP_REGISTRAR_INGRESO(?,?,?,?)}";
            try (Connection conn = DatabaseConnection.getInstance();
                 CallableStatement cs = conn.prepareCall(sql)) {

                cs.setString(1, idCliente);
                cs.setString(2, "HUELLA");
                cs.registerOutParameter(3, Types.INTEGER);
                cs.registerOutParameter(4, Types.VARCHAR);
                cs.execute();

                int codigo = cs.getInt(3);
                String mensaje = cs.getString(4);
                System.out.println(mensaje);
                return codigo == 1;
            }

        } catch (SQLException e) {
            System.err.println("Error en registrarEntradaPorHuella: " + e.getMessage());
            return false;
        }
    }

    public boolean registrarSalida(String idCliente) {
        String sql = "{call PKG_GYMBROT_PROC.SP_REGISTRAR_SALIDA(?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, idCliente);
            cs.registerOutParameter(2, Types.INTEGER);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(2);
            String mensaje = cs.getString(3);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en registrarSalida: " + e.getMessage());
            return false;
        }
    }

    public List<RegistroIngreso> listarIngresosDia(LocalDate fecha) {
        try {
            return ingresoDAO.listarPorFecha(fecha);
        } catch (Exception e) {
            System.err.println("Error en listarIngresosDia: " + e.getMessage());
            return List.of();
        }
    }

    public int contarIngresosMes() {
        try {
            return ingresoDAO.contarIngresosMes();
        } catch (Exception e) {
            System.err.println("Error en contarIngresosMes: " + e.getMessage());
            return 0;
        }
    }

    public boolean registrarEntradaManual(String idCliente, String motivoManual) {
        String sql = "{call PKG_GYMBROT_PROC.SP_REGISTRAR_INGRESO(?,?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, idCliente);
            cs.setString(2, "MANUAL");
            cs.registerOutParameter(3, Types.INTEGER);
            cs.registerOutParameter(4, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(3);
            String mensaje = cs.getString(4);
            System.out.println(mensaje);
            System.out.println("Motivo: " + motivoManual);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en registrarEntradaManual: " + e.getMessage());
            return false;
        }
    }

    public List<RegistroIngreso> obtenerHistorialCliente(String idCliente) {
        try {
            return ingresoDAO.listarPorCliente(idCliente);
        } catch (Exception e) {
            System.err.println("Error en obtenerHistorialCliente: " + e.getMessage());
            return List.of();
        }
    }

}
