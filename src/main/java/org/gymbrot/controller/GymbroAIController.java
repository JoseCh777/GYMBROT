package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.gymbrot.model.MensajeGymbrot;
import org.gymbrot.model.SesionGymbrot;
import org.gymbrot.service.ChatbotService;
import org.gymbrot.util.ChatbotSession;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

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

    private final ChatbotService chatbotService = new ChatbotService();
    private SesionGymbrot sesionActual;
    private String idClienteActual = "123456";

    @FXML
    public void initialize() {
        configurarAnimacionesNav();
        setNavActivo(navAI);

        ChatbotSession session = ChatbotSession.getInstance();

        if (!session.tieneSesionActiva()) {
            sesionActual = chatbotService.iniciarSesion(idClienteActual);
            session.setSesionActual(sesionActual);
            session.setIdCliente(idClienteActual);
            System.out.println("Nueva sesión GymBrot: " + sesionActual.getIdSesion());
        } else {
            sesionActual = session.getSesionActual();
            idClienteActual = session.getIdCliente();
            System.out.println("Sesión GymBrot restaurada: " + sesionActual.getIdSesion());
            cargarHistorialEnUI();
        }

        vboxMensajes.heightProperty().addListener((obs, oldVal, newVal) ->
                scrollChat.setVvalue(1.0));
    }

    // ── CARGAR HISTORIAL EN UI ────────────────────────────────────────────
    private void cargarHistorialEnUI() {
        List<MensajeGymbrot> historial = chatbotService.obtenerHistorial(sesionActual.getIdSesion());
        if (historial == null || historial.isEmpty()) return;
        for (MensajeGymbrot msg : historial) {
            if (msg.getRemitente().equals("CLIENTE")) {
                agregarBurbujaUsuario(msg.getContenido());
            } else {
                agregarBurbujaBot(msg.getContenido());
            }
        }
    }

    // ── ENVIAR MENSAJE ────────────────────────────────────────────────────
    @FXML
    private void handleEnviar() {
        String texto = txtMensaje.getText().trim();
        if (texto.isEmpty()) return;

        agregarBurbujaUsuario(texto);
        txtMensaje.clear();

        HBox typing = crearIndicadorEscritura();
        vboxMensajes.getChildren().add(typing);

        new Thread(() -> {
            String respuesta = chatbotService.procesarMensaje(
                    sesionActual.getIdSesion(), texto, idClienteActual);

            Platform.runLater(() -> {
                vboxMensajes.getChildren().remove(typing);
                agregarBurbujaBot(respuesta);
            });
        }).start();
    }

    // ── BURBUJA USUARIO ───────────────────────────────────────────────────
    private void agregarBurbujaUsuario(String texto) {
        HBox hbox = new HBox();
        hbox.setAlignment(Pos.CENTER_RIGHT);
        hbox.setSpacing(12);

        VBox burbuja = new VBox(6);
        burbuja.setMaxWidth(600);
        burbuja.setAlignment(Pos.CENTER_RIGHT);

        VBox contenido = new VBox();
        contenido.setStyle(
                "-fx-background-color: #D4FF00;" +
                        "-fx-background-radius: 12 0 12 12;");
        contenido.setPadding(new Insets(12, 16, 12, 16));

        Label lblTexto = new Label(texto);
        lblTexto.setStyle(
                "-fx-font-family: 'Inter'; -fx-font-size: 14px;" +
                        "-fx-text-fill: #111316;");
        lblTexto.setWrapText(true);
        lblTexto.setMaxWidth(580);
        contenido.getChildren().add(lblTexto);

        Label lblHora = new Label(horaActual());
        lblHora.setStyle(
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px;" +
                        "-fx-text-fill: #4b5563;");

        burbuja.getChildren().addAll(contenido, lblHora);
        hbox.getChildren().add(burbuja);
        vboxMensajes.getChildren().add(hbox);
    }

    // ── BURBUJA BOT ───────────────────────────────────────────────────────
    private void agregarBurbujaBot(String texto) {
        HBox hbox = new HBox(12);
        hbox.setAlignment(Pos.TOP_LEFT);

        HBox avatar = new HBox();
        avatar.setAlignment(Pos.CENTER);
        avatar.setPrefWidth(36);
        avatar.setPrefHeight(36);
        avatar.setStyle(
                "-fx-background-color: #1e2a00;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #D4FF00;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 18;");
        Label g = new Label("G");
        g.setStyle(
                "-fx-font-family: 'Lexend'; -fx-font-size: 14px;" +
                        "-fx-font-weight: 900; -fx-text-fill: #D4FF00;");
        avatar.getChildren().add(g);

        VBox burbuja = new VBox(6);
        burbuja.setMaxWidth(700);

        Label lblNombre = new Label("GYMBROT AI");
        lblNombre.setStyle(
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px;" +
                        "-fx-font-weight: 700; -fx-text-fill: #D4FF00;");

        VBox contenido = new VBox();
        contenido.setStyle(
                "-fx-background-color: #1a1d21;" +
                        "-fx-background-radius: 0 12 12 12;" +
                        "-fx-border-color: #ffffff0d;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 0 12 12 12;");
        contenido.setPadding(new Insets(14, 16, 14, 16));

        Label lblTexto = new Label(texto);
        lblTexto.setStyle(
                "-fx-font-family: 'Inter'; -fx-font-size: 14px;" +
                        "-fx-text-fill: #e2e2e6; -fx-line-spacing: 2;");
        lblTexto.setWrapText(true);
        lblTexto.setMaxWidth(680);
        contenido.getChildren().add(lblTexto);

        Label lblHora = new Label(horaActual());
        lblHora.setStyle(
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px;" +
                        "-fx-text-fill: #4b5563;");

        burbuja.getChildren().addAll(lblNombre, contenido, lblHora);
        hbox.getChildren().addAll(avatar, burbuja);
        vboxMensajes.getChildren().add(hbox);
    }

    // ── INDICADOR DE ESCRITURA ────────────────────────────────────────────
    private HBox crearIndicadorEscritura() {
        HBox hbox = new HBox(12);
        hbox.setAlignment(Pos.TOP_LEFT);

        HBox avatar = new HBox();
        avatar.setAlignment(Pos.CENTER);
        avatar.setPrefWidth(36);
        avatar.setPrefHeight(36);
        avatar.setStyle(
                "-fx-background-color: #1e2a00;" +
                        "-fx-background-radius: 18;" +
                        "-fx-border-color: #D4FF00;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 18;");
        Label g = new Label("G");
        g.setStyle("-fx-font-family: 'Lexend'; -fx-font-size: 14px;" +
                "-fx-font-weight: 900; -fx-text-fill: #D4FF00;");
        avatar.getChildren().add(g);

        VBox contenido = new VBox();
        contenido.setStyle(
                "-fx-background-color: #1a1d21;" +
                        "-fx-background-radius: 0 12 12 12;" +
                        "-fx-border-color: #ffffff0d;" +
                        "-fx-border-width: 1;" +
                        "-fx-border-radius: 0 12 12 12;");
        contenido.setPadding(new Insets(14, 16, 14, 16));

        Label lblEscribiendo = new Label("Escribiendo...");
        lblEscribiendo.setStyle(
                "-fx-font-family: 'Inter'; -fx-font-size: 14px;" +
                        "-fx-text-fill: #4b5563;");
        contenido.getChildren().add(lblEscribiendo);

        hbox.getChildren().addAll(avatar, contenido);
        return hbox;
    }

    // ── HORA ACTUAL ───────────────────────────────────────────────────────
    private String horaActual() {
        return LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
    }

    // ── NAVEGACION ────────────────────────────────────────────────────────
    @FXML private void handleNavDashboard()    { navegarA("/fxml/Dashboard.fxml"); }
    @FXML private void handleNavClientes()     { navegarA("/fxml/GestionClientes.fxml"); }
    @FXML private void handleNavInstructores() { navegarA("/fxml/GestionInstructores.fxml"); }
    @FXML private void handleNavMembresias()   { navegarA("/fxml/GestionMembresias.fxml"); }
    @FXML private void handleNavProgreso()     { }
    @FXML private void handleNavAI()           { }

    @FXML
    private void handleLogout() {
        if (sesionActual != null) {
            chatbotService.cerrarSesion(sesionActual.getIdSesion());
            ChatbotSession.getInstance().setSesionActual(null);
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Seguro que deseas cerrar sesion?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Cerrar sesion");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) navegarA("/fxml/Login.fxml");
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

    // ── ANIMACIONES NAV ───────────────────────────────────────────────────
    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navClientes, navInstructores, navMembresias, navProgreso};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navAI);
    }

    private void agregarHoverInactivo(Button btn) {
        ScaleTransition g = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition s = new ScaleTransition(Duration.millis(180), btn);
        g.setToX(1.03); g.setToY(1.03);
        s.setToX(1.0);  s.setToY(1.0);
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
        s.setToX(1.0);  s.setToY(1.0);
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

    public void setIdCliente(String idCliente) {
        this.idClienteActual = idCliente;
        ChatbotSession session = ChatbotSession.getInstance();
        if (!session.tieneSesionActiva()) {
            if (sesionActual != null) chatbotService.cerrarSesion(sesionActual.getIdSesion());
            sesionActual = chatbotService.iniciarSesion(idCliente);
            session.setSesionActual(sesionActual);
            session.setIdCliente(idCliente);
        }
    }
}