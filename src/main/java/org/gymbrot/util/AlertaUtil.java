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
}
