package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.gymbrot.dao.PagoDAO;
import org.gymbrot.dao.PlanMembresiaDAO;
import org.gymbrot.model.PlanMembresia;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import org.gymbrot.Main;
import org.gymbrot.util.AlertaPersonalizada;

public class GestionMembresiasController implements Initializable {

    // ── SideNav ──────────────────────────────────────────────
    @FXML private VBox sideNav;
    @FXML private Button navDashboard;
    @FXML private Button navClientes;
    @FXML private Button navInstructores;
    @FXML private Button navMembresias;
    @FXML private Button navAI;
    @FXML private Button navProgreso;
    @FXML private Button navCitas;

    @FXML private Button btnLogout;

    // ── Selector de duracion ──────────────────────────────────
    @FXML private Button btnMensual;
    @FXML private Button btnSemestral;
    @FXML private Button btnAnual;

    // ── Botones de plan ───────────────────────────────────────
    @FXML private Button btnSeleccionarSilver;
    @FXML private Button btnSeleccionarGold;
    @FXML private Button btnSeleccionarBlack;

    // ── Banner AI ─────────────────────────────────────────────
    @FXML private Button btnChatearAI;

    // ── Labels de precio ─────────────────────────────────────
    @FXML private Label lblPrecioSilver;
    @FXML private Label lblPrecioBlack;
    @FXML private Label lblPrecioGold;
    @FXML private Label lblFacturacionSilver;
    @FXML private Label lblFacturacionBlack;
    @FXML private Label lblFacturacionGold;
    @FXML private Label lblPeriodoSilver;
    @FXML private Label lblPeriodoBlack;
    @FXML private Label lblPeriodoGold;

    // ── Beneficios en cards ──────────────────────────────────
    @FXML private VBox vbSilverBeneficios;
    @FXML private VBox vbBlackBeneficios;
    @FXML private VBox vbGoldBeneficios;

    // ── Tabla comparativa ────────────────────────────────────
    @FXML private TableView<FilaComparativa> tablaComparativa;
    @FXML private TableColumn<FilaComparativa, String> colBeneficio;
    @FXML private TableColumn<FilaComparativa, String> colSilver;
    @FXML private TableColumn<FilaComparativa, String> colBlack;
    @FXML private TableColumn<FilaComparativa, String> colGold;

    // ── Historial de Pagos ───────────────────────────────────
    @FXML private TableView<FilaPago> tablaHistorialPagos;
    @FXML private TableColumn<FilaPago, String> colHPCliente;
    @FXML private TableColumn<FilaPago, String> colHPPlan;
    @FXML private TableColumn<FilaPago, String> colHPMonto;
    @FXML private TableColumn<FilaPago, String> colHPMetodo;
    @FXML private TableColumn<FilaPago, String> colHPFecha;
    @FXML private TableColumn<FilaPago, String> colHPEstado;
    @FXML private TableColumn<FilaPago, String> colHPReferencia;

    // ── DAOs ─────────────────────────────────────────────────
    private final PlanMembresiaDAO planDAO = new PlanMembresiaDAO();
    private final PagoDAO pagoDAO = new PagoDAO();

    // ── Estado interno ───────────────────────────────────────
    private enum Duracion { MENSUAL, SEMESTRAL, ANUAL }
    private Duracion duracionActual = Duracion.ANUAL;

    private PlanMembresia planSilver;
    private PlanMembresia planGold;
    private PlanMembresia planBlack;

    // Estilos nav
    private static final String STYLE_NAV_ACTIVO =
            "-fx-background-color: #D4FF00; -fx-background-radius: 8; " +
                    "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 700; " +
                    "-fx-text-fill: black; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;";
    private static final String STYLE_NAV_INACTIVO =
            "-fx-background-color: transparent; -fx-background-radius: 8; " +
                    "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 500; " +
                    "-fx-text-fill: #9ca3af; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;";

    // Estilos selector duracion
    private static final String STYLE_BTN_DURACION_ACTIVO =
            "-fx-background-color: #D4FF00; -fx-background-radius: 8; " +
                    "-fx-font-family: 'Lexend'; -fx-font-size: 13px; -fx-font-weight: 700; " +
                    "-fx-text-fill: #121417; -fx-cursor: hand; -fx-padding: 6 20 6 20;";
    private static final String STYLE_BTN_DURACION_INACTIVO =
            "-fx-background-color: transparent; -fx-background-radius: 8; " +
                    "-fx-font-family: 'Lexend'; -fx-font-size: 13px; -fx-font-weight: 700; " +
                    "-fx-text-fill: #9ca3af; -fx-cursor: hand; -fx-padding: 6 20 6 20;";

