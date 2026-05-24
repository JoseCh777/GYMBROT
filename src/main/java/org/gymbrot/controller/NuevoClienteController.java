package org.gymbrot.controller;

import javafx.animation.FadeTransition;
import javafx.application.Platform;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import com.digitalpersona.onetouch.DPFPTemplate;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.UsuarioDAO;
import org.gymbrot.model.Cliente;
import org.gymbrot.model.Usuario;
import org.gymbrot.service.AuthService;
import org.gymbrot.service.HuellaService;
import org.gymbrot.util.HuellaUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

/**
 * NuevoClienteController
 *
 * Tablas Oracle que alimenta al guardar:
 *  - USUARIOS        → numero_identificacion, tipo_doc, nombre, apellidos,
 *                       correo, telefono, contrasena_hash, fecha_nacimiento,
 *                       tipo_usuario = 'CLIENTE', estado = 'ACTIVO'
 *  - CLIENTES        → numero_identificacion, direccion, foto_perfil (path o BLOB),
 *                       huella_digital (template bytes del AS608)
 *
 * TODO: cuando tengas el DAO, reemplaza los métodos stub con llamadas reales.
 */
public class NuevoClienteController implements Initializable {

    // ─── SideNav ───────────────────────────────────────────────────────────
    @FXML private VBox    sideNav;
    @FXML private Button  navDashboard;
    @FXML private Button  navClientes;
    @FXML private Button  navInstructores;
    @FXML private Button  navMembresias;
    @FXML private Button  navAI;

    // ─── TopBar ────────────────────────────────────────────────────────────
    @FXML private HBox   topBar;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    // ─── Formulario: Información del Usuario ───────────────────────────────
    @FXML private TextField     txtNumeroDoc;
    @FXML private ComboBox<String> cmbTipoDoc;
    @FXML private TextField     txtNombres;
    @FXML private TextField     txtApellidos;
    @FXML private TextField     txtCorreo;
    @FXML private TextField     txtTelefono;
    @FXML private PasswordField txtContrasena;

    // ─── Formulario: Información de Cliente ────────────────────────────────
    @FXML private TextField  txtDireccion;
    @FXML private DatePicker dateFechaNacimiento;

    // ─── Columna derecha: Foto ─────────────────────────────────────────────
    @FXML private ImageView imgFotoPerfil;
    @FXML private Label     lblFotoPlaceholder;
    @FXML private Button    btnCargarFoto;

    // ─── Columna derecha: Biometría ────────────────────────────────────────
    @FXML private Button      btnEscanearHuella;
    @FXML private StackPane   scannerCircle;  // círculo del scanner con glow
    @FXML private Region      lineaEscaneo;    // línea animada de escaneo
    @FXML private Label       lblScannerTitulo;
    @FXML private Label       lblScannerStatus;
    @FXML private ProgressBar pbHuella;
    @FXML private Label       lblPorcentajeHuella;
    @FXML private Region      bioIndicator;
    @FXML private Label       bioStatusLabel;
    @FXML private Region      dot1;
    @FXML private Region      dot2;
    @FXML private Region      dot3;
    @FXML private Region      dot4;

    // ─── Estado interno ────────────────────────────────────────────────────
    private File   archivoFotoSeleccionado = null;
    private boolean huellaCapturada        = false;
    private int     muestrasCapturadas     = 0;   // 0..4
    private Timeline timelineHuella        = null;
    private Timeline timelineLineaEscaneo  = null;
    private HuellaService huellaService;
    private DPFPTemplate templateCapturado = null;

    // ─── Validación ────────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern TELEFONO_PATTERN =
            Pattern.compile("^[0-9 \\-+]{7,15}$");

    // ─── Estilos de campos ─────────────────────────────────────────────────
    private static final String FIELD_NORMAL =
            "-fx-background-color: #1a1c1f; -fx-background-radius: 8;" +
                    "-fx-border-color: #1f2125; -fx-border-width: 1; -fx-border-radius: 8;" +
                    "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;" +
                    "-fx-prompt-text-fill: #444749; -fx-padding: 10 14 10 14;";
    private static final String FIELD_ERROR =
            "-fx-background-color: #1a1c1f; -fx-background-radius: 8;" +
                    "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 8;" +
                    "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;" +
                    "-fx-prompt-text-fill: #444749; -fx-padding: 10 14 10 14;";
    private static final String FIELD_OK =
            "-fx-background-color: #1a1c1f; -fx-background-radius: 8;" +
                    "-fx-border-color: #D4FF00; -fx-border-width: 1; -fx-border-radius: 8;" +
                    "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;" +
                    "-fx-prompt-text-fill: #444749; -fx-padding: 10 14 10 14;";

