package org.gymbrot.util;

import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;

import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class ValidacionUtil {

    public static void soloNumeros(TextField tf) {
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String nuevo = c.getControlNewText();
            if (nuevo.isEmpty()) return c;
            if (!nuevo.matches("\\d*")) return null;
            return c;
        };
        tf.setTextFormatter(new TextFormatter<>(filter));
    }

    public static void soloDecimales(TextField tf) {
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String nuevo = c.getControlNewText();
            if (nuevo.isEmpty()) return c;
            if (nuevo.equals(".")) return c;
            if (!nuevo.matches("\\d*\\.?\\d*")) return null;
            return c;
        };
        tf.setTextFormatter(new TextFormatter<>(filter));
    }

    public static void soloLetras(TextField tf) {
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String nuevo = c.getControlNewText();
            if (nuevo.isEmpty()) return c;
            if (!nuevo.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ\\s]*")) return null;
            return c;
        };
        tf.setTextFormatter(new TextFormatter<>(filter));
    }

    public static void soloLetrasYNumeros(TextField tf) {
        UnaryOperator<TextFormatter.Change> filter = c -> {
            String nuevo = c.getControlNewText();
            if (nuevo.isEmpty()) return c;
            if (!nuevo.matches("[a-zA-ZáéíóúÁÉÍÓÚñÑüÜ0-9\\s]*")) return null;
            return c;
        };
        tf.setTextFormatter(new TextFormatter<>(filter));
    }

    public static void conLongitudMaxima(TextField tf, int max) {
        UnaryOperator<TextFormatter.Change> filter = c -> {
            if (c.getControlNewText().length() > max) return null;
            return c;
        };
        tf.setTextFormatter(new TextFormatter<>(filter));
    }
}
