package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.gymbrot.dao.EjercicioDAO;
import org.gymbrot.model.Ejercicio;
import org.gymbrot.util.AlertaPersonalizada;
import org.gymbrot.util.ValidacionUtil;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class NuevoEjercicioController implements Initializable {

    @FXML private Button btnCerrar;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    @FXML private TextField txtNombre;
    @FXML private ComboBox<String> cmbGrupoMuscular;
    @FXML private ComboBox<String> cmbNivel;
    @FXML private TextArea txtDescripcion;
    @FXML private TextField txtRecursoUrl;

    @FXML private Region progressBar;

    private final EjercicioDAO ejercicioDAO = new EjercicioDAO();
    private StackPane wrapperStack;
    private Parent overlayRoot;
    private boolean modoEdicion = false;
    private int idEjercicio;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarComboBoxes();
        configurarAnimaciones();
        ValidacionUtil.soloLetrasYNumeros(txtNombre);
    }

    private void configurarComboBoxes() {
        List<Ejercicio> todos = ejercicioDAO.listarTodos();
        List<String> grupos = todos.stream()
                .map(Ejercicio::getGrupoMuscular)
                .filter(g -> g != null && !g.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (grupos.isEmpty()) {
            grupos = List.of("Pecho", "Espalda", "Hombros", "Biceps", "Triceps",
                    "Piernas", "Gluteos", "Abdomen", "Cardio", "Cuerpo Completo");
        }
        cmbGrupoMuscular.getItems().addAll(grupos);

        List<String> niveles = todos.stream()
                .map(Ejercicio::getNivel)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        if (niveles.isEmpty()) {
            niveles = List.of("PRINCIPIANTE", "INTERMEDIO", "AVANZADO");
        }
        cmbNivel.getItems().addAll(niveles);
    }

    private void configurarAnimaciones() {
        ScaleTransition grow = new ScaleTransition(Duration.millis(160), btnGuardar);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(160), btnGuardar);
        grow.setToX(1.04); grow.setToY(1.04);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btnGuardar.setOnMouseEntered(e -> grow.playFromStart());
        btnGuardar.setOnMouseExited(e -> shrink.playFromStart());

        ScaleTransition growC = new ScaleTransition(Duration.millis(160), btnCerrar);
        ScaleTransition shrinkC = new ScaleTransition(Duration.millis(160), btnCerrar);
        growC.setToX(1.15); growC.setToY(1.15);
        shrinkC.setToX(1.0); shrinkC.setToY(1.0);
        btnCerrar.setOnMouseEntered(e -> growC.playFromStart());
        btnCerrar.setOnMouseExited(e -> shrinkC.playFromStart());
    }

    public void setWrapperStack(StackPane wrapper, Parent overlayRoot) {
        this.wrapperStack = wrapper;
        this.overlayRoot = overlayRoot;
    }

    public void setEjercicio(Ejercicio ejercicio) {
        this.modoEdicion = true;
        this.idEjercicio = ejercicio.getIdEjercicio();
        txtNombre.setText(ejercicio.getNombre());
        cmbGrupoMuscular.setValue(ejercicio.getGrupoMuscular());
        cmbNivel.setValue(ejercicio.getNivel());
        txtDescripcion.setText(ejercicio.getDescripcion());
        txtRecursoUrl.setText(ejercicio.getRecursoUrl());
    }

    @FXML
    private void handleGuardar() {
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            mostrarAlerta("El nombre del ejercicio es obligatorio");
            return;
        }
        if (cmbGrupoMuscular.getValue() == null) {
            mostrarAlerta("Selecciona un grupo muscular");
            return;
        }
        if (cmbNivel.getValue() == null) {
            mostrarAlerta("Selecciona un nivel de dificultad");
            return;
        }

        Ejercicio e = new Ejercicio();
        e.setNombre(txtNombre.getText().trim());
        e.setGrupoMuscular(cmbGrupoMuscular.getValue());
        e.setNivel(cmbNivel.getValue());
        e.setDescripcion(txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : null);
        e.setRecursoUrl(txtRecursoUrl.getText() != null ? txtRecursoUrl.getText().trim() : null);

        boolean guardado;
        if (modoEdicion) {
            e.setIdEjercicio(idEjercicio);
            guardado = ejercicioDAO.actualizar(e);
        } else {
            guardado = ejercicioDAO.insertar(e);
        }
        if (guardado) {
            AlertaPersonalizada.exito("Ejercicio Guardado", "El ejercicio se guardo correctamente.");
            cerrarOverlay();
        } else {
            mostrarAlerta("Error al guardar el ejercicio. Intenta de nuevo.");
        }
    }

    @FXML
    private void handleCerrar() {
        cerrarOverlay();
    }

    private void cerrarOverlay() {
        if (wrapperStack != null && overlayRoot != null) {
            wrapperStack.getChildren().remove(overlayRoot);
        }
    }

    private void mostrarAlerta(String mensaje) {
        AlertaPersonalizada.error("Validacion", mensaje);
    }
}
