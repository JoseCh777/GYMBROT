package org.gymbrot.controller;

import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import org.gymbrot.model.Cliente;
import org.gymbrot.service.HuellaService;

import java.net.URL;
import java.util.ResourceBundle;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.RegistroIngresoDAO;
import org.gymbrot.model.RegistroIngreso;
import org.gymbrot.service.AuthService;
import org.gymbrot.controller.PerfilClienteController;

public class RegistroEntradaController implements Initializable {

    @FXML private Button btnCancelar;
    @FXML private Label lblTitulo;

    @FXML private Button tabBiometrico;
    @FXML private Button tabManual;
    @FXML private VBox sectionBiometrico;
    @FXML private VBox sectionManual;

    @FXML private TextField txtNumeroId;
    @FXML private PasswordField txtCodigoAcceso;
    @FXML private Button btnValidarIngreso;

    @FXML private Region scanLine;
    @FXML private ProgressBar pbScan;
    @FXML private Label lblEstadoScan;
    @FXML private Rectangle dotBioScan;

    @FXML private Region progressBar;

    private Timeline scanTimeline;
    private Timeline scanLineTimeline;
    private boolean usandoBiometrico = true;
    private StackPane wrapperStack;
    private Parent overlayRoot;
    private HuellaService huellaService;
    private boolean verificando = false;
    private final RegistroIngresoDAO ingresoDAO = new RegistroIngresoDAO();
    private String modo = "ENTRADA";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        huellaService = HuellaService.getInstancia();
        huellaService.addStatusListener(conectado -> {
            Platform.runLater(() -> {
                if (conectado) {
                    dotBioScan.setStyle("-fx-fill: #D4FF00;");
                    lblEstadoScan.setText("ESPERANDO ESCANEO DE HUELLA...");
                    if (usandoBiometrico && !verificando) {
                        iniciarVerificacion();
                    }
                } else {
                    dotBioScan.setStyle("-fx-fill: #ef4444;");
                    lblEstadoScan.setText("LECTOR NO DISPONIBLE");
                }
            });
        });
        huellaService.iniciarLector();
        configurarAnimaciones();
        iniciarScanLine();
        sectionManual.setTranslateX(60);
        aplicarModo();
    }

    private void aplicarModo() {
        if ("SALIDA".equals(modo)) {
            lblTitulo.setText("REGISTRO DE SALIDA");
            btnValidarIngreso.setText("VALIDAR SALIDA");
            lblEstadoScan.setText("ESPERANDO SCAN PARA SALIDA...");
        }
    }

    private void configurarAnimaciones() {
        ScaleTransition grow = new ScaleTransition(Duration.millis(160), btnCancelar);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(160), btnCancelar);
        grow.setToX(1.04); grow.setToY(1.04);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btnCancelar.setOnMouseEntered(e -> grow.playFromStart());
        btnCancelar.setOnMouseExited(e -> shrink.playFromStart());
    }

    private void iniciarScanLine() {
        // scanLine tiene prefWidth=188 y prefHeight=1, alineado TOP_CENTER.
        // translateY=0 → borde superior del StackPane (192px de alto).
        // Rango válido: [0, 190] para que no se salga del borde inferior.
        final double ALTO = 190.0;
        final double PASO = 2.5;             // px por tick a ~60fps
        final double[] posY  = {0.0};
        final boolean[] baja = {true};

        scanLineTimeline = new Timeline(
                new KeyFrame(Duration.millis(16), e -> {
                    if (baja[0]) {
                        posY[0] += PASO;
                        if (posY[0] >= ALTO) { posY[0] = ALTO; baja[0] = false; }
                    } else {
                        posY[0] -= PASO;
                        if (posY[0] <= 0)    { posY[0] = 0;    baja[0] = true;  }
                    }
                    scanLine.setTranslateY(posY[0]);
                    scanLine.setOpacity(baja[0] ? 1.0 : 0.65);
                })
        );
        scanLineTimeline.setCycleCount(Timeline.INDEFINITE);
        scanLineTimeline.play();
    }

    @FXML
    private void handleTabBiometrico() {
        if (usandoBiometrico) return;
        usandoBiometrico = true;

        tabBiometrico.setStyle("-fx-background-color: transparent;" +
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                "-fx-text-fill: #D4FF00; -fx-cursor: hand;" +
                "-fx-border-color: transparent transparent #D4FF00 transparent;" +
                "-fx-border-width: 0 0 2 0;");
        tabManual.setStyle("-fx-background-color: transparent;" +
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                "-fx-text-fill: #6b7280; -fx-cursor: hand;" +
                "-fx-border-color: transparent; -fx-border-width: 0 0 2 0;");

        slideTo(sectionManual, sectionBiometrico, true);
        if (huellaService.lectorActivo() && !verificando) {
            iniciarVerificacion();
        }
    }

    @FXML
    private void handleTabManual() {
        if (!usandoBiometrico) return;
        usandoBiometrico = false;

        tabManual.setStyle("-fx-background-color: transparent;" +
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                "-fx-text-fill: #D4FF00; -fx-cursor: hand;" +
                "-fx-border-color: transparent transparent #D4FF00 transparent;" +
                "-fx-border-width: 0 0 2 0;");
        tabBiometrico.setStyle("-fx-background-color: transparent;" +
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                "-fx-text-fill: #6b7280; -fx-cursor: hand;" +
                "-fx-border-color: transparent; -fx-border-width: 0 0 2 0;");

        slideTo(sectionBiometrico, sectionManual, false);
        huellaService.cancelarEnrolamiento();
    }

    private void iniciarVerificacion() {
        if (verificando) return;
        if (!huellaService.lectorActivo()) {
            lblEstadoScan.setText("LECTOR NO DISPONIBLE");
            return;
        }

        verificando = true;
        pbScan.setProgress(-1);
        lblEstadoScan.setText("PREPARANDO...");

        huellaService.verificarConCaptura(new HuellaService.VerificacionCallback() {
            @Override
            public void onStatus(String status) {
                Platform.runLater(() -> lblEstadoScan.setText(status));
            }

            @Override
            public void onIdentificado(Cliente cliente) {
                Platform.runLater(() -> {
                    pbScan.setProgress(1);
                    dotBioScan.setStyle("-fx-fill: #22c55e;");
                    verificando = false;
                    if ("SALIDA".equals(modo)) {
                        lblEstadoScan.setText("HASTA LUEGO " + cliente.getNombre());
                        ingresoDAO.registrarSalidaPorCliente(cliente.getNumeroIdentificacion());
                    } else {
                        lblEstadoScan.setText("BIENVENIDO " + cliente.getNombre());
                        registrarEntrada(cliente, "HUELLA");
                    }
                    PerfilClienteController.abrirConCliente(cliente, wrapperStack, overlayRoot);
                });
            }

            @Override
            public void onNoIdentificado() {
                Platform.runLater(() -> {
                    pbScan.setProgress(0);
                    dotBioScan.setStyle("-fx-fill: #ef4444;");
                    lblEstadoScan.setText("HUELLA NO RECONOCIDA");
                    mostrarAlerta("Acceso Denegado", "La huella no coincide con ningun socio registrado.");
                    verificando = false;
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    pbScan.setProgress(0);
                    lblEstadoScan.setText("ERROR: " + error);
                    verificando = false;
                });
            }
        });
    }

    private void slideTo(VBox salir, VBox entrar, boolean haciaBiometrico) {
        int dir = haciaBiometrico ? 1 : -1;

        // Aplicar clip al StackPane contenedor para que el dropshadow
        // y los efectos de los hijos no se filtren fuera durante el slide.
        if (salir.getParent() instanceof StackPane sp && sp.getClip() == null) {
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
            clip.widthProperty().bind(sp.widthProperty());
            clip.heightProperty().bind(sp.heightProperty());
            sp.setClip(clip);
        }

        TranslateTransition out = new TranslateTransition(Duration.millis(220), salir);
        out.setToX(dir * -60);
        out.setToY(0);
        out.setOnFinished(e -> {
            salir.setVisible(false);
            salir.setTranslateX(0); // reset para próxima vez
        });

        salir.setManaged(true);
        entrar.setManaged(true);
        entrar.setVisible(true);
        entrar.setTranslateX(dir * 60);
        entrar.setTranslateY(0);
        entrar.setOpacity(0);

        TranslateTransition in = new TranslateTransition(Duration.millis(220), entrar);
        in.setToX(0);
        in.setToY(0);

        // Fade-in suave de la sección entrante
        javafx.animation.FadeTransition fadeIn = new javafx.animation.FadeTransition(Duration.millis(220), entrar);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        out.play();
        in.play();
        fadeIn.play();
    }

    @FXML
    private void handleValidarIngreso() {
        String numId = txtNumeroId.getText().trim();
        String codigo = txtCodigoAcceso.getText().trim();

        if (numId.isEmpty() || codigo.isEmpty()) {
            mostrarAlerta("Validacion", "Completa todos los campos");
            return;
        }

        ClienteDAO clienteDAO = new ClienteDAO();
        Cliente cliente = clienteDAO.buscarPorId(numId);
        if (cliente == null) {
            mostrarAlerta("Error", "No se encontró un cliente con ese número de identificación");
            return;
        }

        AuthService auth = new AuthService();
        if (!auth.validarContrasena(codigo, cliente.getContrasenaHash())) {
            mostrarAlerta("Acceso Denegado", "Contraseña incorrecta");
            return;
        }

        if ("SALIDA".equals(modo)) {
            ingresoDAO.registrarSalidaPorCliente(cliente.getNumeroIdentificacion());
        } else {
            registrarEntrada(cliente, "MANUAL");
        }
        PerfilClienteController.abrirConCliente(cliente, wrapperStack, overlayRoot);
    }


    private void registrarEntrada(Cliente cliente, String metodo) {
        RegistroIngreso ri = new RegistroIngreso();
        ri.setIdCliente(cliente.getNumeroIdentificacion());
        ri.setFecha(java.time.LocalDate.now());
        ri.setHoraEntrada(java.time.LocalDateTime.now());
        ri.setMetodoVerificacion(metodo);
        ri.setEstadoVerificacion("APROBADO");
        ingresoDAO.registrarEntrada(ri);
    }

    public void setModo(String modo) {
        this.modo = modo;
        aplicarModo();
    }

    public void setWrapperStack(StackPane wrapper, Parent overlayRoot) {
        this.wrapperStack = wrapper;
        this.overlayRoot = overlayRoot;
    }

    @FXML
    private void handleCerrar() {
        cerrarOverlay();
    }

    private void cerrarOverlay() {
        if (scanLineTimeline != null) scanLineTimeline.stop();
        if (scanTimeline != null) scanTimeline.stop();
        huellaService.cancelarEnrolamiento();
        if (wrapperStack != null && overlayRoot != null) {
            wrapperStack.getChildren().remove(overlayRoot);
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
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