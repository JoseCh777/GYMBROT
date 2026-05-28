package org.gymbrot.util;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

import java.util.Optional;

/**
 * AlertaPersonalizada — reemplazo de javafx.scene.control.Alert
 * con el estilo visual del sistema GYMBROT.
 *
 * USO (idéntico a Alert):
 *
 *   // Informacion
 *   AlertaPersonalizada.info("Titulo", "Mensaje informativo.");
 *
 *   // Error
 *   AlertaPersonalizada.error("Error", "Descripcion del error.");
 *
 *   // Exito
 *   AlertaPersonalizada.exito("Guardado", "El registro fue exitoso.");
 *
 *   // Confirmacion (devuelve true si el usuario acepta)
 *   boolean confirmo = AlertaPersonalizada.confirmar("Eliminar", "Seguro que deseas eliminar este cliente?");
 *   if (confirmo) { ... }
 */
public class AlertaPersonalizada {

    // ─── Tipos de alerta ───────────────────────────────────────────────────
    public enum Tipo {
        INFO, EXITO, ERROR, CONFIRMACION
    }

    // ─── Colores del sistema ───────────────────────────────────────────────
    private static final String COLOR_FONDO      = "#1a1d21";
    private static final String COLOR_BORDE      = "#1f2125";
    private static final String COLOR_AMARILLO   = "#D4FF00";
    private static final String COLOR_TEXTO      = "#e2e2e6";
    private static final String COLOR_SUBTEXTO   = "#6b7280";
    private static final String COLOR_ROJO       = "#ffb4ab";
    private static final String COLOR_VERDE      = "#bdf4ff";
    private static final String COLOR_BTN_SEC    = "#282a2d";

    // ═══════════════════════════════════════════════════════════════════════
    //  MÉTODOS PÚBLICOS — API simplificada
    // ═══════════════════════════════════════════════════════════════════════

    /** Alerta de información simple. */
    public static void info(String titulo, String mensaje) {
        mostrar(Tipo.INFO, titulo, mensaje, null);
    }

    /** Alerta de éxito. */
    public static void exito(String titulo, String mensaje) {
        mostrar(Tipo.EXITO, titulo, mensaje, null);
    }

    /** Alerta de error. */
    public static void error(String titulo, String mensaje) {
        mostrar(Tipo.ERROR, titulo, mensaje, null);
    }

