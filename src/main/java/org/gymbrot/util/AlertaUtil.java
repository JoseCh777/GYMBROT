package org.gymbrot.util;

import javax.swing.JOptionPane;

public class AlertaUtil {

    // ── MOSTRAR ERROR ─────────────────────────────────────────────────────
    public static void mostrarError(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje, titulo, JOptionPane.ERROR_MESSAGE);
    }

    // ── MOSTRAR EXITO ─────────────────────────────────────────────────────
    public static void mostrarExito(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }
    // ── MOSTRAR CONFIRMACION ──────────────────────────────────────────────
    public static boolean mostrarConfirmacion(String mensaje) {
        int respuesta = JOptionPane.showConfirmDialog(null, mensaje, "Confirmación",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return respuesta == JOptionPane.YES_OPTION;
    }

    // ── MOSTRAR INFO ──────────────────────────────────────────────────────
    public static void mostrarInfo(String titulo, String mensaje) {
        JOptionPane.showMessageDialog(null, mensaje, titulo, JOptionPane.INFORMATION_MESSAGE);
    }
}