    // ═══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarAnimacionesNav();
        setNavActivo(navClientes);
        configurarAnimacionesBotones();
        configurarComboBox();
        configurarValidacionEnVivo();
        configurarFocusFields();
        aplicarClipCircularFoto();
        inicializarBiometria();
    }

    private void inicializarBiometria() {
        huellaService = HuellaService.getInstancia();

        huellaService.addStatusListener(conectado -> {
            if (Platform.isFxApplicationThread()) {
                actualizarIndicadorBio(conectado);
            } else {
                Platform.runLater(() -> actualizarIndicadorBio(conectado));
            }
        });
    }

    private void actualizarIndicadorBio(boolean conectado) {
        if (conectado) {
            bioIndicator.setStyle("-fx-background-color: #72e06a; -fx-background-radius: 50%; -fx-min-width: 6; -fx-min-height: 6;");
            bioStatusLabel.setText("LECTOR CONECTADO");
            bioStatusLabel.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #72e06a;");
        } else {
            bioIndicator.setStyle("-fx-background-color: #ff6b6b; -fx-background-radius: 50%; -fx-min-width: 6; -fx-min-height: 6;");
            bioStatusLabel.setText("LECTOR DESCONECTADO");
            bioStatusLabel.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #ff6b6b;");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NAV — Animaciones idénticas al Dashboard y GestionClientes
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navInstructores, navMembresias, navAI};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navClientes);
    }

    private void agregarHoverInactivo(Button btn) {
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03);   grow.setToY(1.03);
        shrink.setToX(1.0);  shrink.setToY(1.0);

        btn.setOnMouseEntered(e -> {
            grow.playFromStart();
            btn.setStyle(btn.getStyle()
                    .replace("-fx-background-color: transparent", "-fx-background-color: #1f2226")
                    .replace("-fx-text-fill: #9ca3af", "-fx-text-fill: white"));
        });
        btn.setOnMouseExited(e -> {
            shrink.playFromStart();
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
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03);  grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);

        btn.setOnMouseEntered(e  -> grow.playFromStart());
        btn.setOnMouseExited(e   -> shrink.playFromStart());
        btn.setOnMousePressed(e  -> {
            ScaleTransition p = new ScaleTransition(Duration.millis(80), btn);
            p.setToX(0.97); p.setToY(0.97); p.play();
        });
        btn.setOnMouseReleased(e -> {
            ScaleTransition r = new ScaleTransition(Duration.millis(80), btn);
            r.setToX(1.0); r.setToY(1.0); r.play();
        });
    }

    private void setNavActivo(Button activo) {
        Button[] todos = {navDashboard, navClientes, navInstructores, navMembresias, navAI};
        for (Button btn : todos) {
            if (btn == activo) {
                btn.setStyle(
                        "-fx-background-color: #D4FF00; -fx-background-radius: 8;" +
                                "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 700;" +
                                "-fx-text-fill: black; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;"
                );
                agregarHoverActivo(btn);
            } else {
                btn.setStyle(
                        "-fx-background-color: transparent; -fx-background-radius: 8;" +
                                "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 500;" +
                                "-fx-text-fill: #9ca3af; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;"
                );
                agregarHoverInactivo(btn);
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BOTONES TOPBAR — Animaciones
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarAnimacionesBotones() {
        // Guardar: hover con leve brillo
        agregarHoverActivo(btnGuardar);

        // Cancelar: hover con fondo más claro
        agregarHoverInactivo(btnCancelar);

        // Escanear huella: escala
        agregarHoverInactivo(btnEscanearHuella);

        // Cargar foto: escala + color
        ScaleTransition gf = new ScaleTransition(Duration.millis(180), btnCargarFoto);
        ScaleTransition sf = new ScaleTransition(Duration.millis(180), btnCargarFoto);
        gf.setToX(1.05); gf.setToY(1.05);
        sf.setToX(1.0);  sf.setToY(1.0);
        btnCargarFoto.setOnMouseEntered(e -> gf.playFromStart());
        btnCargarFoto.setOnMouseExited(e  -> sf.playFromStart());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  COMBO BOX
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarComboBox() {
        cmbTipoDoc.getItems().addAll("Cédula de Ciudadanía", "Cédula de Extranjería",
                "Tarjeta de Identidad", "Pasaporte", "NIT");
        cmbTipoDoc.getSelectionModel().selectFirst();

        // Estilo del ComboBox para que coincida con los TextField
        cmbTipoDoc.setStyle(
                "-fx-background-color: #1a1c1f; -fx-background-radius: 8;" +
                        "-fx-border-color: #1f2125; -fx-border-width: 1; -fx-border-radius: 8;" +
                        "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;"
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VALIDACIÓN EN VIVO — Borde cambia a amarillo/rojo mientras el usuario escribe
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarValidacionEnVivo() {

        // Número de documento — solo dígitos
        txtNumeroDoc.textProperty().addListener((obs, old, val) -> {
            if (val.isBlank()) {
                txtNumeroDoc.setStyle(FIELD_NORMAL);
            } else if (val.matches("[0-9 \\-]{5,20}")) {
                txtNumeroDoc.setStyle(FIELD_OK);
            } else {
                txtNumeroDoc.setStyle(FIELD_ERROR);
            }
        });

        // Nombres — no vacío
        txtNombres.textProperty().addListener((obs, old, val) ->
                txtNombres.setStyle(val.trim().length() >= 2 ? FIELD_OK : FIELD_NORMAL));

        // Apellidos — no vacío
        txtApellidos.textProperty().addListener((obs, old, val) ->
                txtApellidos.setStyle(val.trim().length() >= 2 ? FIELD_OK : FIELD_NORMAL));

        // Correo — regex básico
        txtCorreo.textProperty().addListener((obs, old, val) -> {
            if (val.isBlank()) {
                txtCorreo.setStyle(FIELD_NORMAL);
            } else if (EMAIL_PATTERN.matcher(val.trim()).matches()) {
                txtCorreo.setStyle(FIELD_OK);
            } else {
                txtCorreo.setStyle(FIELD_ERROR);
            }
        });

        // Teléfono
        txtTelefono.textProperty().addListener((obs, old, val) -> {
            if (val.isBlank()) {
                txtTelefono.setStyle(FIELD_NORMAL);
            } else if (TELEFONO_PATTERN.matcher(val.trim()).matches()) {
                txtTelefono.setStyle(FIELD_OK);
            } else {
                txtTelefono.setStyle(FIELD_ERROR);
            }
        });

        // Contraseña — mínimo 8 caracteres
        txtContrasena.textProperty().addListener((obs, old, val) -> {
            if (val.isBlank()) {
                txtContrasena.setStyle(FIELD_NORMAL);
            } else if (val.length() >= 8) {
                txtContrasena.setStyle(FIELD_OK);
            } else {
                txtContrasena.setStyle(FIELD_ERROR);
            }
        });

        // Dirección
        txtDireccion.textProperty().addListener((obs, old, val) ->
                txtDireccion.setStyle(val.trim().length() >= 5 ? FIELD_OK : FIELD_NORMAL));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FOCUS — Borde resaltado al enfocar cada campo
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarFocusFields() {
        TextField[] campos = {txtNumeroDoc, txtNombres, txtApellidos,
                txtCorreo, txtTelefono, txtDireccion};
        for (TextField tf : campos) {
            tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                // Solo aplicar glow si el campo está en estado normal (sin validación)
                if (tf.getStyle().contains("#1f2125")) {
                    tf.setStyle(isFocused
                            ? tf.getStyle().replace("-fx-border-color: #1f2125", "-fx-border-color: #555a40")
                            : tf.getStyle().replace("-fx-border-color: #555a40", "-fx-border-color: #1f2125"));
                }
            });
        }
        txtContrasena.focusedProperty().addListener((obs, was, isFocused) -> {
            if (txtContrasena.getStyle().contains("#1f2125")) {
                txtContrasena.setStyle(isFocused
                        ? txtContrasena.getStyle().replace("-fx-border-color: #1f2125", "-fx-border-color: #555a40")
                        : txtContrasena.getStyle().replace("-fx-border-color: #555a40", "-fx-border-color: #1f2125"));
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMACIÓN LÍNEA DE ESCANEO
    // ═══════════════════════════════════════════════════════════════════════

    private void iniciarAnimacionLineaEscaneo() {
        final double[] posY   = {0.0};
        final boolean[] baja  = {true};
        final double    MAX_Y = 98.0;
        final double    STEP  = 1.8;

        timelineLineaEscaneo = new Timeline(
                new KeyFrame(Duration.millis(16), e -> {   // ~60fps
                    if (huellaCapturada) return;
                    if (baja[0]) {
                        posY[0] += STEP;
                        if (posY[0] >= MAX_Y) { posY[0] = MAX_Y; baja[0] = false; }
                    } else {
                        posY[0] -= STEP;
                        if (posY[0] <= 0) { posY[0] = 0; baja[0] = true; }
                    }
                    // Mover la línea verticalmente dentro del StackPane
                    lineaEscaneo.setTranslateY(posY[0]);

                    // Pulso de opacidad: más brillante al bajar, más tenue al subir
                    double opacity = baja[0] ? 0.9 : 0.6;
                    lineaEscaneo.setOpacity(opacity);

                    double glow = 0.3 + 0.5 * (posY[0] / MAX_Y);
                    scannerCircle.setStyle(scannerCircle.getStyle()
                            .replaceAll("-fx-effect:.*?;", "")
                            + String.format("-fx-effect: dropshadow(gaussian, #D4FF00, %.0f, 0.3, 0, 0);",
                            6 + glow * 10));
                })
        );
        timelineLineaEscaneo.setCycleCount(Timeline.INDEFINITE);
        timelineLineaEscaneo.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLER — Cargar Foto
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleCargarFoto() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar foto de perfil");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imágenes", "*.jpg", "*.jpeg", "*.png")
        );

        Stage stage = (Stage) sideNav.getScene().getWindow();
        File archivo = chooser.showOpenDialog(stage);

        if (archivo != null) {
            // Verificar tamaño máximo 2MB
            if (archivo.length() > 2 * 1024 * 1024) {
                mostrarError("Foto demasiado grande",
                        "La imagen no puede superar 2MB. Selecciona otra.");
                return;
            }

            archivoFotoSeleccionado = archivo;
            Image img = new Image(archivo.toURI().toString(),
                    152, 152, false, true);
            imgFotoPerfil.setImage(img);
            imgFotoPerfil.setFitWidth(152);
            imgFotoPerfil.setFitHeight(152);
            imgFotoPerfil.setPreserveRatio(false);

            // Ocultar placeholder y mostrar imagen con fade
            lblFotoPlaceholder.setVisible(false);
            imgFotoPerfil.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), imgFotoPerfil);
            ft.setFromValue(0); ft.setToValue(1);
            ft.play();

            btnCargarFoto.setText("CAMBIAR IMAGEN");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLER — Escanear Huella
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleEscanearHuella() {
        if (huellaCapturada) return;

        muestrasCapturadas = 0;
        iniciarAnimacionLineaEscaneo();

        btnEscanearHuella.setDisable(true);
        lblScannerStatus.setText("INICIANDO...");
        pbHuella.setProgress(0);
        lblPorcentajeHuella.setText("0%");

        huellaService.iniciarEnrolamientoConCaptura(new HuellaService.EnrollmentCallback() {
            @Override
            public void onStatus(String status) {
                Platform.runLater(() -> {
                    lblScannerStatus.setText(status);
                    btnEscanearHuella.setText("ESPERANDO DEDO...");
                });
            }

            @Override
            public void onProgress(int captured, int total) {
                Platform.runLater(() -> {
                    muestrasCapturadas = captured;
                    double progreso = captured / (double) total;
                    pbHuella.setProgress(progreso);
                    lblPorcentajeHuella.setText((captured * 25) + "%");
                    lblScannerTitulo.setText("Captura " + captured + " de " + total);
                    btnEscanearHuella.setText("CAPTURANDO...");
                });
            }

            @Override
            public void onSampleRejected(String reason) {
                Platform.runLater(() -> {
                    lblScannerStatus.setText("REPETIR: " + reason);
                    btnEscanearHuella.setDisable(false);
                    btnEscanearHuella.setText("INTENTAR DE NUEVO");
                });
            }

            @Override
            public void onComplete(DPFPTemplate template) {
                Platform.runLater(() -> {
                    templateCapturado = template;
                    huellaCapturada = true;
                    if (timelineLineaEscaneo != null) timelineLineaEscaneo.stop();

                    lineaEscaneo.setTranslateY(40);
                    lineaEscaneo.setOpacity(1.0);
                    lineaEscaneo.setStyle("-fx-background-color: #bdf4ff; -fx-effect: dropshadow(gaussian, #bdf4ff, 6, 0.7, 0, 0);");
                    scannerCircle.setStyle(scannerCircle.getStyle()
                            .replaceAll("-fx-effect:.*?;", "")
                            + "-fx-effect: dropshadow(gaussian, #bdf4ff, 12, 0.5, 0, 0);");

                    lblScannerTitulo.setText("Huella Registrada");
                    lblScannerTitulo.setStyle("-fx-font-family: 'Lexend'; -fx-font-size: 16px; -fx-font-weight: 600; -fx-text-fill: #bdf4ff;");
                    lblScannerStatus.setText("HUELLA REGISTRADA");
                    lblScannerStatus.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #bdf4ff;");
                    lblPorcentajeHuella.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #bdf4ff;");
                    pbHuella.setStyle("-fx-accent: #bdf4ff; -fx-background-color: #282a2d; -fx-background-radius: 4; -fx-border-width: 0;");

                    btnEscanearHuella.setText("REGISTRO COMPLETO ✓");
                    btnEscanearHuella.setDisable(true);
                    btnEscanearHuella.setStyle("-fx-background-color: #002a30; -fx-background-radius: 8; -fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #bdf4ff; -fx-border-color: #0a4a50; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: default;");
                });
            }

            @Override
            public void onError(String error) {
                Platform.runLater(() -> {
                    lblScannerStatus.setText("ERROR: " + error);
                    btnEscanearHuella.setDisable(false);
                    btnEscanearHuella.setText("REINTENTAR");
                });
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLER — Guardar
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleGuardar() {
        if (!validarFormulario()) return;

        UsuarioDAO usuarioDAO = new UsuarioDAO();
        ClienteDAO clienteDAO = new ClienteDAO();
        AuthService authService = new AuthService();

        Usuario u = new Usuario();
        u.setNumeroIdentificacion(txtNumeroDoc.getText().trim());
        u.setTipoIdentificacion(tipoDocToCode(cmbTipoDoc.getValue()));
        u.setNombre(txtNombres.getText().trim());
        u.setApellidos(txtApellidos.getText().trim());
        u.setCorreo(txtCorreo.getText().trim());
        u.setTelefono(txtTelefono.getText().trim());
        u.setContrasenaHash(authService.hashContrasena(txtContrasena.getText()));
        u.setFechaRegistro(LocalDate.now());
        u.setTipoUsuario("CLIENTE");
        u.setEstado("ACTIVO");

        boolean ok = usuarioDAO.insertar(u);
        if (!ok) {
            mostrarError("Error", "No se pudo registrar el usuario.");
            return;
        }

        Cliente c = new Cliente();
        c.setNumeroIdentificacion(u.getNumeroIdentificacion());
        c.setDireccion(txtDireccion.getText().trim());
        c.setFechaNacimiento(dateFechaNacimiento.getValue());
        if (archivoFotoSeleccionado != null)
            c.setFotoUrl(archivoFotoSeleccionado.getAbsolutePath());
        if (templateCapturado != null)
            c.setHuellaDactilar(templateCapturado.serialize());
        ok = clienteDAO.insertar(c);
        if (!ok) {
            mostrarError("Error", "No se pudo registrar el cliente.");
            return;
        }

        // Animación de confirmación en el botón Guardar
        btnGuardar.setText("GUARDADO ✓");
        btnGuardar.setStyle(
                "-fx-background-color: #bdf4ff; -fx-background-radius: 8;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-text-fill: #001f24; -fx-cursor: hand; -fx-padding: 8 20 8 20;"
        );

        // Volver a GestionClientes después de 800ms
        Timeline espera = new Timeline(
                new KeyFrame(Duration.millis(800), e -> navegarA("/fxml/GestionClientes.fxml"))
        );
        espera.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLER — Cancelar
    // ═══════════════════════════════════════════════════════════════════════

    private String tipoDocToCode(String display) {
        if (display == null) return null;
        switch (display) {
            case "Cédula de Ciudadanía":        return "CC";
            case "Cédula de Extranjería":       return "CE";
            case "Tarjeta de Identidad":        return "TI";
            case "Pasaporte":                   return "PASAPORTE";
            case "NIT":                         return "NIT";
            default:                            return display;
        }
    }

    @FXML
    private void handleCancelar() {
        boolean hayDatos = !txtNombres.getText().isBlank()
                || !txtApellidos.getText().isBlank()
                || !txtNumeroDoc.getText().isBlank();

        if (hayDatos) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Tienes datos sin guardar. ¿Deseas salir de todas formas?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Cancelar registro");
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) navegarA("/fxml/GestionClientes.fxml");
            });
        } else {
            navegarA("/fxml/GestionClientes.fxml");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VALIDACIÓN DEL FORMULARIO
    // ═══════════════════════════════════════════════════════════════════════

    private boolean validarFormulario() {
        boolean valido = true;

        // Número de documento
        if (!txtNumeroDoc.getText().matches("[0-9 \\-]{5,20}")) {
            txtNumeroDoc.setStyle(FIELD_ERROR);
            valido = false;
        }

        // Tipo documento
        if (cmbTipoDoc.getValue() == null) {
            cmbTipoDoc.setStyle(
                    "-fx-background-color: #1a1c1f; -fx-background-radius: 8;" +
                            "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 8;" +
                            "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;"
            );
            valido = false;
        }

        // Nombres
        if (txtNombres.getText().trim().length() < 2) {
            txtNombres.setStyle(FIELD_ERROR);
            valido = false;
        }

        // Apellidos
        if (txtApellidos.getText().trim().length() < 2) {
            txtApellidos.setStyle(FIELD_ERROR);
            valido = false;
        }

        // Correo
        if (!EMAIL_PATTERN.matcher(txtCorreo.getText().trim()).matches()) {
            txtCorreo.setStyle(FIELD_ERROR);
            valido = false;
        }

        // Teléfono
        if (!TELEFONO_PATTERN.matcher(txtTelefono.getText().trim()).matches()) {
            txtTelefono.setStyle(FIELD_ERROR);
            valido = false;
        }

        // Contraseña
        if (txtContrasena.getText().length() < 8) {
            txtContrasena.setStyle(FIELD_ERROR);
            valido = false;
        }

        // Dirección
        if (txtDireccion.getText().trim().length() < 5) {
            txtDireccion.setStyle(FIELD_ERROR);
            valido = false;
        }

        // Fecha de nacimiento
        if (dateFechaNacimiento.getValue() == null) {
            dateFechaNacimiento.setStyle(
                    "-fx-background-color: #1a1c1f; -fx-background-radius: 8;" +
                            "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 8;" +
                            "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;"
            );
            valido = false;
        } else {
            // Verificar que tenga al menos 5 años
            LocalDate hoy = LocalDate.now();
            if (Period.between(dateFechaNacimiento.getValue(), hoy).getYears() < 5) {
                dateFechaNacimiento.setStyle(
                        "-fx-background-color: #1a1c1f; -fx-background-radius: 8;" +
                                "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 8;" +
                                "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;"
                );
                mostrarError("Fecha inválida", "La fecha de nacimiento no es válida.");
                valido = false;
            }
        }

        if (!valido) {
            // Shake suave en el botón Guardar para indicar error
            ScaleTransition shake = new ScaleTransition(Duration.millis(60), btnGuardar);
            shake.setFromX(1.0); shake.setToX(0.95);
            shake.setCycleCount(4); shake.setAutoReverse(true);
            shake.play();
            mostrarError("Formulario incompleto",
                    "Revisa los campos marcados en rojo antes de guardar.");
        }

        return valido;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — NAV
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handleNavDashboard()    { navegarA("/fxml/Dashboard.fxml"); }
    @FXML private void handleNavClientes()     { navegarA("/fxml/GestionClientes.fxml"); }
    @FXML private void handleNavInstructores() {  }
    @FXML private void handleNavMembresias()   { navegarA("/fxml/GestionMembresias.fxml");}
    @FXML private void handleNavAI()           {  }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Seguro que deseas cerrar sesión?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Cerrar sesión");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) navegarA("/fxml/Login.fxml");
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UTILIDADES


    // ═══════════════════════════════════════════════════════════════════════
    //  CLIP CIRCULAR — Recorta la imagen en círculo exacto
    // ═══════════════════════════════════════════════════════════════════════

    private void aplicarClipCircularFoto() {
        // Clip circular de radio 76 (diámetro 152px igual que el ImageView)
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(76, 76, 76);
        imgFotoPerfil.setClip(clip);
    }

    private void navegarA(String rutaFxml) {
        if (timelineHuella != null) timelineHuella.stop();
        if (timelineLineaEscaneo != null) timelineLineaEscaneo.stop();
        // Limpiar captura activa si la hay
        if (huellaService.isCapturaActiva()) {
            huellaService.deshabilitarCaptura();
        }
        huellaService.cancelarEnrolamiento();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            Parent root = loader.load();
            Stage stage = (Stage) sideNav.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error de navegación", "No se pudo cargar: " + rutaFxml);
        }
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}