    /**
     * Alerta de confirmación.
     * @return true si el usuario hizo clic en Aceptar, false si canceló.
     */
    public static boolean confirmar(String titulo, String mensaje) {
        boolean[] resultado = {false};
        mostrar(Tipo.CONFIRMACION, titulo, mensaje, aceptado -> resultado[0] = aceptado);
        return resultado[0];
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONSTRUCCION INTERNA
    // ═══════════════════════════════════════════════════════════════════════

    private static void mostrar(Tipo tipo, String titulo, String mensaje, java.util.function.Consumer<Boolean> callback) {

        Stage stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.APPLICATION_MODAL);

        // ── Modal ──
        VBox modal = new VBox(0);
        modal.setMaxWidth(440);
        modal.setStyle(
            "-fx-background-color: " + COLOR_FONDO + ";" +
            "-fx-background-radius: 12;" +
            "-fx-border-color: " + COLOR_BORDE + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 12;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.6), 32, 0, 0, 8);"
        );

        // ── Header ──
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new javafx.geometry.Insets(20, 24, 16, 24));
        header.setStyle(
            "-fx-border-color: " + COLOR_BORDE + ";" +
            "-fx-border-width: 0 0 1 0;"
        );

        // Indicador de color lateral
        Rectangle indicador = new Rectangle(4, 24);
        indicador.setArcWidth(4);
        indicador.setArcHeight(4);
        indicador.setStyle("-fx-fill: " + getColorTipo(tipo) + ";");

        // Emoji/icono de tipo
        Label icono = new Label(getIconoTipo(tipo));
        icono.setStyle("-fx-font-size: 20px;");

        // Titulo
        Label lblTitulo = new Label(titulo.toUpperCase());
        lblTitulo.setStyle(
            "-fx-font-family: 'Lexend';" +
            "-fx-font-size: 16px;" +
            "-fx-font-weight: 700;" +
            "-fx-text-fill: " + getTituloColor(tipo) + ";"
        );

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().addAll(indicador, icono, lblTitulo, spacer);
        modal.getChildren().add(header);

        // ── Cuerpo ──
        VBox cuerpo = new VBox(0);
        cuerpo.setPadding(new javafx.geometry.Insets(20, 24, 24, 24));

        Label lblMensaje = new Label(mensaje);
        lblMensaje.setWrapText(true);
        lblMensaje.setMaxWidth(380);
        lblMensaje.setStyle(
            "-fx-font-family: 'Inter';" +
            "-fx-font-size: 14px;" +
            "-fx-text-fill: " + COLOR_TEXTO + ";" +
            "-fx-line-spacing: 4;"
        );
        cuerpo.getChildren().add(lblMensaje);
        modal.getChildren().add(cuerpo);

        // ── Footer con botones ──
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new javafx.geometry.Insets(16, 24, 20, 24));
        footer.setStyle(
            "-fx-border-color: " + COLOR_BORDE + ";" +
            "-fx-border-width: 1 0 0 0;"
        );

        if (tipo == Tipo.CONFIRMACION) {
            // Botón Cancelar
            Button btnCancelar = crearBotonSecundario("CANCELAR");
            btnCancelar.setOnAction(e -> {
                animarCierre(modal, stage);
                if (callback != null) callback.accept(false);
            });

            // Botón Aceptar
            Button btnAceptar = crearBotonPrimario("ACEPTAR");
            btnAceptar.setOnAction(e -> {
                animarCierre(modal, stage);
                if (callback != null) callback.accept(true);
            });

            footer.getChildren().addAll(btnCancelar, btnAceptar);

        } else {
            // Solo botón Entendido
            Button btnOk = crearBotonPrimario("ENTENDIDO");
            btnOk.setOnAction(e -> {
                animarCierre(modal, stage);
                if (callback != null) callback.accept(true);
            });
            footer.getChildren().add(btnOk);
        }

        modal.getChildren().add(footer);

        // ── Escena transparente — el modal determina el tamaño ──
        Scene scene = new Scene(modal);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);

        // ── Animación de entrada ──
        modal.setOpacity(0);
        modal.setScaleX(0.92);
        modal.setScaleY(0.92);

        FadeTransition ft = new FadeTransition(Duration.millis(200), modal);
        ft.setFromValue(0); ft.setToValue(1); ft.play();

        ScaleTransition st = new ScaleTransition(Duration.millis(200), modal);
        st.setFromX(0.92); st.setToX(1.0);
        st.setFromY(0.92); st.setToY(1.0);
        st.play();

        // Bloquear hasta que se cierre (showAndWait ya hace visible el stage)
        stage.showAndWait();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HELPERS
    // ═══════════════════════════════════════════════════════════════════════

    private static void animarCierre(VBox modal, Stage stage) {
        FadeTransition ft = new FadeTransition(Duration.millis(150), modal);
        ft.setFromValue(1); ft.setToValue(0);
        ft.setOnFinished(e -> stage.close());
        ft.play();
    }

    private static Button crearBotonPrimario(String texto) {
        Button btn = new Button(texto);
        btn.setStyle(
            "-fx-background-color: " + COLOR_AMARILLO + ";" +
            "-fx-background-radius: 8;" +
            "-fx-font-family: 'Space Grotesk';" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: 700;" +
            "-fx-text-fill: #121417;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 10 24 10 24;"
        );
        // Hover
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle()
            .replace("-fx-background-color: " + COLOR_AMARILLO, "-fx-background-color: #c8f000")));
        btn.setOnMouseExited(e  -> btn.setStyle(btn.getStyle()
            .replace("-fx-background-color: #c8f000", "-fx-background-color: " + COLOR_AMARILLO)));
        // Press
        btn.setOnMousePressed(e  -> { btn.setScaleX(0.97); btn.setScaleY(0.97); });
        btn.setOnMouseReleased(e -> { btn.setScaleX(1.0);  btn.setScaleY(1.0); });
        return btn;
    }

    private static Button crearBotonSecundario(String texto) {
        Button btn = new Button(texto);
        btn.setStyle(
            "-fx-background-color: " + COLOR_BTN_SEC + ";" +
            "-fx-background-radius: 8;" +
            "-fx-font-family: 'Space Grotesk';" +
            "-fx-font-size: 11px;" +
            "-fx-font-weight: 700;" +
            "-fx-text-fill: " + COLOR_TEXTO + ";" +
            "-fx-border-color: " + COLOR_BORDE + ";" +
            "-fx-border-width: 1;" +
            "-fx-border-radius: 8;" +
            "-fx-cursor: hand;" +
            "-fx-padding: 10 24 10 24;"
        );
        btn.setOnMouseEntered(e -> btn.setStyle(btn.getStyle()
            .replace("-fx-background-color: " + COLOR_BTN_SEC, "-fx-background-color: #333538")));
        btn.setOnMouseExited(e  -> btn.setStyle(btn.getStyle()
            .replace("-fx-background-color: #333538", "-fx-background-color: " + COLOR_BTN_SEC)));
        btn.setOnMousePressed(e  -> { btn.setScaleX(0.97); btn.setScaleY(0.97); });
        btn.setOnMouseReleased(e -> { btn.setScaleX(1.0);  btn.setScaleY(1.0); });
        return btn;
    }

    private static String getColorTipo(Tipo tipo) {
        return switch (tipo) {
            case EXITO        -> COLOR_VERDE;
            case ERROR        -> COLOR_ROJO;
            case CONFIRMACION -> COLOR_AMARILLO;
            default           -> "#6b7280";
        };
    }

    private static String getTituloColor(Tipo tipo) {
        return switch (tipo) {
            case EXITO        -> COLOR_VERDE;
            case ERROR        -> COLOR_ROJO;
            case CONFIRMACION -> COLOR_AMARILLO;
            default           -> COLOR_TEXTO;
        };
    }

    private static String getIconoTipo(Tipo tipo) {
        return switch (tipo) {
            case EXITO        -> "✓";
            case ERROR        -> "✕";
            case CONFIRMACION -> "?";
            default           -> "i";
        };
    }
}
