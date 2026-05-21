package org.gymbrot.service;

import org.gymbrot.dao.RegistroIngresoDAO;
import org.gymbrot.model.*;
import org.gymbrot.service.*;
//import com.digitalpersona.onetouch.DPFPSample;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class RegistroIngresoService {

    private RegistroIngresoDAO ingresoDAO;
    private HuellaService huellaService;
    private MembresiaService membresiaService;

    public RegistroIngresoService() {
        this.ingresoDAO = new RegistroIngresoDAO();
        this.huellaService = new HuellaService();
        this.membresiaService = new MembresiaService();
    }

    /**
     * Registra la entrada de un cliente verificando su huella dactilar.
     *
     * @param sample Muestra de huella capturada por el lector
     * @return true si se registró exitosamente, false si falló la verificación
     */
    public boolean registrarEntradaPorHuella(DPFPSample sample) {
        try {
            // 1. Verificar la huella contra la base de datos
            String idCliente = huellaService.verificar(sample);

            if (idCliente == null) {
                System.err.println("✗ Huella no reconocida");
                return false;
            }

            System.out.println("✓ Cliente identificado: " + idCliente);

            // 2. Verificar que tenga membresía activa
            HistorialMembresia membresiaActiva = membresiaService.obtenerMembresiaActiva(idCliente);

            if (membresiaActiva == null) {
                System.err.println("✗ Cliente sin membresía activa");
                return false;
            }

            // 3. Verificar que la membresía no esté vencida
            int diasRestantes = membresiaService.calcularDiasRestantes(membresiaActiva.getIdMembresia());

            if (diasRestantes < 0) {
                System.err.println("✗ Membresía vencida - Vencimiento: " + Math.abs(diasRestantes) + " días atrás");
                return false;
            }

            // 4. Crear registro de ingreso
            RegistroIngreso ingreso = new RegistroIngreso();
            ingreso.setIdCliente(idCliente);
            ingreso.setFecha(LocalDate.now());
            ingreso.setHoraEntrada(LocalDateTime.now());
            ingreso.setMetodoVerificacion("huella");
            ingreso.setEstadoVerificacion("verificado");

            // 5. Guardar en base de datos
            if (!ingresoDAO.registrarEntrada(ingreso)) {
                System.err.println("Error al registrar entrada en BD");
                return false;
            }

            System.out.println("✓ Entrada registrada - Bienvenido!");
            System.out.println("  Membresía válida por " + diasRestantes + " días más");

            return true;

        } catch (Exception e) {
            System.err.println("Error en registrarEntradaPorHuella: " + e.getMessage());
            return false;
        }
    }
    /**
     * Registra la salida de un cliente del gimnasio.
     *
     * @param idCliente ID del cliente
     * @return true si se registró correctamente
     */
    public boolean registrarSalida(String idCliente) {
        try {
            // 1. Buscar el último ingreso del cliente sin salida registrada
            List<RegistroIngreso> ingresos = ingresoDAO.listarPorCliente(idCliente);

            if (ingresos.isEmpty()) {
                System.err.println("No hay registros de ingreso para este cliente");
                return false;
            }

            // 2. Obtener el más reciente sin hora de salida
            RegistroIngreso ultimoIngreso = null;
            for (RegistroIngreso ingreso : ingresos) {
                if (ingreso.getHoraSalida() == null) {
                    ultimoIngreso = ingreso;
                    break;
                }
            }

            if (ultimoIngreso == null) {
                System.err.println("No hay ingreso pendiente de salida");
                return false;
            }

            // 3. Registrar la salida
            if (!ingresoDAO.registrarSalida(ultimoIngreso.getIdIngreso())) {
                System.err.println("Error al registrar salida");
                return false;
            }

            System.out.println("✓ Salida registrada - Hasta pronto!");
            return true;

        } catch (Exception e) {
            System.err.println("Error en registrarSalida: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lista todos los ingresos de una fecha específica.
     *
     * @param fecha Fecha a consultar
     * @return Lista de registros de ingreso
     */
    public List<RegistroIngreso> listarIngresosDia(LocalDate fecha) {
        try {
            return ingresoDAO.listarPorFecha(fecha);
        } catch (Exception e) {
            System.err.println("Error en listarIngresosDia: " + e.getMessage());
            return List.of();
        }
    }
    /**
     * Cuenta el total de ingresos del mes actual.
     *
     * @return Cantidad de ingresos
     */
    public int contarIngresosMes() {
        try {
            return ingresoDAO.contarIngresosMes();
        } catch (Exception e) {
            System.err.println("Error en contarIngresosMes: " + e.getMessage());
            return 0;
        }
    }

    /**
     * Registra una entrada manual (sin verificación biométrica).
     * Usado cuando el lector de huellas falla o en casos especiales.
     *
     * @param idCliente ID del cliente
     * @param motivoManual Motivo del registro manual
     * @return true si se registró correctamente
     */
    public boolean registrarEntradaManual(String idCliente, String motivoManual) {
        try {
            // 1. Verificar que tenga membresía activa
            HistorialMembresia membresiaActiva = membresiaService.obtenerMembresiaActiva(idCliente);

            if (membresiaActiva == null) {
                System.err.println("✗ Cliente sin membresía activa");
                return false;
            }

            // 2. Verificar que la membresía no esté vencida
            int diasRestantes = membresiaService.calcularDiasRestantes(membresiaActiva.getIdMembresia());

            if (diasRestantes < 0) {
                System.err.println("✗ Membresía vencida");
                return false;
            }

            // 3. Crear registro de ingreso manual
            RegistroIngreso ingreso = new RegistroIngreso();
            ingreso.setIdCliente(idCliente);
            ingreso.setFecha(LocalDate.now());
            ingreso.setHoraEntrada(LocalDateTime.now());
            ingreso.setMetodoVerificacion("manual");
            ingreso.setEstadoVerificacion("manual - " + motivoManual);

            // 4. Guardar en base de datos
            if (!ingresoDAO.registrarEntrada(ingreso)) {
                System.err.println("Error al registrar entrada manual");
                return false;
            }

            System.out.println("✓ Entrada manual registrada");
            System.out.println("  Motivo: " + motivoManual);
            System.out.println("  Membresía válida por " + diasRestantes + " días más");

            return true;

        } catch (Exception e) {
            System.err.println("Error en registrarEntradaManual: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene estadísticas de ingresos de un cliente específico.
     *
     * @param idCliente ID del cliente
     * @return Lista de ingresos históricos del cliente
     */
    public List<RegistroIngreso> obtenerHistorialCliente(String idCliente) {
        try {
            return ingresoDAO.listarPorCliente(idCliente);
        } catch (Exception e) {
            System.err.println("Error en obtenerHistorialCliente: " + e.getMessage());
            return List.of();
        }
    }
}
