package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

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

    private StackPane wrapperStack;
    private Parent overlayRoot;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarComboBoxes();
        configurarAnimaciones();
    }

    private void configurarComboBoxes() {
        cmbGrupoMuscular.getItems().addAll(
                "Pecho", "Espalda", "Hombros",
                "Biceps", "Triceps", "Piernas",
                "Gluteos", "Abdomen", "Cardio",
                "Cuerpo Completo"
        );
        cmbNivel.getItems().addAll(
                "Principiante", "Intermedio", "Avanzado"
        );
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

        StringBuilder resumen = new StringBuilder();
        resumen.append("Ejercicio creado:\n");
        resumen.append("Nombre: ").append(txtNombre.getText()).append("\n");
        resumen.append("Grupo Muscular: ").append(cmbGrupoMuscular.getValue()).append("\n");
        resumen.append("Nivel: ").append(cmbNivel.getValue()).append("\n");

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Ejercicio Guardado");
        info.setHeaderText(null);
        info.setContentText(resumen.toString());
        info.showAndWait();

        cerrarOverlay();
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
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validacion");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
