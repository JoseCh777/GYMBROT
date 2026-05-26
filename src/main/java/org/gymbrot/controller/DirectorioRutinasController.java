package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.dao.RutinaDAO;
import org.gymbrot.model.Instructor;
import org.gymbrot.model.Rutina;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DirectorioRutinasController implements Initializable {

    @FXML private Button btnCerrar;
    @FXML private Button btnCancelar;

    @FXML private TextField txtBusqueda;
    @FXML private ComboBox<String> cmbObjetivo;

    @FXML private TableView<RutinaRow> tablaRutinas;
    @FXML private TableColumn<RutinaRow, String> colNombre;
    @FXML private TableColumn<RutinaRow, String> colObjetivo;
    @FXML private TableColumn<RutinaRow, String> colInstructor;
    @FXML private TableColumn<RutinaRow, String> colProgramacion;
    @FXML private TableColumn<RutinaRow, Button> colAcciones;

    @FXML private Label lblContador;

    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPag1;
    @FXML private Button btnPag2;
    @FXML private Button btnPag3;
    @FXML private Button btnPaginaSiguiente;

    private final ObservableList<RutinaRow> todasLasRutinas = FXCollections.observableArrayList();
    private FilteredList<RutinaRow> rutinasFiltradas;
    private int paginaActual = 1;
    private static final int FILAS_POR_PAGINA = 10;

    private final RutinaDAO rutinaDAO = new RutinaDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private StackPane wrapperStack;
    private Parent overlayRoot;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarComboBox();
        configurarTabla();
        cargarRutinas();
        configurarBuscador();
        configurarAnimaciones();
        aplicarFiltro();
    }

    private void configurarComboBox() {
        List<Rutina> todas = rutinaDAO.listarTodas();
        List<String> objetivos = todas.stream()
                .map(Rutina::getObjetivo)
                .filter(o -> o != null && !o.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        cmbObjetivo.getItems().add("Todos");
        cmbObjetivo.getItems().addAll(objetivos);
        cmbObjetivo.getSelectionModel().selectFirst();
        cmbObjetivo.setOnAction(e -> aplicarFiltro());
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colObjetivo.setCellValueFactory(new PropertyValueFactory<>("objetivo"));
        colInstructor.setCellValueFactory(new PropertyValueFactory<>("instructor"));
        colProgramacion.setCellValueFactory(new PropertyValueFactory<>("programacion"));
        colAcciones.setCellValueFactory(new PropertyValueFactory<>("btnEliminar"));

        colNombre.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #e2e2e6;");
        colObjetivo.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #e2e2e6;");
        colInstructor.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #e2e2e6;");
        colProgramacion.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #e2e2e6;");
        colAcciones.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #e2e2e6;");

        tablaRutinas.setFixedCellSize(48);
    }

    private String instructorNombre(String id) {
        Instructor inst = instructorDAO.buscarPorId(id);
        if (inst != null) return inst.getNombre() + " " + inst.getApellidos();
        return id;
    }

    private void cargarRutinas() {
        List<Rutina> lista = rutinaDAO.listarTodas();
        todasLasRutinas.clear();
        for (Rutina r : lista) {
            int idRutina = r.getIdRutina();
            String prog = r.getDiasSemana() != null ? r.getDiasSemana() : "";
            String obj = r.getObjetivo() != null ? r.getObjetivo() : "";
            todasLasRutinas.add(new RutinaRow(
                    idRutina, r.getNombre(), obj, instructorNombre(r.getIdInstructor()), prog,
                    () -> {
                        boolean ok = rutinaDAO.desactivar(idRutina);
                        if (ok) {
                            todasLasRutinas.removeIf(row -> row.idRutina == idRutina);
                            aplicarFiltro();
                        }
                    }));
        }
        rutinasFiltradas = new FilteredList<>(todasLasRutinas, p -> true);
    }

    private void configurarBuscador() {
        txtBusqueda.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltro());
    }

    private void configurarAnimaciones() {
        ScaleTransition grow = new ScaleTransition(Duration.millis(160), btnCerrar);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(160), btnCerrar);
        grow.setToX(1.15); grow.setToY(1.15);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btnCerrar.setOnMouseEntered(e -> grow.playFromStart());
        btnCerrar.setOnMouseExited(e -> shrink.playFromStart());

        ScaleTransition growC = new ScaleTransition(Duration.millis(160), btnCancelar);
        ScaleTransition shrinkC = new ScaleTransition(Duration.millis(160), btnCancelar);
        growC.setToX(1.04); growC.setToY(1.04);
        shrinkC.setToX(1.0); shrinkC.setToY(1.0);
        btnCancelar.setOnMouseEntered(e -> growC.playFromStart());
        btnCancelar.setOnMouseExited(e -> shrinkC.playFromStart());
    }

    public void setWrapperStack(StackPane wrapper, Parent overlayRoot) {
        this.wrapperStack = wrapper;
        this.overlayRoot = overlayRoot;
    }

    private void aplicarFiltro() {
        String busqueda = txtBusqueda.getText() == null ? "" : txtBusqueda.getText().toLowerCase().trim();
        String objetivo = cmbObjetivo.getValue() == null || cmbObjetivo.getValue().equals("Todos")
                ? null
                : cmbObjetivo.getValue();

        rutinasFiltradas.setPredicate(rutina -> {
            if (!busqueda.isEmpty()) {
                boolean coincide = rutina.nombre.get().toLowerCase().contains(busqueda)
                        || rutina.instructor.get().toLowerCase().contains(busqueda);
                if (!coincide) return false;
            }
            if (objetivo != null && !rutina.objetivo.get().equals(objetivo)) return false;
            return true;
        });

        paginaActual = 1;
        renderizarPagina();
        actualizarContador();
    }

    private void renderizarPagina() {
        tablaRutinas.getItems().clear();

        int desde = (paginaActual - 1) * FILAS_POR_PAGINA;
        int hasta = Math.min(desde + FILAS_POR_PAGINA, rutinasFiltradas.size());

        if (desde < rutinasFiltradas.size()) {
            var pagina = rutinasFiltradas.subList(desde, hasta);
            tablaRutinas.getItems().addAll(pagina);
        }

        actualizarBotonesPaginacion();
    }

    private void actualizarBotonesPaginacion() {
        int totalPaginas = Math.max(1, (int) Math.ceil((double) rutinasFiltradas.size() / FILAS_POR_PAGINA));
        btnPag1.setText(String.valueOf(totalPaginas >= 1 ? 1 : ""));
        btnPag2.setText(String.valueOf(totalPaginas >= 2 ? 2 : ""));
        btnPag3.setText(String.valueOf(totalPaginas >= 3 ? 3 : ""));

        String activa = "-fx-background-color: #D4FF00; -fx-background-radius: 6; -fx-font-family: 'Space Grotesk'; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #1a1c1f; -fx-cursor: hand;";
        String inactiva = "-fx-background-color: transparent; -fx-background-radius: 6; -fx-font-family: 'Space Grotesk'; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: #6b7280; -fx-cursor: hand;";

        btnPag1.setStyle(paginaActual == 1 ? activa : inactiva);
        btnPag2.setStyle(paginaActual == 2 ? activa : inactiva);
        btnPag3.setStyle(paginaActual == 3 ? activa : inactiva);
    }

    private void actualizarContador() {
        int total = rutinasFiltradas.size();
        int mostrando = Math.min(FILAS_POR_PAGINA, total);
        lblContador.setText("Mostrando " + mostrando + " de " + total + " rutinas registradas");
    }

    @FXML
    private void handlePaginaAnterior() {
        if (paginaActual > 1) { paginaActual--; renderizarPagina(); actualizarContador(); }
    }

    @FXML
    private void handlePagina1() { paginaActual = 1; renderizarPagina(); actualizarContador(); }

    @FXML
    private void handlePagina2() { paginaActual = 2; renderizarPagina(); actualizarContador(); }

    @FXML
    private void handlePagina3() { paginaActual = 3; renderizarPagina(); actualizarContador(); }

    @FXML
    private void handlePaginaSiguiente() {
        int totalPaginas = (int) Math.ceil((double) rutinasFiltradas.size() / FILAS_POR_PAGINA);
        if (paginaActual < totalPaginas) { paginaActual++; renderizarPagina(); actualizarContador(); }
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

    public static class RutinaRow {
        private final int idRutina;
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty objetivo;
        private final SimpleStringProperty instructor;
        private final SimpleStringProperty programacion;
        private final Button btnEliminar;

        public RutinaRow(int idRutina, String nombre, String objetivo, String instructor, String programacion, Runnable onEliminar) {
            this.idRutina = idRutina;
            this.nombre = new SimpleStringProperty(nombre);
            this.objetivo = new SimpleStringProperty(objetivo);
            this.instructor = new SimpleStringProperty(instructor);
            this.programacion = new SimpleStringProperty(programacion);

            this.btnEliminar = new Button("ELIMINAR");
            this.btnEliminar.setStyle("-fx-background-color: transparent; -fx-background-radius: 6;" +
                    "-fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6;" +
                    "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700;" +
                    "-fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 4 14 4 14;");
            this.btnEliminar.setOnAction(e -> {
                if (onEliminar != null) onEliminar.run();
            });
        }

        public String getNombre() { return nombre.get(); }
        public SimpleStringProperty nombreProperty() { return nombre; }

        public String getObjetivo() { return objetivo.get(); }
        public SimpleStringProperty objetivoProperty() { return objetivo; }

        public String getInstructor() { return instructor.get(); }
        public SimpleStringProperty instructorProperty() { return instructor; }

        public String getProgramacion() { return programacion.get(); }
        public SimpleStringProperty programacionProperty() { return programacion; }

        public Button getBtnEliminar() { return btnEliminar; }
    }
}
