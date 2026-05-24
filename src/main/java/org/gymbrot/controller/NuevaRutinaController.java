package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import javafx.util.converter.IntegerStringConverter;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;

public class NuevaRutinaController implements Initializable {

    @FXML private Button btnCerrar;

    @FXML private TextField txtNombre;
    @FXML private TextArea txtDescripcion;

    @FXML private ToggleGroup tgObjetivo;
    @FXML private RadioButton rbMasa;
    @FXML private RadioButton rbDefinicion;
    @FXML private RadioButton rbFuerza;
    @FXML private RadioButton rbResistencia;

    @FXML private ComboBox<String> cmbInstructor;
    @FXML private ComboBox<String> cmbCliente;
    @FXML private ComboBox<String> cmbEjercicio;

    @FXML private TextField txtSeries;
    @FXML private TextField txtReps;
    @FXML private TextField txtCarga;

    @FXML private DatePicker dpInicio;
    @FXML private DatePicker dpFin;

    @FXML private Button btnLunes;
    @FXML private Button btnMartes;
    @FXML private Button btnMiercoles;
    @FXML private Button btnJueves;
    @FXML private Button btnViernes;
    @FXML private Button btnSabado;
    @FXML private Button btnDomingo;

    @FXML private Button btnAgregarEjercicio;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    @FXML private TableView<EjercicioRow> tablaEjercicios;
    @FXML private TableColumn<EjercicioRow, String> colEjercicio;
    @FXML private TableColumn<EjercicioRow, Integer> colSeries;
    @FXML private TableColumn<EjercicioRow, Integer> colReps;
    @FXML private TableColumn<EjercicioRow, String> colCarga;
    @FXML private TableColumn<EjercicioRow, Button> colAccion;

