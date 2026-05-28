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
import org.gymbrot.dao.*;
import org.gymbrot.model.*;
import org.gymbrot.util.AlertaPersonalizada;
import org.gymbrot.util.ValidacionUtil;

import java.net.URL;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

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

    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EjercicioDAO ejercicioDAO = new EjercicioDAO();
    private final RutinaDAO rutinaDAO = new RutinaDAO();

    private boolean modoEdicion = false;
    private Rutina rutinaActual;

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
        ValidacionUtil.soloNumeros(txtSeries);
        ValidacionUtil.soloNumeros(txtReps);
        ValidacionUtil.soloDecimales(txtCarga);
    }

    private void configurarComboBoxes() {
        List<Instructor> instructores = instructorDAO.listarDisponibles();
        if (instructores.isEmpty()) instructores = instructorDAO.listarTodos();
        for (Instructor i : instructores) {
            cmbInstructor.getItems().add(i.getNombre() + " " + i.getApellidos());
            cmbInstructor.getProperties().put(i.getNombre() + " " + i.getApellidos(), i.getNumeroIdentificacion());
        }

        List<Cliente> clientes = clienteDAO.listarTodos();
        for (Cliente c : clientes) {
            cmbCliente.getItems().add(c.getNombre() + " " + c.getApellidos());
            cmbCliente.getProperties().put(c.getNombre() + " " + c.getApellidos(), c.getNumeroIdentificacion());
        }

        List<Ejercicio> ejercicios = ejercicioDAO.listarTodos();
        for (Ejercicio e : ejercicios) {
            cmbEjercicio.getItems().add(e.getNombre());
            cmbEjercicio.getProperties().put(e.getNombre(), e.getIdEjercicio());
        }
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

    public void setInstructorSeleccionado(String instructorId) {
        for (String item : cmbInstructor.getItems()) {
            String id = (String) cmbInstructor.getProperties().get(item);
            if (id != null && id.equals(instructorId)) {
                cmbInstructor.setValue(item);
                break;
            }
        }
    }

    public void cargarRutina(Rutina rutina, List<RutinaEjercicio> ejercicios) {
        this.rutinaActual = rutina;
        this.modoEdicion = true;

        txtNombre.setText(rutina.getNombre());
        txtDescripcion.setText(rutina.getDescripcion());

        for (Toggle toggle : tgObjetivo.getToggles()) {
            RadioButton rb = (RadioButton) toggle;
            if (rb.getText().equalsIgnoreCase(rutina.getObjetivo())) {
                tgObjetivo.selectToggle(toggle);
                break;
            }
        }

        String nombreInstructor = null;
        for (String item : cmbInstructor.getItems()) {
            String id = (String) cmbInstructor.getProperties().get(item);
            if (id != null && id.equals(rutina.getIdInstructor())) {
                nombreInstructor = item;
                break;
            }
        }
        if (nombreInstructor != null) cmbInstructor.setValue(nombreInstructor);

        String nombreCliente = null;
        for (String item : cmbCliente.getItems()) {
            String id = (String) cmbCliente.getProperties().get(item);
            if (id != null && id.equals(rutina.getIdCliente())) {
                nombreCliente = item;
                break;
            }
        }
        if (nombreCliente != null) cmbCliente.setValue(nombreCliente);

        dpInicio.setValue(rutina.getFechaCreacion());
        dpFin.setValue(rutina.getFechaFin());

        if (rutina.getDiasSemana() != null) {
            String[] partes = rutina.getDiasSemana().split(",");
            for (String d : partes) {
                String dia = d.trim();
                if (dia.isEmpty()) continue;
                diasSeleccionados.add(dia);
                Button[] botones = {btnLunes, btnMartes, btnMiercoles, btnJueves, btnViernes, btnSabado, btnDomingo};
                String[] dias = {"LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"};
                for (int i = 0; i < dias.length; i++) {
                    if (dias[i].equals(dia)) {
                        botones[i].setStyle("-fx-background-color: #D4FF00; -fx-background-radius: 8;" +
                                "-fx-border-color: rgba(212,255,0,0.5); -fx-border-width: 1;" +
                                "-fx-border-radius: 8; -fx-font-family: 'Space Grotesk';" +
                                "-fx-font-size: 11px; -fx-font-weight: 700;" +
                                "-fx-text-fill: #1a1c1f; -fx-cursor: hand;");
                        break;
                    }
                }
            }
        }

        for (RutinaEjercicio re : ejercicios) {
            Ejercicio ej = ejercicioDAO.buscarPorId(re.getIdEjercicio());
            if (ej == null) continue;

            Button btnEliminar = new Button("X");
            btnEliminar.setStyle("-fx-background-color: transparent; -fx-text-fill: #ef4444;" +
                    "-fx-font-family: 'Space Grotesk'; -fx-font-size: 12px; -fx-font-weight: 700;" +
                    "-fx-cursor: hand; -fx-padding: 4 8 4 8;");
            btnEliminar.setOnAction(e -> ejerciciosList.removeIf(row -> row.getNombre().equals(ej.getNombre())));

            String carga = re.getNotasInstructor() != null ? re.getNotasInstructor() : ej.getSeries() + " x " + ej.getRepeticiones();
            ejerciciosList.add(new EjercicioRow(ej.getNombre(), ej.getSeries(), ej.getRepeticiones(), carga, btnEliminar));
        }
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
        if (cmbCliente.getValue() == null) {
            mostrarAlerta("Selecciona un cliente");
            return;
        }
        if (cmbInstructor.getValue() == null) {
            mostrarAlerta("Selecciona un instructor");
            return;
        }
        if (ejerciciosList.isEmpty()) {
            mostrarAlerta("Agrega al menos un ejercicio");
            return;
        }

        RadioButton selected = (RadioButton) tgObjetivo.getSelectedToggle();
        String objetivo = selected.getText();

        Rutina rutina;
        boolean guardado;

        if (modoEdicion && rutinaActual != null) {
            rutina = rutinaActual;
            rutina.setNombre(txtNombre.getText().trim());
            rutina.setDescripcion(txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : null);
            rutina.setObjetivo(objetivo);
            rutina.setIdInstructor((String) cmbInstructor.getProperties().get(cmbInstructor.getValue()));
            rutina.setIdCliente((String) cmbCliente.getProperties().get(cmbCliente.getValue()));
            rutina.setFechaCreacion(dpInicio.getValue() != null ? dpInicio.getValue() : LocalDate.now());
            rutina.setFechaFin(dpFin.getValue());
            rutina.setDiasSemana(String.join(", ", diasSeleccionados));
            guardado = rutinaDAO.actualizar(rutina);
        } else {
            rutina = new Rutina();
            rutina.setNombre(txtNombre.getText().trim());
            rutina.setDescripcion(txtDescripcion.getText() != null ? txtDescripcion.getText().trim() : null);
            rutina.setObjetivo(objetivo);
            rutina.setIdInstructor((String) cmbInstructor.getProperties().get(cmbInstructor.getValue()));
            rutina.setIdCliente((String) cmbCliente.getProperties().get(cmbCliente.getValue()));
            rutina.setFechaCreacion(dpInicio.getValue() != null ? dpInicio.getValue() : LocalDate.now());
            rutina.setFechaFin(dpFin.getValue());
            rutina.setDiasSemana(String.join(", ", diasSeleccionados));
            guardado = rutinaDAO.insertar(rutina);
        }

        if (guardado) {
            AlertaPersonalizada.exito("Rutina Guardada", "La rutina se guardo correctamente.");
            cerrarOverlay();
        } else {
            mostrarAlerta("Error al guardar la rutina. Intenta de nuevo.");
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