    // ─────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarPlanes();
        configurarAnimacionesNav();
        setNavActivo(navMembresias);
        configurarAnimacionesBotones();
        configurarTablaComparativa();
        actualizarPrecios();
        configurarTablaHistorial();
        cargarHistorialPagos();
    }

    private void cargarPlanes() {
        List<PlanMembresia> planes = planDAO.listarTodos();
        if (planes.size() >= 3) {
            planSilver = planes.get(0);
            planGold   = planes.get(1);
            planBlack  = planes.get(2);
        }
        poblarBeneficiosCards();
    }

    private void poblarBeneficiosCards() {
        poblarCard(vbSilverBeneficios, planSilver != null ? planSilver.getBeneficios() : null, false);
        poblarCard(vbBlackBeneficios,  planBlack  != null ? planBlack.getBeneficios()  : null, true);
        poblarCard(vbGoldBeneficios,   planGold   != null ? planGold.getBeneficios()   : null, false);
    }

    private void poblarCard(VBox contenedor, String beneficios, boolean destacado) {
        if (contenedor == null) return;
        contenedor.getChildren().clear();
        if (beneficios == null || beneficios.isBlank()) {
            contenedor.getChildren().add(crearItemBeneficio("Sin beneficios", false, destacado));
            return;
        }
        for (String b : beneficios.split(" - ")) {
            contenedor.getChildren().add(crearItemBeneficio(b.trim(), true, destacado));
        }
    }

    private Node crearItemBeneficio(String texto, boolean incluido, boolean destacado) {
        Label icono = new Label(incluido ? "OK" : "--");
        icono.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (incluido ? "#D4FF00" : "#4b5563") + ";");

        Label lbl = new Label(texto);
        lbl.setWrapText(true);
        lbl.setStyle(String.format(
                "-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: %s;",
                destacado ? "white" : "#d1d5db"
        ));
        if (destacado) lbl.setStyle(lbl.getStyle() + "-fx-font-weight: 600;");
        HBox.setHgrow(lbl, javafx.scene.layout.Priority.ALWAYS);

        HBox fila = new HBox(10, icono, lbl);
        fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        return fila;
    }

    // ══ Navegacion ═══════════════════════════════════════════

    @FXML
    private void handleNavDashboard(ActionEvent event) {
        navegarA("/fxml/Dashboard.fxml", event);
    }

    @FXML
    private void handleNavClientes(ActionEvent event) {
        navegarA("/fxml/GestionClientes.fxml", event);
    }

    @FXML
    private void handleNavInstructores(ActionEvent event) {
        navegarA("/fxml/GestionInstructores.fxml", event);
    }

    @FXML
    private void handleNavMembresias(ActionEvent event) {
        // Ya estamos en esta vista, no hacer nada
    }

    @FXML
    private void handleNavAI(ActionEvent event) {
        navegarA("/fxml/GymbroAI.fxml", event);
    }

    @FXML
    private void handleNavProgreso(ActionEvent event) { navegarA("/fxml/ProgresoFisico.fxml", event); }

    @FXML
    private void handleNavCitas(ActionEvent event) { navegarA("/fxml/GestionCitas.fxml", event); }

    @FXML
    private void handleLogout(ActionEvent event) {
        navegarA("/fxml/login.fxml", event);
    }

    // ══ Selector de duracion ══════════════════════════════════

    @FXML
    private void handleDuracionMensual(ActionEvent event) {
        duracionActual = Duracion.MENSUAL;
        btnMensual.setStyle(STYLE_BTN_DURACION_ACTIVO);
        btnSemestral.setStyle(STYLE_BTN_DURACION_INACTIVO);
        btnAnual.setStyle(STYLE_BTN_DURACION_INACTIVO);
        actualizarPrecios();
    }

    @FXML
    private void handleDuracionSemestral(ActionEvent event) {
        duracionActual = Duracion.SEMESTRAL;
        btnMensual.setStyle(STYLE_BTN_DURACION_INACTIVO);
        btnSemestral.setStyle(STYLE_BTN_DURACION_ACTIVO);
        btnAnual.setStyle(STYLE_BTN_DURACION_INACTIVO);
        actualizarPrecios();
    }

    @FXML
    private void handleDuracionAnual(ActionEvent event) {
        duracionActual = Duracion.ANUAL;
        btnMensual.setStyle(STYLE_BTN_DURACION_INACTIVO);
        btnSemestral.setStyle(STYLE_BTN_DURACION_INACTIVO);
        btnAnual.setStyle(STYLE_BTN_DURACION_ACTIVO);
        actualizarPrecios();
    }

    // ══ Seleccion de plan ═════════════════════════════════════

    @FXML
    private void handleSeleccionarSilver(ActionEvent event) {
        if (planSilver != null) abrirPagoMembresia(planSilver, duracionActual);
    }

    @FXML
    private void handleSeleccionarBlack(ActionEvent event) {
        if (planBlack != null) abrirPagoMembresia(planBlack, duracionActual);
    }

    @FXML
    private void handleSeleccionarGold(ActionEvent event) {
        if (planGold != null) abrirPagoMembresia(planGold, duracionActual);
    }

    // ══ Banner AI ════════════════════════════════════════════

    @FXML
    private void handleChatearAI(ActionEvent event) {
        navegarA("/fxml/GymbroAI.fxml", event);
    }

    // ══ Overlay PagoMembresia ═════════════════════════════════

    private void abrirPagoMembresia(PlanMembresia plan, Duracion duracion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/PagoMembresia.fxml"));
            Parent overlay = loader.load();
            PagoMembresiaController ctrl = loader.getController();

            double precio = switch (duracion) {
                case MENSUAL   -> plan.getPrecioMensual();
                case SEMESTRAL -> plan.getPrecioSemestral();
                case ANUAL     -> plan.getPrecioAnual();
            };
            String modalidad = duracion.name();
            ctrl.setPlan(plan, modalidad, precio);

            Scene scene = sideNav.getScene();
            Parent rootActual = scene.getRoot();

            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(rootActual);
            wrapper.getChildren().add(overlay);

            ctrl.setWrapperStack(wrapper, overlay);

            scene.setRoot(wrapper);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir el formulario de pago");
        }
    }

    // ══ Logica de precios ════════════════════════════════════

    private String formatPrecio(double precio) {
        long entero = Math.round(precio);
        if (entero >= 1_000_000)
            return String.format("%,.0f", precio);
        return String.format("%,d", entero);
    }

    private void actualizarPrecios() {
        if (planSilver == null || planGold == null || planBlack == null) return;

        String periodo = switch (duracionActual) {
            case MENSUAL   -> "/mes";
            case SEMESTRAL -> "/semestre";
            case ANUAL     -> "/año";
        };

        // Silver
        double precioBase = switch (duracionActual) {
            case MENSUAL   -> planSilver.getPrecioMensual();
            case SEMESTRAL -> planSilver.getPrecioSemestral();
            case ANUAL     -> planSilver.getPrecioAnual();
        };
        lblPrecioSilver.setText(formatPrecio(precioBase));
        lblPeriodoSilver.setText(periodo);
        lblFacturacionSilver.setText(textoFacturacion(planSilver, "Facturado"));

        // Gold
        precioBase = switch (duracionActual) {
            case MENSUAL   -> planGold.getPrecioMensual();
            case SEMESTRAL -> planGold.getPrecioSemestral();
            case ANUAL     -> planGold.getPrecioAnual();
        };
        lblPrecioGold.setText(formatPrecio(precioBase));
        lblPeriodoGold.setText(periodo);
        lblFacturacionGold.setText(textoFacturacion(planGold, "Facturado"));

        // Black
        precioBase = switch (duracionActual) {
            case MENSUAL   -> planBlack.getPrecioMensual();
            case SEMESTRAL -> planBlack.getPrecioSemestral();
            case ANUAL     -> planBlack.getPrecioAnual();
        };
        lblPrecioBlack.setText(formatPrecio(precioBase));
        lblPeriodoBlack.setText(periodo);
        lblFacturacionBlack.setText(textoFacturacion(planBlack, "Facturado"));
    }

    private String textoFacturacion(PlanMembresia plan, String prefijo) {
        return switch (duracionActual) {
            case MENSUAL   -> prefijo + " mensualmente";
            case SEMESTRAL -> {
                long semestral = Math.round(plan.getPrecioSemestral());
                long mensual = Math.round(plan.getPrecioSemestral() / 6);
                yield prefijo + " semestralmente a $" + formatPrecio(semestral) + " ($" + formatPrecio(mensual) + "/mes)";
            }
            case ANUAL -> {
                long anual = Math.round(plan.getPrecioAnual());
                long mensual = Math.round(plan.getPrecioAnual() / 12);
                yield prefijo + " anualmente a $" + formatPrecio(anual) + " ($" + formatPrecio(mensual) + "/mes)";
            }
        };
    }

    // ══ Tabla comparativa ════════════════════════════════════

    private void configurarTablaComparativa() {
        tablaComparativa.setStyle(
                "-fx-background-color: #1a1d21; -fx-control-inner-background: #1a1d21;" +
                        "-fx-border-color: #1f2125; -fx-border-width: 1; -fx-border-radius: 12; -fx-background-radius: 12;" +
                        "-fx-table-cell-border-color: #1f2125;"
        );
        // Column headers oscuros
        tablaComparativa.widthProperty().addListener((obs, old, w) -> {
            if (w.doubleValue() > 0) {
                var header = tablaComparativa.lookup(".column-header-background");
                if (header != null) header.setStyle("-fx-background-color: #121417;");
                var headers = tablaComparativa.lookupAll(".column-header");
                for (var h : headers) {
                    h.setStyle("-fx-background-color: #121417; -fx-border-color: #1f2125;");
                }
            }
        });

        colBeneficio.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().beneficio()));
        colSilver.setCellValueFactory(data    -> new SimpleStringProperty(data.getValue().silver()));
        colBlack.setCellValueFactory(data     -> new SimpleStringProperty(data.getValue().black()));
        colGold.setCellValueFactory(data       -> new SimpleStringProperty(data.getValue().gold()));

        TableColumn<FilaComparativa, String>[] cols = new TableColumn[]{colBeneficio, colSilver, colBlack, colGold};
        for (TableColumn<FilaComparativa, String> col : cols) {
            col.setCellFactory(c -> new TableCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) { setText(null); return; }
                    setText(item);
                    setStyle("-fx-background-color: transparent; -fx-text-fill: #d1d5db; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-padding: 10 12 10 12;");
                }
            });
        }
        // Columna Black resaltada (plan mas caro)
        colBlack.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle("-fx-background-color: transparent; -fx-text-fill: #D4FF00; -fx-font-weight: 700; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-padding: 10 12 10 12;");
            }
        });

        ObservableList<FilaComparativa> filas = FXCollections.observableArrayList();
        if (planSilver != null && planGold != null && planBlack != null) {
            Set<String> silverSet = planSilver.getBeneficios() != null
                    ? Set.of(planSilver.getBeneficios().split(" - ")) : Set.of();
            Set<String> goldRaw = planGold.getBeneficios() != null
                    ? Set.of(planGold.getBeneficios().split(" - ")) : Set.of();
            Set<String> blackRaw = planBlack.getBeneficios() != null
                    ? Set.of(planBlack.getBeneficios().split(" - ")) : Set.of();

            // Jerarquia: Gold incluye Silver, Black incluye Silver+Gold
            Set<String> goldSet = new LinkedHashSet<>();
            goldSet.addAll(silverSet);
            goldSet.addAll(goldRaw);

            Set<String> blackSet = new LinkedHashSet<>();
            blackSet.addAll(silverSet);
            blackSet.addAll(goldRaw);
            blackSet.addAll(blackRaw);

            Set<String> todos = new LinkedHashSet<>();
            todos.addAll(silverSet);
            todos.addAll(goldRaw);
            todos.addAll(blackRaw);

            for (String beneficio : todos) {
                String ini = Character.toUpperCase(beneficio.charAt(0)) + beneficio.substring(1);
                String s = silverSet.contains(beneficio) ? "✓" : "—";
                String g = goldSet.contains(beneficio)   ? "✓" : "—";
                String b = blackSet.contains(beneficio)  ? "✓" : "—";
                filas.add(new FilaComparativa(ini, s, b, g));
            }
        }
        tablaComparativa.setItems(filas);
    }

    // ══ Historial de Pagos ═══════════════════════════════════════

    private void configurarTablaHistorial() {
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

    // ══ Animaciones de navegacion ═════════════════════════════════

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navClientes, navInstructores, navProgreso, navAI, navCitas, btnLogout};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navMembresias);
    }

    private void configurarAnimacionesBotones() {
        // Botones de duracion
        agregarHoverTransparente(btnMensual);
        agregarHoverTransparente(btnSemestral);
        agregarHoverTransparente(btnAnual);

        // Botones de seleccion de plan
        agregarHoverBorde(btnSeleccionarSilver);
        agregarHoverBorde(btnSeleccionarGold);
        agregarHoverActivo(btnSeleccionarBlack);

        // Boton AI
        agregarHoverBordeAI(btnChatearAI);
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

    private void agregarHoverTransparente(Button btn) {
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03);  grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);

        btn.setOnMouseEntered(e -> {
            grow.playFromStart();
            String s = btn.getStyle();
            if (s.contains("transparent") || s.contains("#9ca3af")) {
                btn.setStyle(s
                        .replace("-fx-background-color: transparent", "-fx-background-color: #1f2226")
                        .replace("-fx-text-fill: #9ca3af", "-fx-text-fill: white"));
            }
        });
        btn.setOnMouseExited(e -> {
            shrink.playFromStart();
            String s = btn.getStyle();
            if (s.contains("#1f2226") || s.contains("white")) {
                btn.setStyle(s
                        .replace("-fx-background-color: #1f2226", "-fx-background-color: transparent")
                        .replace("-fx-text-fill: white", "-fx-text-fill: #9ca3af"));
            }
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

    private void agregarHoverBorde(Button btn) {
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03);  grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);

        btn.setOnMouseEntered(e -> {
            grow.playFromStart();
            btn.setStyle(btn.getStyle()
                    .replace("-fx-border-color: #333538", "-fx-border-color: #D4FF00")
                    .replace("-fx-text-fill: white", "-fx-text-fill: #D4FF00"));
        });
        btn.setOnMouseExited(e -> {
            shrink.playFromStart();
            btn.setStyle(btn.getStyle()
                    .replace("-fx-border-color: #D4FF00", "-fx-border-color: #333538")
                    .replace("-fx-text-fill: #D4FF00", "-fx-text-fill: white"));
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

    private void agregarHoverBordeAI(Button btn) {
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03);  grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);

        btn.setOnMouseEntered(e -> {
            grow.playFromStart();
            btn.setStyle("-fx-background-color: #00e3fd; -fx-background-radius: 20;" +
                    "-fx-font-family: 'Space Grotesk'; -fx-font-size: 12px; -fx-font-weight: 700;" +
                    "-fx-text-fill: #001f24; -fx-border-color: #00e3fd; -fx-border-width: 1;" +
                    "-fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 10 24 10 24;");
        });
        btn.setOnMouseExited(e -> {
            shrink.playFromStart();
            btn.setStyle("-fx-background-color: #0a2a30; -fx-background-radius: 20;" +
                    "-fx-font-family: 'Space Grotesk'; -fx-font-size: 12px; -fx-font-weight: 700;" +
                    "-fx-text-fill: #bdf4ff; -fx-border-color: #bdf4ff; -fx-border-width: 1;" +
                    "-fx-border-radius: 20; -fx-cursor: hand; -fx-padding: 10 24 10 24;");
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

    private void setNavActivo(Button activo) {
        Button[] todos = {navDashboard, navClientes, navInstructores, navMembresias, navProgreso, navCitas, navAI, btnLogout};
        for (Button btn : todos) {
            if (btn == activo) {
                btn.setStyle(
                        "-fx-background-color: #D4FF00; -fx-background-radius: 8;" +
                                "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 700;" +
                                "-fx-text-fill: black; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;"
                );
                agregarHoverActivo(btn);
            } else if (btn == btnLogout) {
                btn.setStyle(
                        "-fx-background-color: transparent; -fx-background-radius: 8;" +
                                "-fx-font-family: 'Lexend'; -fx-font-size: 14px; -fx-font-weight: 500;" +
                                "-fx-text-fill: #9ca3af; -fx-alignment: CENTER_LEFT; -fx-cursor: hand;"
                );
                agregarHoverInactivo(btn);
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

    // ══ Utilidades ═══════════════════════════════════════════

    private void navegarA(String fxmlPath, ActionEvent event) {
        Main.navegarA(fxmlPath);
    }

    private void mostrarInfo(String titulo, String mensaje) {
        AlertaPersonalizada.info(titulo, mensaje);
    }

    private void mostrarError(String titulo, String mensaje) {
        AlertaPersonalizada.error(titulo, mensaje);
    }

    // ══ Record interno para la tabla ═════════════════════════

    public record FilaComparativa(
            String beneficio,
            String silver,
            String black,
            String gold
    ) {}

    public record FilaPago(
            String cliente,
            String plan,
            String monto,
            String metodo,
            String fecha,
            String estado,
            String referencia
    ) {}
}