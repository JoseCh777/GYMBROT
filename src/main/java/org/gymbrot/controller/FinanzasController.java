package org.gymbrot.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.gymbrot.Main;
import org.gymbrot.dao.PagoDAO;
import org.gymbrot.service.FinanzasService;
import org.gymbrot.util.AlertaPersonalizada;

import java.net.URL;
import java.util.*;
import java.util.ResourceBundle;
import java.text.NumberFormat;
import java.util.Locale;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class FinanzasController implements Initializable {

    // ─── SideNav ───────────────────────────────────────────────────────────
    @FXML private VBox sideNav;
    @FXML private Button navDashboard;
    @FXML private Button navClientes;
    @FXML private Button navInstructores;
    @FXML private Button navMembresias;
    @FXML private Button navFinanzas;
    @FXML private Button navProgreso;
    @FXML private Button navCitas;
    @FXML private Button navAI;

    // ─── TopBar ────────────────────────────────────────────────────────────
    @FXML private HBox topBar;

    // ─── Cards ─────────────────────────────────────────────────────────────
    @FXML private Label lblIngresosMes;
    @FXML private Label lblMiembrosActivos;
    @FXML private Label lblTotalIngresos;

    // ─── Charts ────────────────────────────────────────────────────────────
    @FXML private BarChart<String, Number> chartIngresosMes;
    @FXML private PieChart chartMetodoPago;
    @FXML private BarChart<String, Number> chartNuevosClientes;

    // ─── Plan Cards ────────────────────────────────────────────────────────
    @FXML private HBox contenedorIngresosPlan;

    // ─── Tables ────────────────────────────────────────────────────────────
    @FXML private TableView<FinanzasService.PagoVencido> tablaPagosVencidos;
    @FXML private TableColumn<FinanzasService.PagoVencido, String> colVencidoCliente;
    @FXML private TableColumn<FinanzasService.PagoVencido, String> colVencidoPlan;
    @FXML private TableColumn<FinanzasService.PagoVencido, String> colVencidoMonto;
    @FXML private TableColumn<FinanzasService.PagoVencido, String> colVencidoMetodo;
    @FXML private TableColumn<FinanzasService.PagoVencido, String> colVencidoFecha;
    @FXML private TableColumn<FinanzasService.PagoVencido, String> colVencidoEstado;

    // ─── Historial de Pagos ────────────────────────────────────────────────
    @FXML private TableView<FilaPago> tablaHistorialPagos;
    @FXML private TableColumn<FilaPago, String> colHPCliente;
    @FXML private TableColumn<FilaPago, String> colHPPlan;
    @FXML private TableColumn<FilaPago, String> colHPMonto;
    @FXML private TableColumn<FilaPago, String> colHPMetodo;
    @FXML private TableColumn<FilaPago, String> colHPFecha;
    @FXML private TableColumn<FilaPago, String> colHPEstado;
    @FXML private TableColumn<FilaPago, String> colHPReferencia;

    // ─── Service / DAOs ────────────────────────────────────────────────────
    private final FinanzasService finanzasService = new FinanzasService();
    private final PagoDAO pagoDAO = new PagoDAO();

    private static final NumberFormat FORMATO_COP = NumberFormat.getCurrencyInstance(new Locale("es", "CO"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarAnimacionesNav();
        setNavActivo(navFinanzas);
        configurarTablaPagos();
        configurarTablaHistorialPagos();
        cargarCards();
        cargarChartIngresosMes();
        cargarChartMetodoPago();
        cargarChartNuevosClientes();
        cargarIngresosPlan();
        cargarTablaPagosVencidos();
        cargarHistorialPagos();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  NAV
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navClientes, navInstructores, navMembresias, navProgreso, navCitas, navAI};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navFinanzas);
    }

    private void setNavActivo(Button activo) {
        Button[] todos = {navDashboard, navClientes, navInstructores, navMembresias, navFinanzas, navProgreso, navCitas, navAI};
        for (Button btn : todos) {
            if (btn == null) continue;
            if (btn == activo) {
                btn.setStyle("-fx-background-color: #D4FF00; -fx-background-radius: 8; -fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 700; -fx-text-fill: black; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
                agregarHoverActivo(btn);
            } else {
                btn.setStyle("-fx-background-color: transparent; -fx-background-radius: 8; -fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 500; -fx-text-fill: #9ca3af; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;");
                agregarHoverInactivo(btn);
            }
        }
    }

    private void agregarHoverInactivo(Button btn) {
        if (btn == null) return;
        ScaleTransition grow = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03); grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btn.setOnMouseEntered(e -> {
            grow.playFromStart();
            if (btn.getStyle().contains("transparent")) {
                btn.setStyle(btn.getStyle()
                    .replace("-fx-background-color: transparent", "-fx-background-color: #1f2226")
                    .replace("-fx-text-fill: #9ca3af", "-fx-text-fill: white"));
            }
        });
        btn.setOnMouseExited(e -> {
            shrink.playFromStart();
            if (btn.getStyle().contains("#1f2226")) {
                btn.setStyle(btn.getStyle()
                    .replace("-fx-background-color: #1f2226", "-fx-background-color: transparent")
                    .replace("-fx-text-fill: white", "-fx-text-fill: #9ca3af"));
            }
        });
        btn.setOnMousePressed(e -> {
            ScaleTransition p = new ScaleTransition(Duration.millis(80), btn);
            p.setToX(0.97); p.setToY(0.97); p.play();
        });
        btn.setOnMouseReleased(e -> {
            ScaleTransition r = new ScaleTransition(Duration.millis(80), btn);
            r.setToX(1.0); r.setToY(1.0); r.play();
        });
    }

    private void agregarHoverActivo(Button btn) {
        if (btn == null) return;
        ScaleTransition grow = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03); grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btn.setOnMouseEntered(e -> grow.playFromStart());
        btn.setOnMouseExited(e -> shrink.playFromStart());
        btn.setOnMousePressed(e -> {
            ScaleTransition p = new ScaleTransition(Duration.millis(80), btn);
            p.setToX(0.97); p.setToY(0.97); p.play();
        });
        btn.setOnMouseReleased(e -> {
            ScaleTransition r = new ScaleTransition(Duration.millis(80), btn);
            r.setToX(1.0); r.setToY(1.0); r.play();
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TABLAS
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarTablaPagos() {
        colVencidoCliente.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().cliente()));
        colVencidoPlan.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().plan()));
        colVencidoMonto.setCellValueFactory(data -> new SimpleStringProperty(formatearDinero(data.getValue().valor())));
        colVencidoMetodo.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().metodo()));
        colVencidoFecha.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().fecha()));
        colVencidoEstado.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().estado()));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CARDS
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarCards() {
        double ingresosMes = finanzasService.ingresosMesActual();
        lblIngresosMes.setText(formatearDinero(ingresosMes));

        int activos = finanzasService.contarMiembrosActivos();
        lblMiembrosActivos.setText(String.valueOf(activos));

        List<FinanzasService.IngresoMes> todos = finanzasService.ingresosPorMes();
        double total = todos.stream().mapToDouble(FinanzasService.IngresoMes::total).sum();
        lblTotalIngresos.setText(formatearDinero(total));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CHARTS
    // ═══════════════════════════════════════════════════════════════════════

    private void adornarBarras(XYChart.Series<String, Number> series, List<String> claves, List<Double> valores, String colorActual, boolean esDinero) {
        int mesActualIdx = LocalDate.now().getMonthValue() - 1;
        for (int i = 0; i < series.getData().size(); i++) {
            XYChart.Data<String, Number> data = series.getData().get(i);
            String clave = claves.get(i);
            double valor = valores.get(i);
            boolean esActual = (i == mesActualIdx);
            String color = esActual ? colorActual : "#3a5a3a";
            String valorTxt = esDinero ? formatearDinero(valor) : String.valueOf((int) valor);

            Node node = data.getNode();
            if (node != null) {
                aplicarEfectoBarra(node, color, colorActual, clave, valorTxt);
            } else {
                data.nodeProperty().addListener((obs, old, n) -> {
                    if (n != null) aplicarEfectoBarra(n, color, colorActual, clave, valorTxt);
                });
            }
        }
    }

    private void aplicarEfectoBarra(Node node, String color, String colorActual, String clave, String valorTxt) {
        node.setStyle("-fx-bar-fill: " + color + ";");
        String[] p = clave.split("-");
        String label = formatearMes(clave) + " " + p[0];
        Tooltip t = new Tooltip(label + " — " + valorTxt);
        t.setShowDelay(Duration.millis(200));
        Tooltip.install(node, t);
        node.setOnMouseEntered(e -> {
            node.setStyle("-fx-bar-fill: " + colorActual + "; -fx-effect: dropshadow(gaussian, " + colorActual + ", 10, 0.5, 0, 0);");
            node.setScaleY(1.06);
        });
        node.setOnMouseExited(e -> {
            node.setStyle("-fx-bar-fill: " + color + ";");
            node.setScaleY(1.0);
        });
    }

    private List<String> generar12Meses() {
        int anio = LocalDate.now().getYear();
        List<String> claves = new ArrayList<>();
        for (int m = 1; m <= 12; m++) {
            claves.add(String.format("%d-%02d", anio, m));
        }
        return claves;
    }

    private void cargarChartIngresosMes() {
        List<FinanzasService.IngresoMes> datos = finanzasService.ingresosPorMes();
        Map<String, Double> mapa = new HashMap<>();
        for (FinanzasService.IngresoMes d : datos) mapa.put(d.mes(), d.total());

        List<String> claves = generar12Meses();
        List<Double> valores = new ArrayList<>();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (String k : claves) {
            double v = mapa.getOrDefault(k, 0.0);
            valores.add(v);
            series.getData().add(new XYChart.Data<>(formatearMes(k), v));
        }
        chartIngresosMes.getData().clear();
        chartIngresosMes.getData().add(series);
        adornarBarras(series, claves, valores, "#D4FF00", true);
    }

    private void cargarChartMetodoPago() {
        List<FinanzasService.MetodoPago> datos = finanzasService.desgloseMetodoPago();
        chartMetodoPago.getData().clear();
        String[] colores = {"#D4FF00", "#bdf4ff", "#ff6b6b", "#ffd93d", "#6bcbff"};
        for (int i = 0; i < datos.size(); i++) {
            FinanzasService.MetodoPago d = datos.get(i);
            String color = colores[i % colores.length];
            PieChart.Data slice = new PieChart.Data(String.format("%-17s%s", d.metodo(), formatearDinero(d.total())), d.total());
            slice.nodeProperty().addListener((obs, old, node) -> {
                if (node == null) return;
                node.setStyle("-fx-pie-color: " + color + ";");

                Tooltip t = new Tooltip(slice.getName());
                t.setShowDelay(Duration.millis(200));
                Tooltip.install(node, t);

                node.setOnMouseEntered(e -> {
                    node.setScaleX(1.06);
                    node.setScaleY(1.06);
                });
                node.setOnMouseExited(e -> {
                    node.setScaleX(1.0);
                    node.setScaleY(1.0);
                });
            });
            chartMetodoPago.getData().add(slice);
        }
    }

    private void cargarChartNuevosClientes() {
        List<FinanzasService.NuevosClientesMes> datos = finanzasService.nuevosClientesPorMes();
        Map<String, Integer> mapa = new HashMap<>();
        for (FinanzasService.NuevosClientesMes d : datos) mapa.put(d.mes(), d.cantidad());

        List<String> claves = generar12Meses();
        List<Double> valores = new ArrayList<>();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        for (String k : claves) {
            double v = mapa.getOrDefault(k, 0);
            valores.add(v);
            series.getData().add(new XYChart.Data<>(formatearMes(k), v));
        }
        chartNuevosClientes.getData().clear();
        chartNuevosClientes.getData().add(series);
        adornarBarras(series, claves, valores, "#bdf4ff", false);
    }

    private void cargarIngresosPlan() {
        List<FinanzasService.IngresoPlan> datos = finanzasService.ingresosPorPlan();
        contenedorIngresosPlan.getChildren().clear();
        for (FinanzasService.IngresoPlan d : datos) {
            VBox card = new VBox(8);
            card.setPrefHeight(120);
            card.setStyle("-fx-background-color: #121417; -fx-background-radius: 12; -fx-border-color: #1f2125; -fx-border-width: 1; -fx-border-radius: 12;");
            card.setPadding(new javafx.geometry.Insets(16));

            Label lblPlan = new Label(d.plan().toUpperCase());
            lblPlan.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #6b7280;");

            Label lblTotal = new Label(formatearDinero(d.total()));
            lblTotal.setStyle("-fx-font-family: 'Lexend'; -fx-font-size: 28px; -fx-font-weight: 800; -fx-text-fill: white;");

            card.getChildren().addAll(lblPlan, lblTotal);
            HBox.setHgrow(card, javafx.scene.layout.Priority.ALWAYS);
            contenedorIngresosPlan.getChildren().add(card);
        }
    }

    private void cargarTablaPagosVencidos() {
        List<FinanzasService.PagoVencido> datos = finanzasService.pagosVencidos();
        ObservableList<FinanzasService.PagoVencido> items = FXCollections.observableArrayList(datos);
        tablaPagosVencidos.setItems(items);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — NAV
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handleNavDashboard()    { navegarA("/fxml/Dashboard.fxml"); }
    @FXML private void handleNavClientes()     { navegarA("/fxml/GestionClientes.fxml"); }
    @FXML private void handleNavInstructores() { navegarA("/fxml/GestionInstructores.fxml"); }
    @FXML private void handleNavMembresias()   { navegarA("/fxml/GestionMembresias.fxml"); }
    @FXML private void handleNavProgreso()     { navegarA("/fxml/ProgresoFisico.fxml"); }
    @FXML private void handleNavCitas()        { navegarA("/fxml/GestionCitas.fxml"); }
    @FXML private void handleNavAI()           { navegarA("/fxml/GymbroAI.fxml"); }

    @FXML
    private void handleLogout() {
        if (AlertaPersonalizada.confirmar("Cerrar sesion", "Seguro que deseas cerrar sesion?")) {
            navegarA("/fxml/login.fxml");
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════

    private void navegarA(String rutaFxml) {
        Main.navegarA(rutaFxml);
    }

    private String formatearDinero(double valor) {
        return String.format("$%,.0f", valor);
    }

    private String formatearMes(String yyyyMm) {
        String[] partes = yyyyMm.split("-");
        if (partes.length < 2) return yyyyMm;
        int mes = Integer.parseInt(partes[1]);
        return switch (mes) {
            case 1 -> "Ene"; case 2 -> "Feb"; case 3 -> "Mar";
            case 4 -> "Abr"; case 5 -> "May"; case 6 -> "Jun";
            case 7 -> "Jul"; case 8 -> "Ago"; case 9 -> "Sep";
            case 10 -> "Oct"; case 11 -> "Nov"; case 12 -> "Dic";
            default -> yyyyMm;
        };
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HISTORIAL DE PAGOS
    // ═══════════════════════════════════════════════════════════════════════

    public record FilaPago(String cliente, String plan, String monto, String metodo, String fecha, String estado, String referencia) {}

    private void configurarTablaHistorialPagos() {
        tablaHistorialPagos.widthProperty().addListener((obs, old, w) -> {
            if (w.doubleValue() > 0) {
                var header = tablaHistorialPagos.lookup(".column-header-background");
                if (header != null) header.setStyle("-fx-background-color: #121417;");
                var headers = tablaHistorialPagos.lookupAll(".column-header");
                for (var h : headers) {
                    h.setStyle("-fx-background-color: #121417; -fx-border-color: #1f2125;");
                }
            }
        });
        colHPCliente.setCellValueFactory(d  -> new SimpleStringProperty(d.getValue().cliente()));
        colHPPlan.setCellValueFactory(d     -> new SimpleStringProperty(d.getValue().plan()));
        colHPMonto.setCellValueFactory(d    -> new SimpleStringProperty("$" + d.getValue().monto()));
        colHPMetodo.setCellValueFactory(d   -> new SimpleStringProperty(d.getValue().metodo()));
        colHPFecha.setCellValueFactory(d    -> new SimpleStringProperty(d.getValue().fecha()));
        colHPEstado.setCellValueFactory(d   -> new SimpleStringProperty(d.getValue().estado()));
        colHPReferencia.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().referencia()));

        TableColumn<FilaPago, String>[] cols = new TableColumn[]{
                colHPCliente, colHPPlan, colHPMonto, colHPMetodo, colHPFecha, colHPEstado, colHPReferencia
        };
        for (var col : cols) {
            col.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    setText(item);
                    String estilo = "-fx-background-color: transparent; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-padding: 8 12 8 12;";
                    if ("EXITOSO".equals(item)) {
                        setStyle(estilo + "-fx-text-fill: #D4FF00; -fx-font-weight: 700;");
                    } else if ("PENDIENTE".equals(item)) {
                        setStyle(estilo + "-fx-text-fill: #fbbf24; -fx-font-weight: 700;");
                    } else {
                        setStyle(estilo + "-fx-text-fill: #d1d5db;");
                    }
                }
            });
        }
    }

    private void cargarHistorialPagos() {
        List<PagoDAO.PagoConCliente> pagos = pagoDAO.listarTodosConCliente();
        ObservableList<FilaPago> items = FXCollections.observableArrayList();
        for (PagoDAO.PagoConCliente p : pagos) {
            items.add(new FilaPago(
                    p.clienteNombre() + " " + p.clienteApellidos(),
                    p.planNombre(),
                    String.format("%.0f", p.valor()),
                    p.metodoPago(),
                    p.fechaPago(),
                    p.estadoPago(),
                    p.referencia() != null ? p.referencia() : ""
            ));
        }
        tablaHistorialPagos.setItems(items);
    }
}
