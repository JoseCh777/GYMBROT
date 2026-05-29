package org.gymbrot.util;

import org.gymbrot.model.Administrador;

public class SessionManager {

    private static Administrador adminActual = null;
    private static String idAdminActual = null;

    // ── GET ADMIN ACTUAL ──────────────────────────────────────────────────
    public static Administrador getAdminActual() {
        return adminActual;
    }

    // ── SET ADMIN ACTUAL ──────────────────────────────────────────────────
    public static void setAdminActual(Administrador admin) {
        adminActual = admin;
    }

    // ── GET ID ADMIN ACTUAL ───────────────────────────────────────────────
    public static String getIdAdminActual() {
        return idAdminActual;
    }

    // ── SET ID ADMIN ACTUAL ───────────────────────────────────────────────
    public static void setIdAdminActual(String id) {
        idAdminActual = id;
    }

    // ── CERRAR SESION ─────────────────────────────────────────────────────
    public static void cerrarSesion() {
        adminActual = null;
        idAdminActual = null;
    }

    // ── ESTA AUTENTICADO ──────────────────────────────────────────────────
    public static boolean estaAutenticado() {
        return adminActual != null;
    }
}