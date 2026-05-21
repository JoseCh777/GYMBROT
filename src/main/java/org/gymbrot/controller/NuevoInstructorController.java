package org.gymbrot.controller;

import javafx.animation.FadeTransition;
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
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class NuevoInstructorController implements Initializable {

    // ─── SideNav ───────────────────────────────────────────────────────────
    @FXML private VBox sideNav;
    @FXML private Button navDashboard;
    @FXML private Button navClientes;
    @FXML private Button navInstructores;
    @FXML private Button navMembresias;
    @FXML private Button navAI;

    // ─── TopBar ────────────────────────────────────────────────────────────
    @FXML private HBox topBar;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    // ─── Formulario: Informacion del Instructor ─────────────────────────
    @FXML private TextField txtNumeroId;
    @FXML private TextField txtNombres;
    @FXML private TextField txtApellidos;
    @FXML private TextField txtCorreo;
    @FXML private TextField txtTelefono;

    // ─── Formulario: Especialidad y Disponibilidad ──────────────────────
    @FXML private ComboBox<String> cmbEspecialidad;
    @FXML private DatePicker dateFechaContratacion;
    @FXML private TextArea txtNotas;

    // ─── Foto de perfil ────────────────────────────────────────────────
    @FXML private ImageView imgFotoPerfil;
    @FXML private Label lblFotoPlaceholder;
    @FXML private Button btnCargarFoto;

    // ─── Estado interno ────────────────────────────────────────────────
    private File archivoFotoSeleccionado;

    // ─── Validacion ────────────────────────────────────────────────────
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern TELEFONO_PATTERN =
            Pattern.compile("^[0-9 \\-+]{7,15}$");

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
        setNavActivo(navInstructores);
        configurarAnimacionesBotones();
        cargarEspecialidades();
        configurarFechaContratacion();
        configurarValidacionEnVivo();
        configurarFocusFields();
        aplicarClipCircularFoto();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NAV — Animaciones
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navClientes, navMembresias, navAI};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navInstructores);
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
        agregarHoverActivo(btnGuardar);
        agregarHoverInactivo(btnCancelar);

        ScaleTransition gf = new ScaleTransition(Duration.millis(180), btnCargarFoto);
        ScaleTransition sf = new ScaleTransition(Duration.millis(180), btnCargarFoto);
        gf.setToX(1.05); gf.setToY(1.05);
        sf.setToX(1.0);  sf.setToY(1.0);
        btnCargarFoto.setOnMouseEntered(e -> gf.playFromStart());
        btnCargarFoto.setOnMouseExited(e  -> sf.playFromStart());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ESPECIALIDADES
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarEspecialidades() {
        cmbEspecialidad.getItems().addAll(
            "Bodybuilding y Hipertrofia",
            "Powerlifting / Fuerza",
            "Crossfit / Entrenamiento Funcional",
            "Rehabilitacion Deportiva",
            "HIIT y Cardiovascular",
            "Yoga y Flexibilidad",
            "Natacion",
            "Artes Marciales"
        );
        cmbEspecialidad.setStyle(
                "-fx-background-color: #1a1c1f; -fx-background-radius: 8;" +
                        "-fx-border-color: #1f2125; -fx-border-width: 1; -fx-border-radius: 8;" +
                        "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;"
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FECHA DE CONTRATACION
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarFechaContratacion() {
        dateFechaContratacion.setValue(LocalDate.now());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VALIDACION EN VIVO
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarValidacionEnVivo() {
        txtNumeroId.textProperty().addListener((obs, old, val) -> {
            if (val.isBlank()) {
                txtNumeroId.setStyle(FIELD_NORMAL);
            } else if (val.matches("[0-9 \\-]{5,20}")) {
                txtNumeroId.setStyle(FIELD_OK);
            } else {
                txtNumeroId.setStyle(FIELD_ERROR);
            }
        });

        txtNombres.textProperty().addListener((obs, old, val) ->
                txtNombres.setStyle(val.trim().length() >= 2 ? FIELD_OK : FIELD_NORMAL));

        txtApellidos.textProperty().addListener((obs, old, val) ->
                txtApellidos.setStyle(val.trim().length() >= 2 ? FIELD_OK : FIELD_NORMAL));

        txtCorreo.textProperty().addListener((obs, old, val) -> {
            if (val.isBlank()) {
                txtCorreo.setStyle(FIELD_NORMAL);
            } else if (EMAIL_PATTERN.matcher(val.trim()).matches()) {
                txtCorreo.setStyle(FIELD_OK);
            } else {
                txtCorreo.setStyle(FIELD_ERROR);
            }
        });

        txtTelefono.textProperty().addListener((obs, old, val) -> {
            if (val.isBlank()) {
                txtTelefono.setStyle(FIELD_NORMAL);
            } else if (TELEFONO_PATTERN.matcher(val.trim()).matches()) {
                txtTelefono.setStyle(FIELD_OK);
            } else {
                txtTelefono.setStyle(FIELD_ERROR);
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FOCUS — Borde resaltado al enfocar
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarFocusFields() {
        TextField[] campos = {txtNumeroId, txtNombres, txtApellidos, txtCorreo, txtTelefono};
        for (TextField tf : campos) {
            tf.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (tf.getStyle().contains("#1f2125")) {
                    tf.setStyle(isFocused
                            ? tf.getStyle().replace("-fx-border-color: #1f2125", "-fx-border-color: #555a40")
                            : tf.getStyle().replace("-fx-border-color: #555a40", "-fx-border-color: #1f2125"));
                }
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FOTO DE PERFIL
    // ═══════════════════════════════════════════════════════════════════════

    private void aplicarClipCircularFoto() {
        Circle clip = new Circle(76, 76, 76);
        imgFotoPerfil.setClip(clip);
    }

    @FXML
    private void handleCargarFoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar foto de perfil");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Imagenes", "*.jpg", "*.jpeg", "*.png")
        );

        Stage stage = (Stage) sideNav.getScene().getWindow();
        File archivo = fileChooser.showOpenDialog(stage);

        if (archivo != null) {
            if (archivo.length() > 5 * 1024 * 1024) {
                mostrarError("Archivo muy grande", "La imagen no debe superar los 5MB.");
                return;
            }
            archivoFotoSeleccionado = archivo;
            Image imagen = new Image(archivo.toURI().toString(), 152, 152, false, true);
            imgFotoPerfil.setImage(imagen);
            imgFotoPerfil.setFitWidth(152);
            imgFotoPerfil.setFitHeight(152);
            imgFotoPerfil.setPreserveRatio(false);

            lblFotoPlaceholder.setVisible(false);
            imgFotoPerfil.setOpacity(0);
            FadeTransition ft = new FadeTransition(Duration.millis(400), imgFotoPerfil);
            ft.setFromValue(0); ft.setToValue(1);
            ft.play();

            btnCargarFoto.setText("CAMBIAR IMAGEN");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GUARDAR
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleGuardar() {
        if (!validarFormulario()) return;

        btnGuardar.setText("GUARDADO \u2713");
        btnGuardar.setStyle(
                "-fx-background-color: #bdf4ff; -fx-background-radius: 8;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-text-fill: #001f24; -fx-cursor: hand; -fx-padding: 8 20 8 20;"
        );

        Timeline espera = new Timeline(
                new KeyFrame(Duration.millis(800), e -> navegarA("/fxml/GestionInstructores.fxml"))
        );
        espera.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VALIDACION DEL FORMULARIO
    // ═══════════════════════════════════════════════════════════════════════

    private boolean validarFormulario() {
        boolean valido = true;

        if (!txtNumeroId.getText().matches("[0-9 \\-]{5,20}")) {
            txtNumeroId.setStyle(FIELD_ERROR);
            valido = false;
        }

        if (txtNombres.getText().trim().length() < 2) {
            txtNombres.setStyle(FIELD_ERROR);
            valido = false;
        }

        if (txtApellidos.getText().trim().length() < 2) {
            txtApellidos.setStyle(FIELD_ERROR);
            valido = false;
        }

        if (!EMAIL_PATTERN.matcher(txtCorreo.getText().trim()).matches()) {
            txtCorreo.setStyle(FIELD_ERROR);
            valido = false;
        }

        if (!TELEFONO_PATTERN.matcher(txtTelefono.getText().trim()).matches()) {
            txtTelefono.setStyle(FIELD_ERROR);
            valido = false;
        }

        if (cmbEspecialidad.getValue() == null) {
            cmbEspecialidad.setStyle(
                    "-fx-background-color: #1a1c1f; -fx-background-radius: 8;" +
                            "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 8;" +
                            "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;"
            );
            valido = false;
        }

        if (dateFechaContratacion.getValue() == null) {
            dateFechaContratacion.setStyle(
                    "-fx-background-color: #1a1c1f; -fx-background-radius: 8;" +
                            "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 8;" +
                            "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;"
            );
            valido = false;
        }

        if (!valido) {
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
    //  HANDLERS — CANCELAR
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleCancelar() {
        boolean hayDatos = !txtNombres.getText().isBlank()
                || !txtApellidos.getText().isBlank()
                || !txtNumeroId.getText().isBlank();

        if (hayDatos) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                    "Tienes datos sin guardar. \u00bfDeseas salir de todas formas?",
                    ButtonType.YES, ButtonType.NO);
            confirm.setTitle("Cancelar registro");
            confirm.setHeaderText(null);
            confirm.showAndWait().ifPresent(btn -> {
                if (btn == ButtonType.YES) navegarA("/fxml/GestionInstructores.fxml");
            });
        } else {
            navegarA("/fxml/GestionInstructores.fxml");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — NAV
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handleNavDashboard()    { navegarA("/fxml/Dashboard.fxml"); }
    @FXML private void handleNavClientes()     { navegarA("/fxml/GestionClientes.fxml"); }
    @FXML private void handleNavInstructores() {  }
    @FXML private void handleNavMembresias()   {  }
    @FXML private void handleNavAI()           {  }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
            "Seguro que deseas cerrar sesion?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Cerrar sesion");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) navegarA("/fxml/Login.fxml");
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════

    private void navegarA(String rutaFxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(rutaFxml));
            Stage stage = (Stage) sideNav.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error de navegacion", "No se pudo cargar: " + rutaFxml);
        }
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
