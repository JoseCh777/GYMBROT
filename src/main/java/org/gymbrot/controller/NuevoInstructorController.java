package org.gymbrot.controller;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

/**
 * NuevoInstructorController
 *
 * Tablas Oracle que alimentan esta vista:
 *  - USUARIOS         → INSERT nombre, apellidos, correo, telefono, tipo_usuario='INSTRUCTOR'
 *  - INSTRUCTORES     → INSERT numero_identificacion, id_especialidad, fecha_contratacion, disponibilidad
 *  - ESPECIALIDADES   → SELECT para poblar el ComboBox de especialidades
 *
 * TODO: cuando tengas el DAO, reemplaza los métodos mock por llamadas reales.
 */
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
    @FXML private TextField txtNumeroId;       // INSTRUCTORES.numero_identificacion
    @FXML private TextField txtNombres;        // USUARIOS.nombre
    @FXML private TextField txtApellidos;      // USUARIOS.apellidos
    @FXML private TextField txtCorreo;         // USUARIOS.correo
    @FXML private TextField txtTelefono;       // USUARIOS.telefono

    // ─── Formulario: Especialidad y Disponibilidad ──────────────────────
    @FXML private ComboBox<String> cmbEspecialidad;   // ESPECIALIDADES → INSTRUCTORES.id_especialidad
    @FXML private DatePicker dateFechaContratacion;   // INSTRUCTORES.fecha_contratacion
    @FXML private TextArea txtNotas;                  // INSTRUCTORES.disponibilidad (perfil)

    // ─── Foto de perfil ────────────────────────────────────────────────
    @FXML private ImageView imgFotoPerfil;
    @FXML private Label lblFotoPlaceholder;
    @FXML private Button btnCargarFoto;

    // ─── Estado de Registro ────────────────────────────────────────────
    @FXML private Button btnVerificarCredenciales;
    @FXML private Rectangle dotEstado;
    @FXML private Label lblEstadoRRHH;

    // ─── Estado interno ────────────────────────────────────────────────
    private File archivoFotoSeleccionado;

    // ═══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarAnimacionesNav();
        setNavActivo(navInstructores);
        cargarEspecialidades();
        configurarFechaContratacion();
        configurarAnimacionesBotones();
        iniciarAnimacionDotEstado();
        configurarValidacionCampos();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ESPECIALIDADES
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarEspecialidades() {
        // TODO: reemplazar con llamada al DAO
        // List<String> especialidades = especialidadDAO.listarNombres();
        // Query: SELECT nombre FROM ESPECIALIDADES ORDER BY nombre
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
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FECHA DE CONTRATACION
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarFechaContratacion() {
        // Por defecto hoy
        dateFechaContratacion.setValue(LocalDate.now());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VALIDACION DE CAMPOS
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarValidacionCampos() {
        // Resalta el borde en amarillo al hacer focus
        TextField[] campos = {txtNumeroId, txtNombres, txtApellidos, txtCorreo, txtTelefono};
        for (TextField campo : campos) {
            campo.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
                if (isFocused) {
                    campo.setStyle(campo.getStyle()
                        .replace("-fx-border-color: #1f2125", "-fx-border-color: #D4FF00"));
                } else {
                    campo.setStyle(campo.getStyle()
                        .replace("-fx-border-color: #D4FF00", "-fx-border-color: #1f2125"));
                }
            });
        }
        // TextArea tambien
        txtNotas.focusedProperty().addListener((obs, wasFocused, isFocused) -> {
            if (isFocused) {
                txtNotas.setStyle(txtNotas.getStyle()
                    .replace("-fx-border-color: #1f2125", "-fx-border-color: #D4FF00"));
            } else {
                txtNotas.setStyle(txtNotas.getStyle()
                    .replace("-fx-border-color: #D4FF00", "-fx-border-color: #1f2125"));
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FOTO DE PERFIL
    // ═══════════════════════════════════════════════════════════════════════

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
            // Verificar tamaño maximo 5MB
            if (archivo.length() > 5 * 1024 * 1024) {
                mostrarError("Archivo muy grande", "La imagen no debe superar los 5MB.");
                return;
            }
            archivoFotoSeleccionado = archivo;
            Image imagen = new Image(archivo.toURI().toString(), 152, 152, true, true);
            imgFotoPerfil.setImage(imagen);
            lblFotoPlaceholder.setVisible(false);
            lblFotoPlaceholder.setManaged(false);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VERIFICAR CREDENCIALES
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleVerificarCredenciales() {
        // TODO: integrar con sistema de verificacion de RRHH
        // Por ahora cambia el estado a "EN PROCESO"
        dotEstado.setStyle("-fx-fill: #bdf4ff;");
        lblEstadoRRHH.setText("EN PROCESO DE VERIFICACION");
        lblEstadoRRHH.setStyle(lblEstadoRRHH.getStyle()
            .replace("-fx-text-fill: #f59e0b", "-fx-text-fill: #bdf4ff"));

        mostrarInfo("Verificacion de Credenciales",
            "El proceso de verificacion ha sido iniciado.\n" +
            "RRHH revisara los antecedentes y certificaciones del instructor.");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  GUARDAR
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleGuardar() {
        if (!validarFormulario()) return;

        // TODO: reemplazar con llamada al DAO
        // Primero INSERT en USUARIOS:
        // INSERT INTO USUARIOS (numero_identificacion, tipo_identificacion, nombre, apellidos,
        //   telefono, correo, contrasena_hash, estado, fecha_registro, tipo_usuario)
        // VALUES (?, 'CC', ?, ?, ?, ?, ?, 'ACTIVO', SYSDATE, 'INSTRUCTOR')
        //
        // Luego INSERT en INSTRUCTORES:
        // INSERT INTO INSTRUCTORES (numero_identificacion, id_especialidad, disponibilidad, fecha_contratacion)
        // VALUES (?, (SELECT id_especialidad FROM ESPECIALIDADES WHERE nombre = ?), ?, ?)
        //
        // Si tiene foto: actualizar USUARIOS.foto_url con la ruta guardada

        System.out.println("Guardando instructor:");
        System.out.println("  ID:          " + txtNumeroId.getText());
        System.out.println("  Nombre:      " + txtNombres.getText() + " " + txtApellidos.getText());
        System.out.println("  Correo:      " + txtCorreo.getText());
        System.out.println("  Telefono:    " + txtTelefono.getText());
        System.out.println("  Especialidad: " + cmbEspecialidad.getValue());
        System.out.println("  Contratacion: " + dateFechaContratacion.getValue());
        System.out.println("  Notas:       " + txtNotas.getText());

        mostrarInfo("Instructor Registrado",
            "El instructor " + txtNombres.getText() + " " + txtApellidos.getText() +
            " ha sido registrado exitosamente.");

        // Navegar de regreso a la lista de instructores (o clientes segun flujo)
        // navegarA("/fxml/GestionInstructores.fxml");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  VALIDACION DEL FORMULARIO
    // ═══════════════════════════════════════════════════════════════════════

    private boolean validarFormulario() {
        StringBuilder errores = new StringBuilder();

        if (txtNumeroId.getText().trim().isEmpty())
            errores.append("- Numero de identificacion es obligatorio\n");
        if (txtNombres.getText().trim().isEmpty())
            errores.append("- Nombres son obligatorios\n");
        if (txtApellidos.getText().trim().isEmpty())
            errores.append("- Apellidos son obligatorios\n");
        if (txtCorreo.getText().trim().isEmpty() || !txtCorreo.getText().contains("@"))
            errores.append("- Email corporativo invalido\n");
        if (cmbEspecialidad.getValue() == null)
            errores.append("- Debes seleccionar una especialidad\n");
        if (dateFechaContratacion.getValue() == null)
            errores.append("- Fecha de contratacion es obligatoria\n");

        if (errores.length() > 0) {
            mostrarError("Campos incompletos", errores.toString());
            return false;
        }
        return true;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMACION DOT ESTADO (pulso ambar)
    // ═══════════════════════════════════════════════════════════════════════

    private void iniciarAnimacionDotEstado() {
        Timeline pulse = new Timeline(
            new KeyFrame(Duration.ZERO,        e -> dotEstado.setOpacity(1.0)),
            new KeyFrame(Duration.millis(700),  e -> dotEstado.setOpacity(0.2)),
            new KeyFrame(Duration.millis(1400), e -> dotEstado.setOpacity(1.0))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMACIONES NAV Y BOTONES
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navClientes, navMembresias, navAI};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navInstructores);
    }

    private void configurarAnimacionesBotones() {
        agregarHoverActivo(btnGuardar);
        agregarHoverInactivo(btnCancelar);
        agregarHoverInactivo(btnVerificarCredenciales);
        agregarHoverInactivo(btnCargarFoto);
    }

    private void agregarHoverInactivo(Button btn) {
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03); grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btn.setOnMouseEntered(e  -> grow.playFromStart());
        btn.setOnMouseExited(e   -> shrink.playFromStart());
        btn.setOnMousePressed(e  -> { ScaleTransition p = new ScaleTransition(Duration.millis(80), btn); p.setToX(0.97); p.setToY(0.97); p.play(); });
        btn.setOnMouseReleased(e -> { ScaleTransition r = new ScaleTransition(Duration.millis(80), btn); r.setToX(1.0); r.setToY(1.0); r.play(); });
    }

    private void agregarHoverActivo(Button btn) {
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03); grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btn.setOnMouseEntered(e  -> grow.playFromStart());
        btn.setOnMouseExited(e   -> shrink.playFromStart());
        btn.setOnMousePressed(e  -> { ScaleTransition p = new ScaleTransition(Duration.millis(80), btn); p.setToX(0.97); p.setToY(0.97); p.play(); });
        btn.setOnMouseReleased(e -> { ScaleTransition r = new ScaleTransition(Duration.millis(80), btn); r.setToX(1.0); r.setToY(1.0); r.play(); });
    }

    private void setNavActivo(Button activo) {
        Button[] todos = {navDashboard, navClientes, navInstructores, navMembresias, navAI};
        for (Button btn : todos) {
            if (btn == activo) {
                btn.setStyle("-fx-background-color: #D4FF00; -fx-background-radius: 8;" +
                             "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 700;" +
                             "-fx-text-fill: black; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;" +
                             "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 500;" +
                             "-fx-text-fill: #9ca3af; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — CANCELAR
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleCancelar() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
            "Descartar los cambios y volver?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Cancelar registro");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES)
                navegarA("/fxml/Dashboard.fxml");
        });
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
