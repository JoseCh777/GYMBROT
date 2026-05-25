package org.gymbrot.controller;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.gymbrot.dao.*;
import org.gymbrot.model.*;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class GestionInstructoresController implements Initializable {

    // ─── SideNav ───────────────────────────────────────────────────────────
    @FXML private VBox sideNav;
    @FXML private Button navDashboard;
    @FXML private Button navClientes;
    @FXML private Button navInstructores;
    @FXML private Button navMembresias;
    @FXML private Button navAI;
    @FXML private Button navProgreso;

    // ─── TopBar ────────────────────────────────────────────────────────────
    @FXML private HBox topBar;

    // ─── Stats ─────────────────────────────────────────────────────────────
    @FXML private Label lblTotalInstructores;
    @FXML private Label lblTendenciaInstructores;
    @FXML private Label lblSesionesHoy;
    @FXML private Rectangle dotSesiones;

    // ─── Toolbar ───────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private ComboBox<String> cmbFiltroEspecialidad;
    @FXML private Button btnNuevoInstructor;
    @FXML private Button btnNuevaRutina1;
    @FXML private Button btnNuevoEjercicio1;
    @FXML private Button btnMostrarRutinas;
    @FXML private Button btnMostrarEjercicios;

    // ─── Grid ──────────────────────────────────────────────────────────────
    @FXML private FlowPane gridInstructores;

    // ─── Paginacion ────────────────────────────────────────────────────────
    @FXML private Label lblRegistros;
    @FXML private Button btnAnterior;
    @FXML private Button btnPag1;
    @FXML private Button btnPag2;
    @FXML private Button btnPag3;
    @FXML private Button btnSiguiente;

    // ─── AI ────────────────────────────────────────────────────────────────
    @FXML private Button btnEmparejamiento;
    @FXML private Label lblAIInsight;

    // ─── DAOs ──────────────────────────────────────────────────────────────
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final EspecialidadDAO especialidadDAO = new EspecialidadDAO();
    private final RutinaDAO rutinaDAO = new RutinaDAO();
    private final RutinaEjercicioDAO rutinaEjercicioDAO = new RutinaEjercicioDAO();

    // ─── Estado interno ────────────────────────────────────────────────────
    private final ObservableList<InstructorCard> todosLosInstructores = FXCollections.observableArrayList();
    private FilteredList<InstructorCard> instructoresFiltrados;
    private int paginaActual = 1;
    private static final int TARJETAS_POR_PAGINA = 8;
    private String filtroActual = "TODAS";

    // ═══════════════════════════════════════════════════════════════════════
    //  MODELO DE TARJETA
    // ═══════════════════════════════════════════════════════════════════════

    public record InstructorCard(String id, String nombre, String especialidad, String badgeStyle) {
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarAnimacionesNav();
        setNavActivo(navInstructores);
        cargarStats();
        cargarDatosMock();
        configurarBuscador();
        cargarAIInsight();
        iniciarAnimacionDotSesiones();
        configurarFiltroEspecialidad();
        configurarAnimacionesBotones();
        aplicarFiltro();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STATS
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarStats() {
        List<Instructor> todos = instructorDAO.listarTodos();
        lblTotalInstructores.setText(String.valueOf(todos.size()));
        lblTendenciaInstructores.setText(todos.size() + " registrados");
        lblSesionesHoy.setText("—");
    }

    private void iniciarAnimacionDotSesiones() {
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,        e -> dotSesiones.setOpacity(1.0)),
                new KeyFrame(Duration.millis(600),  e -> dotSesiones.setOpacity(0.2)),
                new KeyFrame(Duration.millis(1200), e -> dotSesiones.setOpacity(1.0))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DATOS MOCK
    // ═══════════════════════════════════════════════════════════════════════

    private String badgeColor(int idEsp) {
        return switch (idEsp) {
            case 1 -> "-fx-background-color: #D4FF00; -fx-text-fill: black;";
            case 2 -> "-fx-background-color: #00e3fd; -fx-text-fill: #001f24;";
            case 3 -> "-fx-background-color: #e9ddff; -fx-text-fill: #3c0090;";
            case 4 -> "-fx-background-color: #9cf0ff; -fx-text-fill: #001f24;";
            default -> "-fx-background-color: #D4FF00; -fx-text-fill: black;";
        };
    }

    private void cargarDatosMock() {
        List<Instructor> instructores = instructorDAO.listarTodos();
        List<Especialidad> especialidades = especialidadDAO.listarTodas();
        java.util.Map<Integer, String> nombreEsp = new java.util.HashMap<>();
        for (Especialidad e : especialidades) nombreEsp.put(e.getIdEspecialidad(), e.getNombre().toUpperCase());

        todosLosInstructores.clear();
        for (int i = 0; i < instructores.size(); i++) {
            Instructor inst = instructores.get(i);
            String id = inst.getNumeroIdentificacion();
            String nombreCompleto = inst.getNombre() + " " + inst.getApellidos();
            String espNombre = nombreEsp.getOrDefault(inst.getIdEspecialidad(), "GENERAL");

            todosLosInstructores.add(new InstructorCard(
                    id, nombreCompleto, espNombre, badgeColor(inst.getIdEspecialidad())
            ));
        }

        instructoresFiltrados = new FilteredList<>(todosLosInstructores, p -> true);
        aplicarFiltro();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FILTRO POR ESPECIALIDAD
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarFiltroEspecialidad() {
        cmbFiltroEspecialidad.getItems().add("TODAS");
        List<Especialidad> especialidades = especialidadDAO.listarTodas();
        for (Especialidad e : especialidades) {
            cmbFiltroEspecialidad.getItems().add(e.getNombre().toUpperCase());
        }
        cmbFiltroEspecialidad.getSelectionModel().selectFirst();
        cmbFiltroEspecialidad.setStyle(
                "-fx-background-color: #282a2d; -fx-background-radius: 8;" +
                        "-fx-border-color: #333538; -fx-border-width: 1; -fx-border-radius: 8;" +
                        "-fx-font-family: 'Inter'; -fx-font-size: 14px; -fx-text-fill: white;"
        );
    }

    @FXML
    private void handleFiltroEspecialidad() {
        String seleccion = cmbFiltroEspecialidad.getValue();
        filtroActual = seleccion != null ? seleccion : "TODAS";
        aplicarFiltro();
    }

    private void aplicarFiltro() {
        String busqueda = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        instructoresFiltrados.setPredicate(instructor -> {
            if (!busqueda.isEmpty()) {
                boolean coincideBusqueda = instructor.nombre().toLowerCase().contains(busqueda)
                        || instructor.id().toLowerCase().contains(busqueda)
                        || instructor.especialidad().toLowerCase().contains(busqueda);
                if (!coincideBusqueda) return false;
            }

            if (filtroActual.equals("TODAS")) return true;
            return instructor.especialidad().contains(filtroActual);
        });

        paginaActual = 1;
        renderizarPagina();
        actualizarLabelRegistros();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  BUSCADOR
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarBuscador() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> aplicarFiltro());
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  RENDERIZAR TARJETAS
    // ═══════════════════════════════════════════════════════════════════════

    private void renderizarPagina() {
        gridInstructores.getChildren().clear();

        int desde = (paginaActual - 1) * TARJETAS_POR_PAGINA;
        int hasta = Math.min(desde + TARJETAS_POR_PAGINA, instructoresFiltrados.size());
        var pagina = instructoresFiltrados.subList(Math.min(desde, instructoresFiltrados.size()), hasta);

        for (InstructorCard inst : pagina) {
            VBox card = crearTarjeta(inst);
            FadeTransition ft = new FadeTransition(Duration.millis(300), card);
            ft.setFromValue(0);
            ft.setToValue(1);
            gridInstructores.getChildren().add(card);
            ft.play();
        }

        actualizarBotonesPaginacion();
    }

    private VBox crearTarjeta(InstructorCard inst) {
        VBox card = new VBox(0);
        card.setPrefWidth(258);
        card.setStyle("-fx-background-color: #111316; -fx-background-radius: 12;" +
                "-fx-border-color: #282a2d; -fx-border-width: 1; -fx-border-radius: 12; -fx-cursor: hand;");

        // Imagen + badge
        StackPane imageStack = new StackPane();
        imageStack.setPrefHeight(180);
        imageStack.setPrefWidth(258);

        Rectangle placeholder = new Rectangle(258, 180);
        placeholder.setArcHeight(12);
        placeholder.setArcWidth(12);
        placeholder.setStyle("-fx-fill: #282a2d;");
        imageStack.getChildren().add(placeholder);

        ImageView img = new ImageView();
        img.setFitHeight(180);
        img.setFitWidth(258);
        img.setPreserveRatio(false);
        imageStack.getChildren().add(img);

        Label badge = new Label(inst.especialidad());
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(inst.badgeStyle() +
                "-fx-background-radius: 4; -fx-font-family: 'Space Grotesk';" +
                "-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 3 8 3 8;");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(12, 0, 0, 12));
        imageStack.getChildren().add(badge);

        // Info
        VBox info = new VBox(10);
        info.setStyle("-fx-padding: 14 14 14 14;");

        Label lblNombre = new Label(inst.nombre());
        lblNombre.setStyle("-fx-font-family: 'Lexend'; -fx-font-size: 16px;" +
                "-fx-font-weight: 700; -fx-text-fill: white;");

        Button btnReservar = new Button("RESERVAR SESION");
        btnReservar.setMaxWidth(Double.MAX_VALUE);
        btnReservar.setPrefHeight(36);
        btnReservar.setStyle("-fx-background-color: #D4FF00; -fx-background-radius: 8;" +
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                "-fx-font-weight: 700; -fx-text-fill: black; -fx-cursor: hand;");
        ScaleTransition grow = new ScaleTransition(Duration.millis(180), btnReservar);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btnReservar);
        grow.setToX(1.03); grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btnReservar.setOnMouseEntered(e -> grow.playFromStart());
        btnReservar.setOnMouseExited(e -> shrink.playFromStart());

        String instId = inst.id();
        Button btnRutina = new Button("VER RUTINA");
        btnRutina.setMaxWidth(Double.MAX_VALUE);
        btnRutina.setPrefHeight(36);
        btnRutina.setStyle("-fx-background-color: rgba(189,244,255,0.15); -fx-background-radius: 8;" +
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                "-fx-font-weight: 700; -fx-text-fill: #bdf4ff;" +
                "-fx-border-color: #bdf4ff; -fx-border-width: 1;" +
                "-fx-border-radius: 8; -fx-cursor: hand;");
        ScaleTransition grow2 = new ScaleTransition(Duration.millis(180), btnRutina);
        ScaleTransition shrink2 = new ScaleTransition(Duration.millis(180), btnRutina);
        grow2.setToX(1.03); grow2.setToY(1.03);
        shrink2.setToX(1.0); shrink2.setToY(1.0);
        btnRutina.setOnMouseEntered(e -> grow2.playFromStart());
        btnRutina.setOnMouseExited(e -> shrink2.playFromStart());
        btnRutina.setOnAction(e -> handleNuevaRutina(instId));

        // Acciones: Ver, Editar, Eliminar
        HBox acciones = new HBox(6);
        acciones.setAlignment(Pos.CENTER);

        Button btnVer = new Button("Ver");
        btnVer.setStyle("-fx-background-color: rgba(212,255,0,0.15); -fx-background-radius: 6;" +
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 9px; -fx-font-weight: 700;" +
                "-fx-text-fill: #D4FF00; -fx-border-color: #D4FF00; -fx-border-width: 1;" +
                "-fx-border-radius: 6; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        btnVer.setOnMouseEntered(e -> btnVer.setStyle(
                "-fx-background-color: #D4FF00; -fx-background-radius: 6;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 9px; -fx-font-weight: 700;" +
                        "-fx-text-fill: black; -fx-border-color: #D4FF00; -fx-border-width: 1;" +
                        "-fx-border-radius: 6; -fx-cursor: hand; -fx-padding: 4 8 4 8;"));
        btnVer.setOnMouseExited(e -> btnVer.setStyle(
                "-fx-background-color: rgba(212,255,0,0.15); -fx-background-radius: 6;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 9px; -fx-font-weight: 700;" +
                        "-fx-text-fill: #D4FF00; -fx-border-color: #D4FF00; -fx-border-width: 1;" +
                        "-fx-border-radius: 6; -fx-cursor: hand; -fx-padding: 4 8 4 8;"));
        btnVer.setOnAction(e -> handleVerInstructor(instId));

        Button btnEditar = new Button("Ed.");
        btnEditar.setStyle("-fx-background-color: rgba(96,165,250,0.15); -fx-background-radius: 6;" +
                "-fx-border-color: #60a5fa; -fx-border-width: 1; -fx-border-radius: 6;" +
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 9px; -fx-font-weight: 700;" +
                "-fx-text-fill: #60a5fa; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        btnEditar.setOnMouseEntered(e -> btnEditar.setStyle(
                "-fx-background-color: #60a5fa; -fx-background-radius: 6;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 9px; -fx-font-weight: 700;" +
                        "-fx-text-fill: black; -fx-border-color: #60a5fa; -fx-border-width: 1;" +
                        "-fx-border-radius: 6; -fx-cursor: hand; -fx-padding: 4 8 4 8;"));
        btnEditar.setOnMouseExited(e -> btnEditar.setStyle(
                "-fx-background-color: rgba(96,165,250,0.15); -fx-background-radius: 6;" +
                        "-fx-border-color: #60a5fa; -fx-border-width: 1; -fx-border-radius: 6;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 9px; -fx-font-weight: 700;" +
                        "-fx-text-fill: #60a5fa; -fx-cursor: hand; -fx-padding: 4 8 4 8;"));
        btnEditar.setOnAction(e -> handleEditarInstructor(instId));

        Button btnEliminar = new Button("El.");
        btnEliminar.setStyle("-fx-background-color: rgba(255,180,171,0.15); -fx-background-radius: 6;" +
                "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 6;" +
                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 9px; -fx-font-weight: 700;" +
                "-fx-text-fill: #ffb4ab; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
        btnEliminar.setOnMouseEntered(e -> btnEliminar.setStyle(
                "-fx-background-color: #ffb4ab; -fx-background-radius: 6;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 9px; -fx-font-weight: 700;" +
                        "-fx-text-fill: black; -fx-border-color: #ffb4ab; -fx-border-width: 1;" +
                        "-fx-border-radius: 6; -fx-cursor: hand; -fx-padding: 4 8 4 8;"));
        btnEliminar.setOnMouseExited(e -> btnEliminar.setStyle(
                "-fx-background-color: rgba(255,180,171,0.15); -fx-background-radius: 6;" +
                        "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 6;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 9px; -fx-font-weight: 700;" +
                        "-fx-text-fill: #ffb4ab; -fx-cursor: hand; -fx-padding: 4 8 4 8;"));
        btnEliminar.setOnAction(e -> handleEliminarInstructor(instId, inst.nombre()));

        acciones.getChildren().addAll(btnVer, btnEditar, btnEliminar);

        info.getChildren().addAll(lblNombre, acciones, btnReservar, btnRutina);
        card.getChildren().addAll(imageStack, info);
        return card;
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  PAGINACION
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handlePaginaAnterior() {
        if (paginaActual > 1) { paginaActual--; renderizarPagina(); actualizarLabelRegistros(); }
    }
    @FXML private void handlePagina1() { paginaActual = 1; renderizarPagina(); actualizarLabelRegistros(); }
    @FXML private void handlePagina2() { paginaActual = 2; renderizarPagina(); actualizarLabelRegistros(); }
    @FXML private void handlePagina3() { paginaActual = 3; renderizarPagina(); actualizarLabelRegistros(); }
    @FXML private void handlePaginaSiguiente() {
        int totalPaginas = (int) Math.ceil((double) instructoresFiltrados.size() / TARJETAS_POR_PAGINA);
        if (paginaActual < totalPaginas) { paginaActual++; renderizarPagina(); actualizarLabelRegistros(); }
    }

    private void actualizarBotonesPaginacion() {
        int totalPaginas = Math.max(1, (int) Math.ceil((double) instructoresFiltrados.size() / TARJETAS_POR_PAGINA));
        btnPag1.setText(String.valueOf(totalPaginas >= 1 ? 1 : ""));
        btnPag2.setText(String.valueOf(totalPaginas >= 2 ? 2 : ""));
        btnPag3.setText(String.valueOf(totalPaginas >= 3 ? 3 : ""));

        btnPag1.setStyle(paginaActual == 1
                ? "-fx-background-color: #282a2d; -fx-background-radius: 8; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: white; -fx-border-color: #D4FF00; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-background-radius: 8; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: white; -fx-border-color: #333538; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
        btnPag2.setStyle(paginaActual == 2
                ? "-fx-background-color: #282a2d; -fx-background-radius: 8; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: white; -fx-border-color: #D4FF00; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-background-radius: 8; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: white; -fx-border-color: #333538; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
        btnPag3.setStyle(paginaActual == 3
                ? "-fx-background-color: #282a2d; -fx-background-radius: 8; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: white; -fx-border-color: #D4FF00; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;"
                : "-fx-background-color: transparent; -fx-background-radius: 8; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: white; -fx-border-color: #333538; -fx-border-width: 1; -fx-border-radius: 8; -fx-cursor: hand;");
    }

    private void actualizarLabelRegistros() {
        int total = instructoresFiltrados.size();
        lblRegistros.setText("Mostrando " + Math.min(total, TARJETAS_POR_PAGINA) + " de " + total + " instructores");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AI INSIGHT
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarAIInsight() {
        lblAIInsight.setText(
                "He analizado los horarios con mayor demanda de instructores. " +
                "Sugiero emparejar a Marcus Thorne con los clientes de alta intensidad " +
                "para maximizar la retencion en horario pico."
        );
    }

    @FXML
    private void handleEmparejamientoIA() {
        mostrarInfo("Emparejamiento con IA",
                "Ejecutando algoritmo de emparejamiento inteligente...\n" +
                "Analizando perfiles de clientes e instructores.");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — TOOLBAR
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleAgregarInstructor() {
        navegarA("/fxml/NuevoInstructor.fxml");
    }

    @FXML
    private void handleNuevaRutina() {
        abrirNuevaRutina(null);
    }

    private void handleNuevaRutina(String instructorId) {
        abrirNuevaRutina(instructorId);
    }

    private void abrirNuevaRutina(String instructorId) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NuevaRutina.fxml"));
            Parent overlay = loader.load();
            NuevaRutinaController ctrl = loader.getController();

            if (instructorId != null) {
                ctrl.setInstructorSeleccionado(instructorId);

                List<Rutina> rutinas = rutinaDAO.buscarPorInstructor(instructorId);
                if (!rutinas.isEmpty()) {
                    Rutina primera = rutinas.get(0);
                    List<RutinaEjercicio> ejercicios = rutinaEjercicioDAO.listarPorRutina(primera.getIdRutina());
                    ctrl.cargarRutina(primera, ejercicios);
                }
            }

            Scene scene = sideNav.getScene();
            Parent rootActual = scene.getRoot();

            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(rootActual);
            wrapper.getChildren().add(overlay);

            ctrl.setWrapperStack(wrapper, overlay);

            scene.setRoot(wrapper);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir Nueva Rutina");
        }
    }

    @FXML
    private void handleNuevoEjercicio() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NuevoEjercicio.fxml"));
            Parent overlay = loader.load();
            NuevoEjercicioController ctrl = loader.getController();

            Scene scene = sideNav.getScene();
            Parent rootActual = scene.getRoot();

            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(rootActual);
            wrapper.getChildren().add(overlay);

            ctrl.setWrapperStack(wrapper, overlay);

            scene.setRoot(wrapper);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir Nuevo Ejercicio");
        }
    }

    @FXML
    private void handleMostrarRutinas() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DirectorioRutinas.fxml"));
            Parent overlay = loader.load();
            DirectorioRutinasController ctrl = loader.getController();

            Scene scene = sideNav.getScene();
            Parent rootActual = scene.getRoot();

            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(rootActual);
            wrapper.getChildren().add(overlay);

            ctrl.setWrapperStack(wrapper, overlay);

            scene.setRoot(wrapper);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir Directorio de Rutinas");
        }
    }

    @FXML
    private void handleMostrarEjercicios() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/DirectorioEjercicios.fxml"));
            Parent overlay = loader.load();
            DirectorioEjerciciosController ctrl = loader.getController();

            Scene scene = sideNav.getScene();
            Parent rootActual = scene.getRoot();

            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(rootActual);
            wrapper.getChildren().add(overlay);

            ctrl.setWrapperStack(wrapper, overlay);

            scene.setRoot(wrapper);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir Directorio de Ejercicios");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — TARJETAS INSTRUCTOR (Ver / Editar / Eliminar)
    // ═══════════════════════════════════════════════════════════════════════

    private void handleVerInstructor(String id) {
        try {
            Instructor inst = instructorDAO.buscarPorId(id);
            if (inst == null) return;

            Scene scene = sideNav.getScene();
            Parent rootActual = scene.getRoot();
            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(rootActual);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PerfilInstructor.fxml"));
            Parent overlay = loader.load();
            PerfilInstructorController ctrl = loader.getController();
            ctrl.setWrapperStack(wrapper, overlay);
            ctrl.setInstructor(inst);

            wrapper.getChildren().add(overlay);
            scene.setRoot(wrapper);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleEditarInstructor(String id) {
        try {
            Instructor inst = instructorDAO.buscarPorId(id);
            if (inst == null) return;

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NuevoInstructor.fxml"));
            Parent overlay = loader.load();
            NuevoInstructorController ctrl = loader.getController();
            ctrl.setInstructor(inst);

            Scene scene = sideNav.getScene();
            Parent rootActual = scene.getRoot();
            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(rootActual);
            wrapper.getChildren().add(overlay);
            scene.setRoot(wrapper);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleEliminarInstructor(String id, String nombre) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Eliminar a " + nombre + "?", ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar eliminacion");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                instructorDAO.eliminar(id);
                cargarDatosMock();
                aplicarFiltro();
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — NAV
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handleNavDashboard()    { navegarA("/fxml/Dashboard.fxml"); }
    @FXML private void handleNavClientes()     { navegarA("/fxml/GestionClientes.fxml"); }
    @FXML private void handleNavInstructores() { }
    @FXML private void handleNavMembresias()   { navegarA("/fxml/GestionMembresias.fxml"); }
    @FXML private void handleNavAI()           { navegarA("/fxml/GymbroAI.fxml"); }
    @FXML private void handleNavProgreso()     { }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Seguro que deseas cerrar sesion?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Cerrar sesion");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) navegarA("/fxml/login.fxml");
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMACIONES
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navClientes, navMembresias, navProgreso, navAI};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navInstructores);
    }

    private void configurarAnimacionesBotones() {
        agregarHoverActivo(btnNuevoInstructor);
        agregarHoverActivo(btnNuevaRutina1);
        agregarHoverActivo(btnNuevoEjercicio1);
        agregarHoverActivo(btnMostrarRutinas);
        agregarHoverActivo(btnMostrarEjercicios);
        agregarHoverInactivo(btnEmparejamiento);
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
        Button[] todos = {navDashboard, navClientes, navInstructores, navMembresias, navProgreso, navAI};
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
