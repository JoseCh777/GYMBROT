package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.gymbrot.dao.EjercicioDAO;
import org.gymbrot.model.Ejercicio;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DirectorioEjerciciosController implements Initializable {

    @FXML private Button btnCerrar;
    @FXML private Button btnCancelar;

    @FXML private TextField txtBusqueda;
    @FXML private ComboBox<String> cmbFiltroGrupo;
    @FXML private ComboBox<String> cmbFiltroNivel;

    @FXML private TableView<EjercicioRow> tablaEjercicios;
    @FXML private TableColumn<EjercicioRow, String> colNombre;
    @FXML private TableColumn<EjercicioRow, String> colGrupoMuscular;
    @FXML private TableColumn<EjercicioRow, String> colNivel;
    @FXML private TableColumn<EjercicioRow, String> colDescripcion;
    @FXML private TableColumn<EjercicioRow, HBox> colAcciones;

    @FXML private Label lblContador;

    @FXML private Button btnPaginaAnterior;
    @FXML private Button btnPag1;
    @FXML private Button btnPag2;
    @FXML private Button btnPag3;
    @FXML private Button btnPaginaSiguiente;

    private final ObservableList<EjercicioRow> todosLosEjercicios = FXCollections.observableArrayList();
    private FilteredList<EjercicioRow> ejerciciosFiltrados;
    private int paginaActual = 1;
    private static final int FILAS_POR_PAGINA = 10;

    private StackPane wrapperStack;
    private Parent overlayRoot;

    private final EjercicioDAO ejercicioDAO = new EjercicioDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarComboBoxes();
        configurarTabla();
        cargarEjercicios();
        configurarBuscador();
        configurarAnimaciones();
        aplicarFiltro();
    }

    private void configurarComboBoxes() {
        List<Ejercicio> todos = ejercicioDAO.listarTodos();
        List<String> grupos = todos.stream()
                .map(Ejercicio::getGrupoMuscular)
                .filter(g -> g != null && !g.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        cmbFiltroGrupo.getItems().add("Todos");
        cmbFiltroGrupo.getItems().addAll(grupos);
        cmbFiltroGrupo.getSelectionModel().selectFirst();
        cmbFiltroGrupo.setOnAction(e -> aplicarFiltro());

        List<String> niveles = todos.stream()
                .map(Ejercicio::getNivel)
                .filter(n -> n != null && !n.isBlank())
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        cmbFiltroNivel.getItems().add("Todos");
        cmbFiltroNivel.getItems().addAll(niveles);
        cmbFiltroNivel.getSelectionModel().selectFirst();
        cmbFiltroNivel.setOnAction(e -> aplicarFiltro());
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        colGrupoMuscular.setCellValueFactory(new PropertyValueFactory<>("grupoMuscular"));
        colNivel.setCellValueFactory(new PropertyValueFactory<>("nivel"));
        colDescripcion.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        colAcciones.setCellValueFactory(new PropertyValueFactory<>("accionesBox"));

        colNombre.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #e2e2e6;");
        colGrupoMuscular.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #e2e2e6;");
        colNivel.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #e2e2e6;");
        colDescripcion.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #e2e2e6;");
        colAcciones.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #e2e2e6;");

        tablaEjercicios.setFixedCellSize(48);
    }

    private void cargarEjercicios() {
        List<Ejercicio> lista = ejercicioDAO.listarTodos();
        todosLosEjercicios.clear();
        for (Ejercicio e : lista) {
            int idEjercicio = e.getIdEjercicio();
            String nivel = e.getNivel() != null ? e.getNivel().substring(0, 1).toUpperCase()
                    + e.getNivel().substring(1).toLowerCase() : "";
            String desc = e.getDescripcion() != null ? e.getDescripcion() : "";
            todosLosEjercicios.add(new EjercicioRow(
                    idEjercicio, e.getNombre(), e.getGrupoMuscular(), nivel, desc,
                    () -> abrirEditarEjercicio(idEjercicio),
                    () -> {
                        boolean ok = ejercicioDAO.desactivar(idEjercicio);
                        if (ok) {
                            todosLosEjercicios.removeIf(row -> row.idEjercicio == idEjercicio);
                            aplicarFiltro();
                        }
                    }));
        }
        ejerciciosFiltrados = new FilteredList<>(todosLosEjercicios, p -> true);
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
        String grupo = cmbFiltroGrupo.getValue() == null || cmbFiltroGrupo.getValue().equals("Todos")
                ? null : cmbFiltroGrupo.getValue();
        String nivel = cmbFiltroNivel.getValue() == null || cmbFiltroNivel.getValue().equals("Todos")
                ? null : cmbFiltroNivel.getValue();

        ejerciciosFiltrados.setPredicate(ej -> {
            if (!busqueda.isEmpty()) {
                boolean coincide = ej.nombre.get().toLowerCase().contains(busqueda)
                        || ej.grupoMuscular.get().toLowerCase().contains(busqueda);
                if (!coincide) return false;
            }
            if (grupo != null && !ej.grupoMuscular.get().equals(grupo)) return false;
            if (nivel != null && !ej.nivel.get().equals(nivel)) return false;
            return true;
        });

        paginaActual = 1;
        renderizarPagina();
        actualizarContador();
    }

    private void renderizarPagina() {
        tablaEjercicios.getItems().clear();

        int desde = (paginaActual - 1) * FILAS_POR_PAGINA;
        int hasta = Math.min(desde + FILAS_POR_PAGINA, ejerciciosFiltrados.size());

        if (desde < ejerciciosFiltrados.size()) {
            var pagina = ejerciciosFiltrados.subList(desde, hasta);
            tablaEjercicios.getItems().addAll(pagina);
        }

        actualizarBotonesPaginacion();
    }

    private void actualizarBotonesPaginacion() {
        int totalPaginas = Math.max(1, (int) Math.ceil((double) ejerciciosFiltrados.size() / FILAS_POR_PAGINA));
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
        int total = ejerciciosFiltrados.size();
        int mostrando = Math.min(FILAS_POR_PAGINA, total);
        lblContador.setText("Mostrando " + mostrando + " de " + total + " ejercicios registrados");
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
        int totalPaginas = (int) Math.ceil((double) ejerciciosFiltrados.size() / FILAS_POR_PAGINA);
        if (paginaActual < totalPaginas) { paginaActual++; renderizarPagina(); actualizarContador(); }
    }

    private void abrirEditarEjercicio(int idEjercicio) {
        try {
            Ejercicio ejercicio = ejercicioDAO.buscarPorId(idEjercicio);
            if (ejercicio == null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NuevoEjercicio.fxml"));
            Parent overlay = loader.load();
            NuevoEjercicioController ctrl = loader.getController();
            ctrl.setEjercicio(ejercicio);

            Scene scene = wrapperStack.getScene();
            Parent rootActual = scene.getRoot();

            StackPane newWrapper = new StackPane();
            newWrapper.getChildren().add(rootActual);
            newWrapper.getChildren().add(overlay);

            ctrl.setWrapperStack(newWrapper, overlay);

            scene.setRoot(newWrapper);
        } catch (IOException e) {
            e.printStackTrace();
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

    public static class EjercicioRow {
        private final int idEjercicio;
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty grupoMuscular;
        private final SimpleStringProperty nivel;
        private final SimpleStringProperty descripcion;
        private final Button btnEditar;
        private final Button btnEliminar;
        private final HBox accionesBox;

        public EjercicioRow(int idEjercicio, String nombre, String grupoMuscular, String nivel, String descripcion, Runnable onEditar, Runnable onEliminar) {
            this.idEjercicio = idEjercicio;
            this.nombre = new SimpleStringProperty(nombre);
            this.grupoMuscular = new SimpleStringProperty(grupoMuscular);
            this.nivel = new SimpleStringProperty(nivel);
            this.descripcion = new SimpleStringProperty(descripcion);

            this.btnEditar = new Button("EDITAR");
            this.btnEditar.setStyle("-fx-background-color: transparent; -fx-background-radius: 6;" +
                    "-fx-border-color: #D4FF00; -fx-border-width: 1; -fx-border-radius: 6;" +
                    "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700;" +
                    "-fx-text-fill: #D4FF00; -fx-cursor: hand; -fx-padding: 4 14 4 14;");
            this.btnEditar.setOnAction(e -> {
                if (onEditar != null) onEditar.run();
            });

            this.btnEliminar = new Button("ELIMINAR");
            this.btnEliminar.setStyle("-fx-background-color: transparent; -fx-background-radius: 6;" +
                    "-fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6;" +
                    "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700;" +
                    "-fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 4 14 4 14;");
            this.btnEliminar.setOnAction(e -> {
                if (onEliminar != null) onEliminar.run();
            });

            this.accionesBox = new HBox(6);
            this.accionesBox.setAlignment(javafx.geometry.Pos.CENTER);
            this.accionesBox.getChildren().addAll(btnEditar, btnEliminar);
        }

        public String getNombre() { return nombre.get(); }
        public SimpleStringProperty nombreProperty() { return nombre; }

        public String getGrupoMuscular() { return grupoMuscular.get(); }
        public SimpleStringProperty grupoMuscularProperty() { return grupoMuscular; }

        public String getNivel() { return nivel.get(); }
        public SimpleStringProperty nivelProperty() { return nivel; }

        public String getDescripcion() { return descripcion.get(); }
        public SimpleStringProperty descripcionProperty() { return descripcion; }

        public HBox getAccionesBox() { return accionesBox; }
    }
}
