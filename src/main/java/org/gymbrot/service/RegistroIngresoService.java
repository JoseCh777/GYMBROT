package org.gymbrot.service;

import org.gymbrot.dao.RegistroIngresoDAO;
import org.gymbrot.model.RegistroIngreso;
import com.digitalpersona.onetouch.DPFPSample;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class IngresoService {

    private RegistroIngresoDAO ingresoDAO;
    private HuellaService huellaService;
    private MembresiaService membresiaService;

    public IngresoService() {
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
}
