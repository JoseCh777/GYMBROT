package org.gymbrot.util;

public class Validador {

    // ── VALIDAR CORREO ────────────────────────────────────────────────────
    public static boolean validarCorreo(String correo) {
        if (correo == null || correo.trim().isEmpty()) return false;
        return correo.matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");
    }

    // ── VALIDAR TELEFONO ──────────────────────────────────────────────────
    public static boolean validarTelefono(String telefono) {
        if (telefono == null || telefono.trim().isEmpty()) return false;
        return telefono.matches("^[0-9]{7,15}$");
    }
    // ── VALIDAR IDENTIFICACION ────────────────────────────────────────────
    public static boolean validarIdentificacion(String identificacion) {
        if (identificacion == null || identificacion.trim().isEmpty()) return false;
        return identificacion.matches("^[0-9]{6,12}$");
    }

    // ── VALIDAR FECHA ─────────────────────────────────────────────────────
    public static boolean validarFecha(String fecha) {
        if (fecha == null || fecha.trim().isEmpty()) return false;
        return fecha.matches("^\\d{4}-\\d{2}-\\d{2}$");
    }
}