package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class GestionMembresiasController implements Initializable {

    // ── SideNav ──────────────────────────────────────────────
    @FXML private Button navDashboard;
    @FXML private Button navClientes;
    @FXML private Button navInstructores;
    @FXML private Button navMembresias;
    @FXML private Button navAI;

    // ── Selector de duracion ─────────────────────────────────
    @FXML private Button btnMensual;
    @FXML private Button btnSemestral;
    @FXML private Button btnAnual;

    // ── Labels de precios ────────────────────────────────────
    @FXML private Label lblPrecioBronce;
    @FXML private Label lblPrecioPlata;
    @FXML private Label lblPrecioOro;

    @FXML private Label lblFacturacionBronce;
    @FXML private Label lblFacturacionPlata;
    @FXML private Label lblFacturacionOro;

    // ── Botones de seleccion de plan ─────────────────────────
    @FXML private Button btnSeleccionarBronce;
    @FXML private Button btnSeleccionarPlata;
    @FXML private Button btnSeleccionarOro;

    // ── Tabla comparativa ────────────────────────────────────
    @FXML private TableView<FilaComparativa> tablaComparativa;
    @FXML private TableColumn<FilaComparativa, String> colBeneficio;
    @FXML private TableColumn<FilaComparativa, String> colBronce;
    @FXML private TableColumn<FilaComparativa, String> colPlata;
    @FXML private TableColumn<FilaComparativa, String> colOro;

    // ── Banner AI ────────────────────────────────────────────
    @FXML private Button btnChatearAI;

    @FXML private Button btnLogout;

    // ── Estado interno ───────────────────────────────────────
    private enum Duracion { MENSUAL, SEMESTRAL, ANUAL }
    private Duracion duracionActual = Duracion.ANUAL;

    // Precios base (mensual)
    private static final int PRECIO_BRONCE   = 61;
    private static final int PRECIO_PLATA    = 111;
    private static final int PRECIO_ORO      = 161;

    // Descuentos
    private static final double DESC_SEMESTRAL = 0.10;
    private static final double DESC_ANUAL     = 0.20;

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
        configurarAnimacionesNav();
        setNavActivo(navMembresias);
        configurarAnimacionesBotones();
        configurarTablaComparativa();
        actualizarPrecios();
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
    private void handleLogout(ActionEvent event) {
        navegarA("/fxml/Login.fxml", event);
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
    private void handleSeleccionarBronce(ActionEvent event) {
        System.out.println("Plan seleccionado: Bronce | Duracion: " + duracionActual);
        // TODO: abrir formulario de inscripcion con plan = BRONCE
    }

    @FXML
    private void handleSeleccionarPlata(ActionEvent event) {
        System.out.println("Plan seleccionado: Plata | Duracion: " + duracionActual);
        // TODO: abrir formulario de inscripcion con plan = PLATA
    }

    @FXML
    private void handleSeleccionarOro(ActionEvent event) {
        System.out.println("Plan seleccionado: Oro Elite | Duracion: " + duracionActual);
        // TODO: abrir formulario de inscripcion con plan = ORO
    }

    // ══ Banner AI ════════════════════════════════════════════

    @FXML
    private void handleChatearAI(ActionEvent event) {
        navegarA("/fxml/GymbroAI.fxml", event);
    }

    // ══ Logica de precios ════════════════════════════════════

    private void actualizarPrecios() {
        double factor = switch (duracionActual) {
            case MENSUAL   -> 1.0;
            case SEMESTRAL -> 1.0 - DESC_SEMESTRAL;
            case ANUAL     -> 1.0 - DESC_ANUAL;
        };

        int precioBronce = (int) Math.round(PRECIO_BRONCE * factor);
        int precioPlata  = (int) Math.round(PRECIO_PLATA  * factor);
        int precioOro    = (int) Math.round(PRECIO_ORO    * factor);

        lblPrecioBronce.setText(String.valueOf(precioBronce));
        lblPrecioPlata.setText(String.valueOf(precioPlata));
        lblPrecioOro.setText(String.valueOf(precioOro));

        lblFacturacionBronce.setText(textoFacturacion(precioBronce, "Facturado"));
        lblFacturacionPlata.setText(textoFacturacion(precioPlata, "Facturado"));
        lblFacturacionOro.setText(textoFacturacion(precioOro, "Mejor Valor: Facturado"));
    }

    private String textoFacturacion(int precioMensual, String prefijo) {
        return switch (duracionActual) {
            case MENSUAL   -> prefijo + " mensualmente";
            case SEMESTRAL -> prefijo + " semestralmente a USD " + (precioMensual * 6) + "/semestre";
            case ANUAL     -> prefijo + " anualmente a USD " + (precioMensual * 12) + "/año";
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
        colBronce.setCellValueFactory(data    -> new SimpleStringProperty(data.getValue().bronce()));
        colPlata.setCellValueFactory(data     -> new SimpleStringProperty(data.getValue().plata()));
        colOro.setCellValueFactory(data       -> new SimpleStringProperty(data.getValue().oro()));

        TableColumn<FilaComparativa, String>[] cols = new TableColumn[]{colBeneficio, colBronce, colPlata, colOro};
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
        // Columna Oro resaltada
        colOro.setCellFactory(c -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setText(null); return; }
                setText(item);
                setStyle("-fx-background-color: transparent; -fx-text-fill: #D4FF00; -fx-font-weight: 700; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-padding: 10 12 10 12;");
            }
        });

        ObservableList<FilaComparativa> filas = FXCollections.observableArrayList(
            new FilaComparativa("Acceso al Gimnasio",              "Diurno",           "Extendido",         "24/7 Ultra"),
            new FilaComparativa("Herramientas de IA",              "Basico",           "Avanzado",          "Tiempo Real"),
            new FilaComparativa("Sesiones con Instructor",         "1 al mes",         "Digital + Grupal",  "1-a-1 Elite"),
            new FilaComparativa("Suite de Recuperacion",           "No incluida",      "Hidromasaje/Sauna", "Crioterapia"),
            new FilaComparativa("Locker Personal y Lavanderia",    "No incluida",      "No incluida",       "Incluida"),
            new FilaComparativa("Pases de Invitado",               "No incluidos",     "No incluidos",      "Ilimitados"),
            new FilaComparativa("Seguimiento Nutricional",         "No incluido",      "Incluido",          "Incluido")
        );

        tablaComparativa.setItems(filas);
    }

    // ══ Animaciones de navegacion ═════════════════════════════════

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navClientes, navInstructores, navAI, btnLogout};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navMembresias);
    }

    private void configurarAnimacionesBotones() {
        // Botones de duracion
        agregarHoverTransparente(btnMensual);
        agregarHoverTransparente(btnSemestral);
        agregarHoverTransparente(btnAnual);

        // Botones de seleccion de plan
        agregarHoverBorde(btnSeleccionarBronce);
        agregarHoverBorde(btnSeleccionarPlata);
        agregarHoverActivo(btnSeleccionarOro);

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
        Button[] todos = {navDashboard, navClientes, navInstructores, navMembresias, navAI, btnLogout};
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
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) navDashboard.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Error al navegar a: " + fxmlPath);
            e.printStackTrace();
        }
    }

    // ══ Record interno para la tabla ═════════════════════════

    public record FilaComparativa(
        String beneficio,
        String bronce,
        String plata,
        String oro
    ) {}
}