    private final Set<String> diasSeleccionados = new HashSet<>();
    private final ObservableList<EjercicioRow> ejerciciosList = FXCollections.observableArrayList();
    private StackPane wrapperStack;
    private Parent overlayRoot;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarComboBoxes();
        configurarTabla();
        configurarAnimaciones();
        dpInicio.setValue(LocalDate.now());
        dpFin.setValue(LocalDate.now().plusMonths(1));
    }

    private void configurarComboBoxes() {
        cmbInstructor.getItems().addAll(
                "Marcus Thorne", "Elena Rodriguez", "Jaxson Vane",
                "Sarah Chen", "Diego Rojas", "Camila Torres"
        );
        cmbCliente.getItems().addAll(
                "Carlos Mendez", "Ana Lucia Perez", "Pedro Ramirez",
                "Maria Gomez", "Luis Torres", "Sofia Castro"
        );
        cmbEjercicio.getItems().addAll(
                "Press Banca", "Sentadilla", "Peso Muerto",
                "Dominadas", "Fondos", "Remo con Barra",
                "Press Militar", "Curl Biceps", "Extension Triceps",
                "Elevaciones Laterales", "Jalon al Pecho", "Prensa de Piernas"
        );
    }

    private void configurarTabla() {
        tablaEjercicios.setEditable(true);

        colEjercicio.setCellValueFactory(new PropertyValueFactory<>("nombre"));

        colSeries.setCellValueFactory(new PropertyValueFactory<>("series"));
        colSeries.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colSeries.setOnEditCommit(e -> {
            EjercicioRow row = e.getRowValue();
            row.setSeries(e.getNewValue() != null ? e.getNewValue() : 0);
        });

        colReps.setCellValueFactory(new PropertyValueFactory<>("repeticiones"));
        colReps.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        colReps.setOnEditCommit(e -> {
            EjercicioRow row = e.getRowValue();
            row.setRepeticiones(e.getNewValue() != null ? e.getNewValue() : 0);
        });

        colCarga.setCellValueFactory(new PropertyValueFactory<>("carga"));
        colCarga.setCellFactory(TextFieldTableCell.forTableColumn());
        colCarga.setOnEditCommit(e -> {
            EjercicioRow row = e.getRowValue();
            row.setCarga(e.getNewValue() != null ? e.getNewValue() : "0 kg");
        });

        colAccion.setCellValueFactory(new PropertyValueFactory<>("btnEliminar"));

        colEjercicio.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #e2e2e6;");
        colSeries.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: #e2e2e6;");
        colReps.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: #e2e2e6;");
        colCarga.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: #e2e2e6;");
        colAccion.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: #e2e2e6;");

        tablaEjercicios.setItems(ejerciciosList);
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

    @FXML
    private void handleDia(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        String dia = btn.getText();

        if (diasSeleccionados.contains(dia)) {
            diasSeleccionados.remove(dia);
            btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;" +
                    "-fx-border-color: rgba(255,255,255,0.1); -fx-border-width: 1;" +
                    "-fx-border-radius: 8; -fx-font-family: 'Space Grotesk';" +
                    "-fx-font-size: 11px; -fx-font-weight: 700;" +
                    "-fx-text-fill: #6b7280; -fx-cursor: hand;");
        } else {
            diasSeleccionados.add(dia);
            btn.setStyle("-fx-background-color: #D4FF00; -fx-background-radius: 8;" +
                    "-fx-border-color: rgba(212,255,0,0.5); -fx-border-width: 1;" +
                    "-fx-border-radius: 8; -fx-font-family: 'Space Grotesk';" +
                    "-fx-font-size: 11px; -fx-font-weight: 700;" +
                    "-fx-text-fill: #1a1c1f; -fx-cursor: hand;");
        }
    }

    @FXML
    private void handleAgregarEjercicio() {
        String ejercicio = cmbEjercicio.getValue();
        if (ejercicio == null || ejercicio.isEmpty()) {
            mostrarAlerta("Selecciona un ejercicio");
            return;
        }

        int series;
        int reps;
        String carga;

        try {
            series = Integer.parseInt(txtSeries.getText().trim());
            reps = Integer.parseInt(txtReps.getText().trim());
            carga = txtCarga.getText().trim() + " kg";
        } catch (NumberFormatException e) {
            mostrarAlerta("Series y Reps deben ser n\u00fameros v\u00e1lidos");
            return;
        }

        Button btnEliminar = new Button("X");
        btnEliminar.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444;" +
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 12px; -fx-font-weight: 700;" +
                "-fx-cursor: hand; -fx-padding: 4 8 4 8;");
        btnEliminar.setOnAction(e -> ejerciciosList.removeIf(row -> row.getNombre().equals(ejercicio)));

        ejerciciosList.add(new EjercicioRow(ejercicio, series, reps, carga, btnEliminar));
        cmbEjercicio.getSelectionModel().clearSelection();
    }

    public void setWrapperStack(StackPane wrapper, Parent overlayRoot) {
        this.wrapperStack = wrapper;
        this.overlayRoot = overlayRoot;
    }

    @FXML
    private void handleGuardar() {
        if (txtNombre.getText() == null || txtNombre.getText().trim().isEmpty()) {
            mostrarAlerta("El nombre de la rutina es obligatorio");
            return;
        }
        if (tgObjetivo.getSelectedToggle() == null) {
            mostrarAlerta("Selecciona un objetivo");
            return;
        }
        if (ejerciciosList.isEmpty()) {
            mostrarAlerta("Agrega al menos un ejercicio");
            return;
        }

        RadioButton selected = (RadioButton) tgObjetivo.getSelectedToggle();
        String objetivo = selected.getText();

        StringBuilder resumen = new StringBuilder();
        resumen.append("Rutina creada:\n");
        resumen.append("Nombre: ").append(txtNombre.getText()).append("\n");
        resumen.append("Objetivo: ").append(objetivo).append("\n");
        resumen.append("Ejercicios: ").append(ejerciciosList.size()).append("\n");
        resumen.append("Dias: ").append(String.join(", ", diasSeleccionados)).append("\n");

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Rutina Guardada");
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

    public static class EjercicioRow {
        private final SimpleStringProperty nombre;
        private final SimpleIntegerProperty series;
        private final SimpleIntegerProperty repeticiones;
        private final SimpleStringProperty carga;
        private final Button btnEliminar;

        public EjercicioRow(String nombre, int series, int repeticiones, String carga, Button btnEliminar) {
            this.nombre = new SimpleStringProperty(nombre);
            this.series = new SimpleIntegerProperty(series);
            this.repeticiones = new SimpleIntegerProperty(repeticiones);
            this.carga = new SimpleStringProperty(carga);
            this.btnEliminar = btnEliminar;
        }

        public String getNombre() { return nombre.get(); }
        public SimpleStringProperty nombreProperty() { return nombre; }

        public int getSeries() { return series.get(); }
        public void setSeries(int series) { this.series.set(series); }
        public SimpleIntegerProperty seriesProperty() { return series; }

        public int getRepeticiones() { return repeticiones.get(); }
        public void setRepeticiones(int reps) { this.repeticiones.set(reps); }
        public SimpleIntegerProperty repeticionesProperty() { return repeticiones; }

        public String getCarga() { return carga.get(); }
        public void setCarga(String carga) { this.carga.set(carga); }
        public SimpleStringProperty cargaProperty() { return carga; }

        public Button getBtnEliminar() { return btnEliminar; }
    }
}
