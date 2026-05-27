package org.gymbrot;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * TitleBarController — Maneja la barra de título personalizada.
 *
 * Llama init(stage) desde Main.java después de cargar el FXML.
 * Para cambiar el título desde cualquier controller:
 *   ((TitleBarController) barLoader.getController()).setTitulo("Clientes");
 */
public class TitleBarController {

    @FXML private HBox   titleBar;
    @FXML private Region dragArea;
    @FXML private Label  lblTitulo;
    @FXML private Button btnMinimizar;
    @FXML private Button btnMaximizar;
    @FXML private Button btnCerrar;

    private Stage stage;

    // Posición del mouse al iniciar el drag
    private double dragOffsetX;
    private double dragOffsetY;

    // ═══════════════════════════════════════════════════════════════════════
    //  INICIALIZACIÓN — llamar desde Main.java
    // ═══════════════════════════════════════════════════════════════════════

    public void init(Stage stage) {
        this.stage = stage;
        configurarDrag();
        configurarHovers();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DRAG — mover la ventana arrastrando la barra
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarDrag() {
        // Toda la barra (incluido el dragArea) permite arrastrar
        titleBar.setOnMousePressed(e -> {
            dragOffsetX = e.getScreenX() - stage.getX();
            dragOffsetY = e.getScreenY() - stage.getY();
        });

        titleBar.setOnMouseDragged(e -> {
            // No arrastrar si está maximizado — primero restaurar
            if (stage.isMaximized()) {
                stage.setMaximized(false);
                dragOffsetX = stage.getWidth() / 2;
                dragOffsetY = 18;
            }
            stage.setX(e.getScreenX() - dragOffsetX);
            stage.setY(e.getScreenY() - dragOffsetY);
        });

        // Doble clic en la barra → maximizar/restaurar
        titleBar.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                handleMaximizar();
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HOVER — cambio de color en los botones de control
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarHovers() {
        // Minimizar: gris al pasar
        btnMinimizar.setOnMouseEntered(e ->
                btnMinimizar.setStyle(btnMinimizar.getStyle()
                        .replace("-fx-background-color: transparent",
                                 "-fx-background-color: #333538")));
        btnMinimizar.setOnMouseExited(e ->
                btnMinimizar.setStyle(btnMinimizar.getStyle()
                        .replace("-fx-background-color: #333538",
                                 "-fx-background-color: transparent")));

        // Maximizar: gris al pasar
        btnMaximizar.setOnMouseEntered(e ->
                btnMaximizar.setStyle(btnMaximizar.getStyle()
                        .replace("-fx-background-color: transparent",
                                 "-fx-background-color: #333538")));
        btnMaximizar.setOnMouseExited(e ->
                btnMaximizar.setStyle(btnMaximizar.getStyle()
                        .replace("-fx-background-color: #333538",
                                 "-fx-background-color: transparent")));

        // Cerrar: rojo al pasar
        btnCerrar.setOnMouseEntered(e ->
                btnCerrar.setStyle(btnCerrar.getStyle()
                        .replace("-fx-background-color: transparent",
                                 "-fx-background-color: #dc2626")
                        .replace("-fx-text-fill: #9ca3af",
                                 "-fx-text-fill: white")));
        btnCerrar.setOnMouseExited(e ->
                btnCerrar.setStyle(btnCerrar.getStyle()
                        .replace("-fx-background-color: #dc2626",
                                 "-fx-background-color: transparent")
                        .replace("-fx-text-fill: white",
                                 "-fx-text-fill: #9ca3af")));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleMinimizar() {
        stage.setIconified(true);
    }

    @FXML
    private void handleMaximizar() {
        boolean max = stage.isMaximized();
        stage.setMaximized(!max);
        // Cambiar ícono del botón según estado
        btnMaximizar.setText(max ? "▢" : "❐");
    }

    @FXML
    private void handleCerrar() {
        stage.close();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  API PÚBLICA — cambiar el título desde otros controllers
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Actualiza el subtítulo que aparece junto al logo en la barra.
     * Llama esto desde cada controller al navegar entre vistas.
     * Ejemplo: titleBarCtrl.setTitulo("Clientes");
     */
    public void setTitulo(String titulo) {
        if (lblTitulo != null) lblTitulo.setText(titulo);
    }
}
