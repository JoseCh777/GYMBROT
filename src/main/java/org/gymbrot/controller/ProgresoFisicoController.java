package org.gymbrot.controller;

import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.gymbrot.Main;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.util.AlertaPersonalizada;
import org.gymbrot.dao.ProgresoDAO;
import org.gymbrot.model.Cliente;
import org.gymbrot.model.Progreso;
import org.gymbrot.service.ProgresoService;
import org.gymbrot.util.ValidacionUtil;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class    ProgresoFisicoController implements Initializable {

    public static String pendingClienteId;

    private static final int PAGE_SIZE = 8;

    @FXML
    private VBox sideNav;
    @FXML
    private Button navDashboard, navClientes, navInstructores, navMembresias, navFinanzas, navProgreso, navAI, navCitas;

    @FXML
    private HBox topBar;

    @FXML
    private ImageView imgMiembro;
    @FXML
    private Label lblNombreMiembro, lblIdMiembro, lblPesoActual, lblGrasaActual, lblInicialesAvatar;

    @FXML
    private TextField txtPeso, txtAltura, txtIMC, txtGrasa, txtMusculo;
    @FXML
    private Button btnGuardarProgreso;

    @FXML
    private TextField txtBuscarCliente;
    @FXML
    private ListView<Cliente> lvClientes;

    @FXML
    private LineChart<String, Number> chartTendencia;
    @FXML
    private CategoryAxis ejeX;
    @FXML
    private NumberAxis ejeY;

    @FXML
    private Label lblDeltaMusculo, lblDeltaGrasa;

    @FXML
    private TableView<Progreso> tablaHistorial;
    @FXML
    private TableColumn<Progreso, String> colFecha;
    @FXML
    private TableColumn<Progreso, String> colPeso;
    @FXML
    private TableColumn<Progreso, String> colIMC;
    @FXML
    private TableColumn<Progreso, String> colGrasa;
    @FXML
    private TableColumn<Progreso, String> colMusculo;
    @FXML
    private TableColumn<Progreso, Void> colAcciones;
    @FXML
    private Label lblRegistros;
    @FXML
    private Button btnAnterior, btnSiguiente, btnPag1, btnPag2, btnPag3;

    private ClienteDAO clienteDAO;
    private ProgresoDAO progresoDAO;
    private ProgresoService progresoService;

    private String idCliente;
    private Cliente cliente;
    private List<Progreso> historialCompleto;
    private int paginaActual = 1;
    private int totalPaginas = 1;

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        clienteDAO = new ClienteDAO();
        progresoDAO = new ProgresoDAO();
        progresoService = new ProgresoService();

        configurarTabla();
        configurarIMCEnVivo();
        configurarBuscador();
        configurarNavActivo();
        ValidacionUtil.soloDecimales(txtPeso);
        ValidacionUtil.soloDecimales(txtAltura);
        ValidacionUtil.soloDecimales(txtGrasa);
        ValidacionUtil.soloDecimales(txtMusculo);

        Button[] inactivos = {navDashboard, navClientes, navInstructores, navMembresias, navFinanzas, navAI, navCitas};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navProgreso);

        if (pendingClienteId != null) {
            setIdCliente(pendingClienteId);
            pendingClienteId = null;
        }
    }

    public void setIdCliente(String id) {
        this.idCliente = id;
        if (id != null && !id.trim().isEmpty()) {
            cargarDatosCliente();
            cargarHistorial();
            cargarChart();
            if (cliente != null) {
                seleccionando = true;
                txtBuscarCliente.setText(cliente.getNombre() + " " + cliente.getApellidos());
                seleccionando = false;
            }
        }
    }

    private boolean seleccionando = false;

    private void configurarBuscador() {
        lvClientes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(Cliente c, boolean empty) {
                super.updateItem(c, empty);
                if (empty || c == null) {
                    setText(null);
                    setStyle("-fx-background-color: transparent;");
                } else {
                    setText(c.getNombre() + " " + c.getApellidos() + "  —  #" + c.getNumeroIdentificacion());
                    setStyle("-fx-text-fill: #e2e2e6; -fx-font-family: 'Inter'; -fx-font-size: 13px;"
                            + "-fx-padding: 8 12 8 12; -fx-background-color: transparent;");
                }
            }
        });

        txtBuscarCliente.textProperty().addListener((obs, old, val) -> {
            if (seleccionando) return;
            String q = val != null ? val.trim() : "";
            if (q.isEmpty()) {
                lvClientes.setVisible(false);
                return;
            }
            List<Cliente> resultados = clienteDAO.listarTodos().stream()
                    .filter(c -> c.getEstado() != null && c.getEstado().equals("ACTIVO"))
                    .filter(c -> (c.getNombre() + " " + c.getApellidos() + " " + c.getNumeroIdentificacion())
                            .toLowerCase().contains(q.toLowerCase()))
                    .limit(10)
                    .collect(Collectors.toList());
            lvClientes.getItems().setAll(resultados);
            lvClientes.setVisible(!resultados.isEmpty());
            lvClientes.setPrefHeight(Math.min(resultados.size() * 40 + 4, 180));
        });

        lvClientes.setOnMouseClicked(e -> {
            Cliente c = lvClientes.getSelectionModel().getSelectedItem();
            if (c != null) {
                seleccionando = true;
                txtBuscarCliente.setText(c.getNombre() + " " + c.getApellidos());
                seleccionando = false;
                lvClientes.setVisible(false);
                setIdCliente(c.getNumeroIdentificacion());
            }
        });
    }

    // ── IMC EN VIVO ────────────────────────────────────────────────────────

    private void configurarIMCEnVivo() {
        ChangeListener<String> listener = (obs, old, val) -> calcularIMC();
        txtPeso.textProperty().addListener(listener);
        txtAltura.textProperty().addListener(listener);
    }

    private void calcularIMC() {
        try {
            String pesoStr = txtPeso.getText().trim();
            String alturaStr = txtAltura.getText().trim();
            if (pesoStr.isEmpty() || alturaStr.isEmpty()) {
                txtIMC.setText("");
                return;
            }
            double peso = Double.parseDouble(pesoStr);
            double alturaCm = Double.parseDouble(alturaStr);
            if (peso <= 0 || alturaCm <= 0) {
                txtIMC.setText("");
                return;
            }
            double alturaM = alturaCm / 100.0;
            double imc = peso / (alturaM * alturaM);
            txtIMC.setText(String.format("%.1f", imc));
        } catch (NumberFormatException e) {
            txtIMC.setText("");
        }
    }

    // ── CARGA DE DATOS DEL CLIENTE ─────────────────────────────────────────

    private void cargarDatosCliente() {
        try {
            cliente = clienteDAO.buscarPorId(idCliente);
        } catch (Exception e) {
            System.err.println("Error al buscar cliente: " + e.getMessage());
            return;
        }
        if (cliente == null) return;

        lblNombreMiembro.setText(cliente.getNombre() + " " + cliente.getApellidos());
        lblIdMiembro.setText("ID: #" + cliente.getNumeroIdentificacion());

        String fotoUrl = cliente.getFotoUrl();
        if (fotoUrl != null && !fotoUrl.isBlank()) {
            try {
                Image img = new Image(new java.io.File(fotoUrl).toURI().toString(), false);
                if (!img.isError()) {
                    imgMiembro.setImage(img);
                    lblInicialesAvatar.setVisible(false);
                } else {
                    mostrarIniciales();
                }
            } catch (Exception e) {
                mostrarIniciales();
            }
        } else {
            mostrarIniciales();
        }

        List<Progreso> ultimos = progresoService.listarProgreso(idCliente);
        if (ultimos != null && !ultimos.isEmpty()) {
            Progreso ultimo = ultimos.get(0);
            lblPesoActual.setText(String.format("%.1f kg", ultimo.getPeso()));
            lblGrasaActual.setText(String.format("%.1f %%", ultimo.getPorcentajeGrasa()));
        }
    }

    private void mostrarIniciales() {
        String nombre = cliente.getNombre();
        String apellido = cliente.getApellidos();
        String iniciales = "";
        if (nombre != null && !nombre.isBlank()) iniciales += nombre.charAt(0);
        if (apellido != null && !apellido.isBlank()) iniciales += apellido.charAt(0);
        lblInicialesAvatar.setText(iniciales.isBlank() ? "?" : iniciales.toUpperCase());
        lblInicialesAvatar.setVisible(true);
        imgMiembro.setImage(null);
    }

    // ── HISTORIAL (TABLA + PAGINACIÓN) ─────────────────────────────────────

    private void configurarTabla() {
        colFecha.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getFechaRegistro() != null
                                ? d.getValue().getFechaRegistro().format(FMT_FECHA) : ""));
        colPeso.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%.1f", d.getValue().getPeso())));
        colIMC.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        String.format("%.1f", d.getValue().getImc())));
        colGrasa.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getPorcentajeGrasa() > 0
                                ? String.format("%.1f%%", d.getValue().getPorcentajeGrasa()) : "--"));
        colMusculo.setCellValueFactory(d ->
                new javafx.beans.property.SimpleStringProperty(
                        d.getValue().getMasaMuscular() > 0
                                ? String.format("%.1f kg", d.getValue().getMasaMuscular()) : "--"));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEliminar = new Button("ELIMINAR");

            {
                btnEliminar.setStyle("-fx-background-color: transparent; -fx-background-radius: 6;" +
                        "-fx-border-color: #ef4444; -fx-border-width: 1; -fx-border-radius: 6;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700;" +
                        "-fx-text-fill: #ef4444; -fx-cursor: hand; -fx-padding: 4 14 4 14;");
                btnEliminar.setOnAction(e -> {
                    Progreso p = getTableView().getItems().get(getIndex());
                    boolean ok = progresoDAO.desactivar(p.getIdProgreso());
                    if (ok) {
                        cargarHistorial();
                        cargarChart();
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnEliminar);
            }
        });
    }

    private void cargarHistorial() {
        historialCompleto = progresoService.listarProgreso(idCliente);
        if (historialCompleto == null) historialCompleto = List.of();

        totalPaginas = (int) Math.ceil((double) historialCompleto.size() / PAGE_SIZE);
        if (totalPaginas < 1) totalPaginas = 1;
        paginaActual = 1;
        aplicarPagina();
        actualizarBotonesPagina();
    }

    private void aplicarPagina() {
        int desde = (paginaActual - 1) * PAGE_SIZE;
        int hasta = Math.min(desde + PAGE_SIZE, historialCompleto.size());
        List<Progreso> pagina;
        if (desde >= historialCompleto.size()) {
            pagina = List.of();
        } else {
            pagina = historialCompleto.subList(desde, hasta);
        }
        tablaHistorial.setItems(FXCollections.observableArrayList(pagina));

        int total = historialCompleto.size();
        lblRegistros.setText("Mostrando " + (desde + 1) + " - " + hasta + " de " + total + " registros");
    }

    private void actualizarBotonesPagina() {
        btnAnterior.setDisable(paginaActual <= 1);
        btnSiguiente.setDisable(paginaActual >= totalPaginas);

        Button[] pagBts = {btnPag1, btnPag2, btnPag3};
        int inicio = Math.max(1, paginaActual - 1);
        int fin = Math.min(totalPaginas, inicio + 2);
        if (fin - inicio < 2) inicio = Math.max(1, fin - 2);

        for (int i = 0; i < pagBts.length; i++) {
            int numPag = inicio + i;
            if (numPag <= totalPaginas) {
                pagBts[i].setVisible(true);
                pagBts[i].setText(String.valueOf(numPag));
                pagBts[i].setStyle(numPag == paginaActual
                        ? "-fx-background-color: #282a2d; -fx-background-radius: 8;"
                        + "-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600;"
                        + "-fx-text-fill: white; -fx-border-color: #D4FF00; -fx-border-width: 1;"
                        + "-fx-border-radius: 8; -fx-cursor: hand;"
                        : "-fx-background-color: transparent; -fx-background-radius: 8;"
                        + "-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-font-weight: 600;"
                        + "-fx-text-fill: white; -fx-border-color: #333538; -fx-border-width: 1;"
                        + "-fx-border-radius: 8; -fx-cursor: hand;");
                final int p = numPag;
                pagBts[i].setOnAction(e -> irAPagina(p));
            } else {
                pagBts[i].setVisible(false);
            }
        }
    }

    private void irAPagina(int p) {
        if (p < 1 || p > totalPaginas) return;
        paginaActual = p;
        aplicarPagina();
        actualizarBotonesPagina();
    }

    @FXML
    private void handlePaginaAnterior() {
        irAPagina(paginaActual - 1);
    }

    @FXML
    private void handlePaginaSiguiente() {
        irAPagina(paginaActual + 1);
    }

    @FXML
    private void handlePagina1() {
        irAPagina(1);
    }

    @FXML
    private void handlePagina2() {
        irAPagina(2);
    }

    @FXML
    private void handlePagina3() {
        irAPagina(3);
    }

    // ── CHART DE TENDENCIA ─────────────────────────────────────────────────

    private void cargarChart() {
        chartTendencia.getData().clear();
        chartTendencia.setAnimated(false);

        if (historialCompleto == null || historialCompleto.isEmpty()) return;

        List<Progreso> cronologico = historialCompleto.stream()
                .sorted((a, b) -> a.getFechaRegistro().compareTo(b.getFechaRegistro()))
                .collect(Collectors.toList());

        XYChart.Series<String, Number> seriePeso = new XYChart.Series<>();
        seriePeso.setName("Peso");

        XYChart.Series<String, Number> serieGrasa = new XYChart.Series<>();
        serieGrasa.setName("Grasa %");

        for (Progreso p : cronologico) {
            String label = p.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM"));
            seriePeso.getData().add(new XYChart.Data<>(label, p.getPeso()));
            if (p.getPorcentajeGrasa() > 0) {
                serieGrasa.getData().add(new XYChart.Data<>(label, p.getPorcentajeGrasa()));
            }
        }

        chartTendencia.getData().addAll(seriePeso, serieGrasa);

        List<String> ordenCategorias = cronologico.stream()
                .map(p -> p.getFechaRegistro().format(DateTimeFormatter.ofPattern("dd/MM")))
                .distinct()
                .collect(Collectors.toList());
        ejeX.setAutoRanging(false);
        ejeX.setCategories(FXCollections.observableArrayList(ordenCategorias));

        // Estilo dark para el chart
        chartTendencia.setStyle("-fx-background-color: transparent;");
        chartTendencia.setCreateSymbols(true);
        chartTendencia.setLegendVisible(true);
        chartTendencia.setHorizontalGridLinesVisible(true);
        chartTendencia.setVerticalGridLinesVisible(false);
        chartTendencia.setAlternativeRowFillVisible(false);
        chartTendencia.setAlternativeColumnFillVisible(false);

        ejeX.setTickLabelFill(javafx.scene.paint.Color.valueOf("#6b7280"));
        ejeX.setTickLabelFont(javafx.scene.text.Font.font("Space Grotesk", 11));
        ejeY.setTickLabelFill(javafx.scene.paint.Color.valueOf("#6b7280"));
        ejeY.setTickLabelFont(javafx.scene.text.Font.font("Space Grotesk", 11));

        // Color series via CSS lookup después del layout
        chartTendencia.applyCss();
        chartTendencia.lookupAll(".chart-series-line").forEach(n ->
                n.setStyle("-fx-stroke: #D4FF00; -fx-stroke-width: 2;"));
        chartTendencia.lookupAll(".chart-series-line").stream()
                .skip(1).forEach(n ->
                        n.setStyle("-fx-stroke: #bdf4ff; -fx-stroke-width: 2;"));
        chartTendencia.lookupAll(".chart-line-symbol").forEach(n ->
                n.setStyle("-fx-background-color: #D4FF00, white;"));
        chartTendencia.lookupAll(".chart-legend-item").forEach(n -> {
            if (n instanceof Label lbl) {
                if ("Peso".equals(lbl.getText())) lbl.setStyle("-fx-text-fill: #D4FF00;");
                if ("Grasa %".equals(lbl.getText())) lbl.setStyle("-fx-text-fill: #bdf4ff;");
            }
        });
        Node legend = chartTendencia.lookup(".chart-legend");
        if (legend != null) legend.setStyle("-fx-background-color: transparent;");
        Node plotBg = chartTendencia.lookup(".chart-plot-background");
        if (plotBg != null) plotBg.setStyle("-fx-background-color: transparent;");
        chartTendencia.lookupAll(".chart-horizontal-grid-lines").forEach(n ->
                n.setStyle("-fx-stroke: #1f2125;"));
        chartTendencia.lookupAll(".chart-vertical-grid-lines").forEach(n ->
                n.setStyle("-fx-stroke: transparent;"));

        // Mini stats (delta última vs primera)
        if (cronologico.size() >= 2) {
            Progreso primero = cronologico.get(0);
            Progreso ultimo = cronologico.get(cronologico.size() - 1);

            double deltaMusculo = ultimo.getMasaMuscular() - primero.getMasaMuscular();
            double deltaGrasa = ultimo.getPorcentajeGrasa() - primero.getPorcentajeGrasa();

            lblDeltaMusculo.setText((deltaMusculo >= 0 ? "+" : "") + String.format("%.1f kg", deltaMusculo));
            lblDeltaMusculo.setStyle(deltaMusculo >= 0
                    ? "-fx-text-fill: #D4FF00; -fx-font-family: 'Lexend'; -fx-font-size: 22px; -fx-font-weight: 700;"
                    : "-fx-text-fill: #ffb4ab; -fx-font-family: 'Lexend'; -fx-font-size: 22px; -fx-font-weight: 700;");

            lblDeltaGrasa.setText((deltaGrasa >= 0 ? "+" : "") + String.format("%.1f %%", deltaGrasa));
            lblDeltaGrasa.setStyle(deltaGrasa <= 0
                    ? "-fx-text-fill: #bdf4ff; -fx-font-family: 'Lexend'; -fx-font-size: 22px; -fx-font-weight: 700;"
                    : "-fx-text-fill: #ffb4ab; -fx-font-family: 'Lexend'; -fx-font-size: 22px; -fx-font-weight: 700;");
        }
    }

    // ── GUARDAR PROGRESO ───────────────────────────────────────────────────

    @FXML
    private void handleGuardarProgreso() {
        if (idCliente == null) {
            mostrarAlerta("Seleccione un cliente primero.");
            return;
        }

        String pesoStr = txtPeso.getText().trim();
        String alturaStr = txtAltura.getText().trim();

        if (pesoStr.isEmpty() || alturaStr.isEmpty()) {
            mostrarAlerta("Ingrese peso y altura.");
            return;
        }

        try {
            double peso = Double.parseDouble(pesoStr);
            double alturaCm = Double.parseDouble(alturaStr);
            if (peso <= 0 || alturaCm <= 0) {
                mostrarAlerta("Peso y altura deben ser valores positivos.");
                return;
            }

            double alturaM = alturaCm / 100.0;
            double imc = peso / (alturaM * alturaM);

            Progreso p = new Progreso();
            p.setIdCliente(idCliente);
            p.setFechaRegistro(java.time.LocalDate.now());
            p.setPeso(peso);
            p.setAltura(alturaM);
            p.setImc(imc);
            p.setObjetivo("");

            String grasaStr = txtGrasa.getText().trim();
            if (!grasaStr.isEmpty()) {
                try {
                    p.setPorcentajeGrasa(Double.parseDouble(grasaStr));
                } catch (NumberFormatException ignored) {
                }
            }

            String musculoStr = txtMusculo.getText().trim();
            if (!musculoStr.isEmpty()) {
                try {
                    p.setMasaMuscular(Double.parseDouble(musculoStr));
                } catch (NumberFormatException ignored) {
                }
            }

            boolean ok = progresoService.registrarProgreso(p);
            if (ok) {
                txtPeso.clear();
                txtAltura.clear();
                txtGrasa.clear();
                txtMusculo.clear();
                txtIMC.clear();
                cargarHistorial();
                cargarChart();
                cargarDatosCliente();
            } else {
                mostrarAlerta("Error al guardar el progreso.");
            }
        } catch (NumberFormatException e) {
            mostrarAlerta("Ingrese valores numéricos válidos.");
        }
    }

    // ── HANDLERS — TOP BAR ─────────────────────────────────────────────────
    @FXML private void handleFiltrarHistorial() {}

    // ── HANDLERS — NAV ─────────────────────────────────────────────────────

    @FXML private void handleNavDashboard()    { navegarA("/fxml/Dashboard.fxml"); }
    @FXML private void handleNavClientes()     { navegarA("/fxml/GestionClientes.fxml"); }
    @FXML private void handleNavInstructores() { navegarA("/fxml/GestionInstructores.fxml"); }
    @FXML private void handleNavMembresias()   { navegarA("/fxml/GestionMembresias.fxml"); }
    @FXML private void handleNavFinanzas()    { navegarA("/fxml/Finanzas.fxml"); }
    @FXML private void handleNavProgreso()     { navegarA("/fxml/ProgresoFisico.fxml"); }
    @FXML private void handleNavAI()           { navegarA("/fxml/GymbroAI.fxml"); }
    @FXML private void handleNavCitas()        { navegarA("/fxml/GestionCitas.fxml"); }

    @FXML
    private void handleLogout() {
        if (AlertaPersonalizada.confirmar("Cerrar sesion", "Seguro que deseas cerrar sesion?")) {
            navegarA("/fxml/login.fxml");
        }
    }

    private void configurarNavActivo() {
        Button[] allNav = {navDashboard, navClientes, navInstructores, navMembresias, navFinanzas, navProgreso, navCitas, navAI};
        for (Button b : allNav) {
            if (b != navProgreso) {
                b.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;"
                        + "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 500;"
                        + "-fx-text-fill: #9ca3af; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
            }
        }
        navProgreso.setStyle("-fx-background-color: #D4FF00; -fx-background-radius: 8;"
                + "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 700;"
                + "-fx-text-fill: black; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
    }

    private void agregarHoverInactivo(Button btn) {
        btn.setOnMouseEntered(e -> {
            if (!btn.getStyle().contains("#D4FF00")) {
                btn.setStyle("-fx-background-color: #1f2226; -fx-background-radius: 8;"
                        + "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 500;"
                        + "-fx-text-fill: white; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
            }
        });
        btn.setOnMouseExited(e -> {
            if (!btn.getStyle().contains("#D4FF00")) {
                btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8;"
                        + "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 500;"
                        + "-fx-text-fill: #9ca3af; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
            }
        });
    }

    private void agregarHoverActivo(Button btn) {
        btn.setOnMouseEntered(e -> {
            btn.setStyle("-fx-background-color: #c8f000; -fx-background-radius: 8;"
                    + "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 700;"
                    + "-fx-text-fill: black; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
        });
        btn.setOnMouseExited(e -> {
            btn.setStyle("-fx-background-color: #D4FF00; -fx-background-radius: 8;"
                    + "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 700;"
                    + "-fx-text-fill: black; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
        });
    }

    private void navegarA(String rutaFxml) {
        Main.navegarA(rutaFxml);
    }

    private void mostrarAlerta(String mensaje) {
        AlertaPersonalizada.info("Informacion", mensaje);
    }
}