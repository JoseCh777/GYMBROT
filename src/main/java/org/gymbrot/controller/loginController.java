package org.gymbrot.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.gymbrot.dao.UsuarioDAO;
import org.gymbrot.model.Usuario;
import org.gymbrot.service.HuellaService;
import org.mindrot.jbcrypt.BCrypt;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class loginController implements Initializable {

    @FXML private ImageView backgroundImage;
    @FXML private Label brandTitle;
    @FXML private VBox loginCard;
    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private Hyperlink forgotPasswordLink;
    @FXML private Button loginButton;
    @FXML private Region bioIndicator;
    @FXML private Label bioStatusLabel;

    private HuellaService huellaService;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarFocusFields();
        configurarAnimaciones();
        configurarEnter();
        animarEntrada();
        inicializarBiometria();
    }

    private void configurarEnter() {
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> BtnIniciarSecion());
    }

    private void inicializarBiometria() {
        try {
            huellaService = HuellaService.getInstancia();

            huellaService.addStatusListener(conectado -> {
                if (Platform.isFxApplicationThread()) {
                    actualizarIndicadorBio(conectado);
                } else {
                    Platform.runLater(() -> actualizarIndicadorBio(conectado));
                }
            });

            huellaService.iniciarLector();
        } catch (Exception e) {
            System.err.println("✗ " + e.getMessage());
        }
    }

    private void actualizarIndicadorBio(boolean conectado) {
        if (conectado) {
            bioIndicator.setStyle("-fx-background-color: #72e06a; -fx-background-radius: 50%; -fx-min-width: 8; -fx-min-height: 8;");
            bioStatusLabel.setText("LECTOR CONECTADO");
            bioStatusLabel.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #72e06a;");
        } else {
            bioIndicator.setStyle("-fx-background-color: #ff6b6b; -fx-background-radius: 50%; -fx-min-width: 8; -fx-min-height: 8;");
            bioStatusLabel.setText("LECTOR DESCONECTADO");
            bioStatusLabel.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #ff6b6b;");
        }
    }

    private void configurarFocusFields() {
        usernameField.focusedProperty().addListener((obs, was, isFocused) -> {
            if (isFocused) {
                usernameField.setStyle(usernameField.getStyle()
                        .replace("-fx-border-width: 0", "-fx-border-width: 1")
                        .replace("-fx-border-color: transparent", "-fx-border-color: #CAF300"));
            } else {
                usernameField.setStyle(usernameField.getStyle()
                        .replace("-fx-border-width: 1", "-fx-border-width: 0")
                        .replace("-fx-border-color: #CAF300", "-fx-border-color: transparent"));
            }
        });

        passwordField.focusedProperty().addListener((obs, was, isFocused) -> {
            if (isFocused) {
                passwordField.setStyle(passwordField.getStyle()
                        .replace("-fx-border-width: 0", "-fx-border-width: 1")
                        .replace("-fx-border-color: transparent", "-fx-border-color: #CAF300"));
            } else {
                passwordField.setStyle(passwordField.getStyle()
                        .replace("-fx-border-width: 1", "-fx-border-width: 0")
                        .replace("-fx-border-color: #CAF300", "-fx-border-color: transparent"));
            }
        });
    }

    private void configurarAnimaciones() {
        ScaleTransition grow = new ScaleTransition(Duration.millis(180), loginButton);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), loginButton);
        grow.setToX(1.03); grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);

        loginButton.setOnMouseEntered(e -> grow.playFromStart());
        loginButton.setOnMouseExited(e -> shrink.playFromStart());
        loginButton.setOnMousePressed(e -> {
            ScaleTransition p = new ScaleTransition(Duration.millis(80), loginButton);
            p.setToX(0.97); p.setToY(0.97); p.play();
        });
        loginButton.setOnMouseReleased(e -> {
            ScaleTransition r = new ScaleTransition(Duration.millis(80), loginButton);
            r.setToX(1.0); r.setToY(1.0); r.play();
        });

        ScaleTransition fg = new ScaleTransition(Duration.millis(180), forgotPasswordLink);
        ScaleTransition fs = new ScaleTransition(Duration.millis(180), forgotPasswordLink);
        fg.setToX(1.03); fg.setToY(1.03);
        fs.setToX(1.0); fs.setToY(1.0);
        forgotPasswordLink.setOnMouseEntered(e -> fg.playFromStart());
        forgotPasswordLink.setOnMouseExited(e -> fs.playFromStart());
    }

    private void animarEntrada() {
        loginCard.setOpacity(0);
        loginCard.setTranslateY(20);
        FadeTransition ft = new FadeTransition(Duration.millis(600), loginCard);
        ft.setFromValue(0); ft.setToValue(1);
        ft.play();

        ScaleTransition st = new ScaleTransition(Duration.millis(600), loginCard);
        st.setFromX(0.97); st.setFromY(0.97);
        st.setToX(1.0); st.setToY(1.0);
        st.play();
    }

    @FXML
    private void BtnIniciarSecion() {
        String usuario = usernameField.getText().trim();
        String contrasena = passwordField.getText();

        if (usuario.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Campos vacios", "Ingresa tu usuario y contrasena.");
            return;
        }

        UsuarioDAO dao = new UsuarioDAO();
        Usuario user = dao.buscarPorNombreOCorreo(usuario);

        if (user != null && verificarContrasena(contrasena, user.getContrasenaHash())) {
            navegarA("/fxml/Dashboard.fxml");
        } else {
            mostrarError("Credenciales invalidas", "Usuario o contrasena incorrectos.");
            usernameField.setStyle("-fx-background-color: #333538; -fx-background-radius: 8;" +
                    "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 8;" +
                    "-fx-font-family: 'Inter'; -fx-font-size: 15px; -fx-text-fill: #e2e2e6;" +
                    "-fx-prompt-text-fill: #555759; -fx-padding: 14 16 14 48;");
            passwordField.setStyle("-fx-background-color: #333538; -fx-background-radius: 8;" +
                    "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 8;" +
                    "-fx-font-family: 'Inter'; -fx-font-size: 15px; -fx-text-fill: #e2e2e6;" +
                    "-fx-prompt-text-fill: #555759; -fx-padding: 14 16 14 48;");
        }
    }

    private boolean verificarContrasena(String contrasenaPlana, String hash) {
        try {
            return BCrypt.checkpw(contrasenaPlana, hash);
        } catch (Exception e) {
            return contrasenaPlana.equals(hash);
        }
    }

    @FXML
    private void btnContrase\u00F1a() {
        mostrarInfo("Recuperar contrasena",
                "Contacta al administrador del sistema para restablecer tu contrasena.");
    }

    private void navegarA(String rutaFxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(rutaFxml));
            Stage stage = (Stage) loginButton.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo cargar la vista: " + rutaFxml);
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
