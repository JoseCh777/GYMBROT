package org.gymbrot.util;

import org.gymbrot.model.Administrador;

public class SessionManager {

    private static Administrador adminActual = null;

    // ── GET ADMIN ACTUAL ──────────────────────────────────────────────────
    public static Administrador getAdminActual() {
        return adminActual;
    }

    // ── SET ADMIN ACTUAL ──────────────────────────────────────────────────
    public static void setAdminActual(Administrador admin) {
        adminActual = admin;
    }
    // ── CERRAR SESION ─────────────────────────────────────────────────────
    public static void cerrarSesion() {
        adminActual = null;
    }

    // ── ESTA AUTENTICADO ──────────────────────────────────────────────────
    public static boolean estaAutenticado() {
        return adminActual != null;
    }
}