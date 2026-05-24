package org.gymbrot;

import org.gymbrot.model.Membresia;
import org.gymbrot.service.MembresiaService;

import java.time.LocalDate;

public class PruebaMembresia {
    public static void main(String[] args) {

        MembresiaService membresiaService = new MembresiaService();

        // ── SUSCRIBIR CLIENTE A PLAN BÁSICO ───────────────────────────────
        Membresia membresia = new Membresia();
        membresia.setIdPlan(1); // Plan Básico
        membresia.setTipoMembresia("INDIVIDUAL");
        membresia.setModalidadPago("MENSUAL");
        membresia.setValor(80000);
        membresia.setFechaInicio(LocalDate.now());
        membresia.setFechaVencimiento(LocalDate.now().plusMonths(1));
        membresia.setEstado("ACTIVA");

        boolean resultado = membresiaService.crearMembresia(membresia, "123456");
        System.out.println("Membresía creada: " + resultado);
    }
}