package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;

public class GymbroAIController {

    @FXML private VBox sideNav;
    @FXML private Button navDashboard;
    @FXML private Button navClientes;
    @FXML private Button navInstructores;
    @FXML private Button navMembresias;
    @FXML private Button navProgreso;
    @FXML private Button navAI;
    @FXML private ScrollPane scrollChat;
    @FXML private VBox vboxMensajes;
    @FXML private TextField txtMensaje;

    @FXML
    public void initialize() {
        configurarAnimacionesNav();
        setNavActivo(navAI);
    }

    @FXML
    private void handleEnviar() { }

    @FXML private void handleNavDashboard()    { navegarA("/fxml/Dashboard.fxml"); }
    @FXML private void handleNavClientes()     { navegarA("/fxml/GestionClientes.fxml"); }
    @FXML private void handleNavInstructores() { navegarA("/fxml/GestionInstructores.fxml"); }
    @FXML private void handleNavMembresias()   { navegarA("/fxml/GestionMembresias.fxml"); }
    @FXML private void handleNavProgreso()     { navegarA("/fxml/ProgresoFisico.fxml"); }
    @FXML private void handleNavAI()           { }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Seguro que deseas cerrar sesion?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Cerrar sesion");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) navegarA("/fxml/login.fxml");
        });
    }

    private void navegarA(String rutaFxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(rutaFxml));
            Stage stage = (Stage) sideNav.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navClientes, navInstructores, navMembresias, navProgreso};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navAI);
    }

    private void agregarHoverInactivo(Button btn) {
        ScaleTransition g = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition s = new ScaleTransition(Duration.millis(180), btn);
        g.setToX(1.03); g.setToY(1.03);
        s.setToX(1.0); s.setToY(1.0);
        btn.setOnMouseEntered(e -> {
            g.playFromStart();
            btn.setStyle(btn.getStyle()
                    .replace("-fx-background-color: transparent", "-fx-background-color: #1f2226")
                    .replace("-fx-text-fill: #9ca3af", "-fx-text-fill: white"));
        });
        btn.setOnMouseExited(e -> {
            s.playFromStart();
            btn.setStyle(btn.getStyle()
                    .replace("-fx-background-color: #1f2226", "-fx-background-color: transparent")
                    .replace("-fx-text-fill: white", "-fx-text-fill: #9ca3af"));
        });
        btn.setOnMousePressed(e -> {
            ScaleTransition p = new ScaleTransition(Duration.millis(80), btn);
            p.setToX(0.96); p.setToY(0.96); p.play();
        });
        btn.setOnMouseReleased(e -> {
            ScaleTransition r = new ScaleTransition(Duration.millis(80), btn);
            r.setToX(1.0); r.setToY(1.0); r.play();
        });
    }

    private void agregarHoverActivo(Button btn) {
        ScaleTransition g = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition s = new ScaleTransition(Duration.millis(180), btn);
        g.setToX(1.03); g.setToY(1.03);
        s.setToX(1.0); s.setToY(1.0);
        btn.setOnMouseEntered(e -> g.playFromStart());
        btn.setOnMouseExited(e -> s.playFromStart());
        btn.setOnMousePressed(e -> {
            ScaleTransition p = new ScaleTransition(Duration.millis(80), btn);
            p.setToX(0.97); p.setToY(0.97); p.play();
        });
        btn.setOnMouseReleased(e -> {
            ScaleTransition r = new ScaleTransition(Duration.millis(80), btn);
            r.setToX(1.0); r.setToY(1.0); r.play();
        });
    }

    private void setNavActivo(Button activo) {
        Button[] todos = {navDashboard, navClientes, navInstructores, navMembresias, navProgreso, navAI};
        for (Button btn : todos) {
            if (btn == activo) {
                btn.setStyle("-fx-background-color: #D4FF00; -fx-background-radius: 8;" +
                        "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 700;" +
                        "-fx-text-fill: black; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
                agregarHoverActivo(btn);
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;" +
                        "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 500;" +
                        "-fx-text-fill: #9ca3af; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
                agregarHoverInactivo(btn);
            }
        }
    }
}
