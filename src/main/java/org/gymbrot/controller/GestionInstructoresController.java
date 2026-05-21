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
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class GestionInstructoresController implements Initializable {

    // ─── SideNav ───────────────────────────────────────────────────────────
    @FXML private VBox sideNav;
    @FXML private Button navDashboard;
    @FXML private Button navClientes;
    @FXML private Button navInstructores;
    @FXML private Button navMembresias;
    @FXML private Button navAI;

    // ─── TopBar ────────────────────────────────────────────────────────────
    @FXML private HBox topBar;

    // ─── Stats ─────────────────────────────────────────────────────────────
    @FXML private Label lblTotalInstructores;
    @FXML private Label lblTendenciaInstructores;
    @FXML private Label lblSesionesHoy;
    @FXML private Rectangle dotSesiones;

    // ─── Toolbar ───────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Button btnFiltroTodos;
    @FXML private Button btnFiltroSuperior;
    @FXML private Button btnFiltroInferior;
    @FXML private Button btnFiltroCardio;
    @FXML private Button btnAgregarInstructor;

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

    // ─── Estado interno ────────────────────────────────────────────────────
    private final ObservableList<InstructorCard> todosLosInstructores = FXCollections.observableArrayList();
    private FilteredList<InstructorCard> instructoresFiltrados;
    private int paginaActual = 1;
    private static final int TARJETAS_POR_PAGINA = 8;
    private String filtroActual = "TODOS";

    // ═══════════════════════════════════════════════════════════════════════
    //  MODELO DE TARJETA
    // ═══════════════════════════════════════════════════════════════════════

    public static class InstructorCard {
        private final String id;
        private final String nombre;
        private final double rating;
        private final int sesiones;
        private final String especialidad;
        private final String estilo;
        private final String[] tags;
        private final String badgeStyle;

        public InstructorCard(String id, String nombre, double rating, int sesiones,
                              String especialidad, String estilo, String[] tags, String badgeStyle) {
            this.id = id;
            this.nombre = nombre;
            this.rating = rating;
            this.sesiones = sesiones;
            this.especialidad = especialidad;
            this.estilo = estilo;
            this.tags = tags;
            this.badgeStyle = badgeStyle;
        }

        public String getId() { return id; }
        public String getNombre() { return nombre; }
        public double getRating() { return rating; }
        public int getSesiones() { return sesiones; }
        public String getEspecialidad() { return especialidad; }
        public String getEstilo() { return estilo; }
        public String[] getTags() { return tags; }
        public String getBadgeStyle() { return badgeStyle; }
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
        configurarAnimacionesBotones();
        aplicarFiltro();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STATS
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarStats() {
        lblTotalInstructores.setText("24");
        lblTendenciaInstructores.setText("8% vs mes anterior");
        lblSesionesHoy.setText("18");
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

    private void cargarDatosMock() {
        todosLosInstructores.setAll(
                new InstructorCard("INS-001", "Marcus Thorne",   4.9, 128, "TREN SUPERIOR",  "Fuerza",     new String[]{"FUERZA", "HIPERTROFIA"},     "-fx-background-color: #D4FF00; -fx-text-fill: black;"),
                new InstructorCard("INS-002", "Elena Rodriguez", 5.0, 94,  "TREN INFERIOR",  "Piernas",    new String[]{"PIERNAS", "MOVILIDAD"},     "-fx-background-color: #00e3fd; -fx-text-fill: #001f24;"),
                new InstructorCard("INS-003", "Jaxson Vane",     4.8, 210, "CARDIO",         "HIIT",       new String[]{"HIIT", "RESISTENCIA"},      "-fx-background-color: #e9ddff; -fx-text-fill: #3c0090;"),
                new InstructorCard("INS-004", "Sarah Chen",      4.9, 156, "NUTRICION",      "Dieta",      new String[]{"DIETA", "SUPLEMENTOS"},     "-fx-background-color: #9cf0ff; -fx-text-fill: #001f24;"),
                new InstructorCard("INS-005", "Diego Rojas",     4.7, 82,  "TREN SUPERIOR",  "Funcional",  new String[]{"FUNCIONAL", "FUERZA"},      "-fx-background-color: #D4FF00; -fx-text-fill: black;"),
                new InstructorCard("INS-006", "Camila Torres",   4.9, 67,  "TREN INFERIOR",  "Potencia",   new String[]{"POTENCIA", "SALTOS"},       "-fx-background-color: #00e3fd; -fx-text-fill: #001f24;"),
                new InstructorCard("INS-007", "Liam O'Brien",    4.6, 193, "CARDIO",         "Boxeo",      new String[]{"BOXEO", "CONDICION"},       "-fx-background-color: #e9ddff; -fx-text-fill: #3c0090;"),
                new InstructorCard("INS-008", "Valentina Paz",   5.0, 112, "NUTRICION",      "Nutricion",  new String[]{"NUTRICION", "BIENESTAR"},   "-fx-background-color: #9cf0ff; -fx-text-fill: #001f24;"),
                new InstructorCard("INS-009", "Andres Marin",    4.8, 145, "TREN SUPERIOR",  "Powerlifting", new String[]{"POWERLIFTING", "PESAS"},  "-fx-background-color: #D4FF00; -fx-text-fill: black;"),
                new InstructorCard("INS-010", "Sofia Lagos",     4.7, 73,  "TREN INFERIOR",  "Yoga",       new String[]{"YOGA", "FLEXIBILIDAD"},    "-fx-background-color: #00e3fd; -fx-text-fill: #001f24;"),
                new InstructorCard("INS-011", "Kai Nakamura",    4.9, 201, "CARDIO",         "Crossfit",   new String[]{"CROSSFIT", "AGILIDAD"},     "-fx-background-color: #e9ddff; -fx-text-fill: #3c0090;"),
                new InstructorCard("INS-012", "Lucia Fernandez", 4.8, 89,  "NUTRICION",      "Suplementos", new String[]{"DIETA", "RENDIMIENTO"},     "-fx-background-color: #9cf0ff; -fx-text-fill: #001f24;")
        );

        instructoresFiltrados = new FilteredList<>(todosLosInstructores, p -> true);
        aplicarFiltro();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  FILTROS
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handleFiltroTodos()    { filtroActual = "TODOS";    actualizarEstilosFiltros(btnFiltroTodos);    aplicarFiltro(); }
    @FXML private void handleFiltroSuperior() { filtroActual = "SUPERIOR"; actualizarEstilosFiltros(btnFiltroSuperior); aplicarFiltro(); }
    @FXML private void handleFiltroInferior() { filtroActual = "INFERIOR"; actualizarEstilosFiltros(btnFiltroInferior); aplicarFiltro(); }
    @FXML private void handleFiltroCardio()   { filtroActual = "CARDIO";   actualizarEstilosFiltros(btnFiltroCardio);   aplicarFiltro(); }

    private void actualizarEstilosFiltros(Button activo) {
        Button[] filtros = {btnFiltroTodos, btnFiltroSuperior, btnFiltroInferior, btnFiltroCardio};
        for (Button btn : filtros) {
            if (btn == activo) {
                btn.setStyle("-fx-background-color: #D4FF00; -fx-background-radius: 8;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-text-fill: black; -fx-cursor: hand; -fx-padding: 8 16 8 16;");
            } else {
                btn.setStyle("-fx-background-color: #282a2d; -fx-background-radius: 8;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-text-fill: #9ca3af; -fx-cursor: hand; -fx-padding: 8 16 8 16;" +
                        "-fx-border-color: #333538; -fx-border-width: 1; -fx-border-radius: 8;");
            }
        }
    }

    private void aplicarFiltro() {
        String busqueda = searchField.getText() == null ? "" : searchField.getText().toLowerCase().trim();

        instructoresFiltrados.setPredicate(instructor -> {
            if (!busqueda.isEmpty()) {
                boolean coincideBusqueda = instructor.getNombre().toLowerCase().contains(busqueda)
                        || instructor.getId().toLowerCase().contains(busqueda)
                        || instructor.getEspecialidad().toLowerCase().contains(busqueda);
                if (!coincideBusqueda) return false;
            }

            if (filtroActual.equals("TODOS")) return true;
            return instructor.getEspecialidad().contains(filtroActual);
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

        Label badge = new Label(inst.getEspecialidad());
        badge.setAlignment(Pos.CENTER);
        badge.setStyle(inst.getBadgeStyle() +
                "-fx-background-radius: 4; -fx-font-family: 'Space Grotesk';" +
                "-fx-font-size: 10px; -fx-font-weight: 700; -fx-padding: 3 8 3 8;");
        StackPane.setAlignment(badge, Pos.TOP_LEFT);
        StackPane.setMargin(badge, new Insets(12, 0, 0, 12));
        imageStack.getChildren().add(badge);

        // Info
        VBox info = new VBox(10);
        info.setStyle("-fx-padding: 14 14 14 14;");

        Label lblNombre = new Label(inst.getNombre());
        lblNombre.setStyle("-fx-font-family: 'Lexend'; -fx-font-size: 16px;" +
                "-fx-font-weight: 700; -fx-text-fill: white;");

        HBox ratingRow = new HBox(6);
        ratingRow.setAlignment(Pos.CENTER_LEFT);
        Label lblRating = new Label("\u2605 " + inst.getRating());
        lblRating.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 13px;" +
                "-fx-font-weight: 700; -fx-text-fill: #D4FF00;");
        Label lblSesiones = new Label("(" + inst.getSesiones() + " sesiones)");
        lblSesiones.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #6b7280;");
        ratingRow.getChildren().addAll(lblRating, lblSesiones);

        HBox tagsRow = new HBox(6);
        for (String tag : inst.getTags()) {
            Label lblTag = new Label(tag);
            lblTag.setStyle("-fx-background-color: #1f2226; -fx-background-radius: 4;" +
                    "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px;" +
                    "-fx-font-weight: 700; -fx-text-fill: #9ca3af; -fx-padding: 3 8 3 8;");
            tagsRow.getChildren().add(lblTag);
        }

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

        Button btnRutina = new Button("VER RUTINA");
        btnRutina.setMaxWidth(Double.MAX_VALUE);
        btnRutina.setPrefHeight(36);
        btnRutina.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;" +
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

        info.getChildren().addAll(lblNombre, ratingRow, tagsRow, btnReservar, btnRutina);
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

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — NAV
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handleNavDashboard()    { navegarA("/fxml/Dashboard.fxml"); }
    @FXML private void handleNavClientes()     { navegarA("/fxml/GestionClientes.fxml"); }
    @FXML private void handleNavInstructores() { }
    @FXML private void handleNavMembresias()   { }
    @FXML private void handleNavAI()           { }

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
    //  ANIMACIONES
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navClientes, navMembresias, navAI};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navInstructores);
    }

    private void configurarAnimacionesBotones() {
        agregarHoverActivo(btnAgregarInstructor);
        agregarHoverInactivo(btnEmparejamiento);
        agregarHoverInactivo(btnFiltroTodos);
        agregarHoverInactivo(btnFiltroSuperior);
        agregarHoverInactivo(btnFiltroInferior);
        agregarHoverInactivo(btnFiltroCardio);
    }

    private void agregarHoverInactivo(Button btn) {
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03); grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btn.setOnMouseEntered(e -> grow.playFromStart());
        btn.setOnMouseExited(e  -> shrink.playFromStart());
        btn.setOnMousePressed(e -> { ScaleTransition p = new ScaleTransition(Duration.millis(80), btn); p.setToX(0.97); p.setToY(0.97); p.play(); });
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
