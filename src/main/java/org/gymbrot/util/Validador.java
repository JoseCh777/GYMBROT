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
}