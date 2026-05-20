package org.gymbrot.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    // ─── SideNav ───────────────────────────────────────────────────────────
    @FXML private VBox sideNav;
    @FXML private Button navDashboard;
    @FXML private Button navClientes;
    @FXML private Button navInstructores;
    @FXML private Button navMembresias;
    @FXML private Button navAI;

    // ─── TopBar ────────────────────────────────────────────────────────────
    @FXML private HBox topBar;

    // ─── Métricas ──────────────────────────────────────────────────────────
    @FXML private Label lblTotalMiembros;
    @FXML private Label lblMiembrosTrend;
    @FXML private ProgressBar pbMiembros;
    @FXML private Label lblActivosAhora;
    @FXML private Rectangle dotActivos;
    @FXML private Label lblIngresos;
    @FXML private Label lblIngresosStatus;
    @FXML private Label lblIngresosProgreso;

    // ─── Gráfica Asistencia Semanal ────────────────────────────────────────
    @FXML private HBox chartAsistencia;
    @FXML private Button btnDiario;
    @FXML private Button btnSemanal;
    @FXML private Region barLun;
    @FXML private Region barMar;
    @FXML private Region barMie;
    @FXML private Region barJue;
    @FXML private Region barVie;
    @FXML private Region barSab;
    @FXML private Region barDom;

    // ─── Demografía ────────────────────────────────────────────────────────
    @FXML private Label lblPctAdulto;
    @FXML private Label lblCntAdulto;
    @FXML private Label lblPctMenor;
    @FXML private Label lblCntMenor;
    @FXML private Label lblPctSenior;
    @FXML private Label lblCntSenior;

    // ─── Horas Pico ────────────────────────────────────────────────────────
    @FXML private HBox chartHorasPico;
    @FXML private Region h06, h07, h08, h09, h10, h11;
    @FXML private Region h12, h13, h14, h15, h16, h17;
    @FXML private Region h18, h19, h20, h21;

    // ─── Constantes ────────────────────────────────────────────────────────
    private static final String COLOR_ACTIVO   = "#D4FF00";
    private static final String COLOR_INACTIVO = "#2a4a50";
    private static final String COLOR_ALTO     = "#D4FF00";
    private static final String COLOR_MODERADO = "#3a5a60";
    private static final String COLOR_BAJO     = "#2a2d30";
    private static final double META_INGRESOS_MES = 50000.0;
    private static final int    META_MIEMBROS     = 3500;

    // ═══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarAnimacionesNav();
        configurarAnimacionesBotones();
        setNavActivo(navDashboard);
        cargarMetricas();
        cargarAsistenciaSemanal();
        cargarDemografia();
        cargarHorasPico();
        iniciarAnimacionPuntoVivo();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMACIONES NAV — idénticas a GestionClientes
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navClientes, navInstructores, navMembresias, navAI};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navDashboard);
    }

    private void configurarAnimacionesBotones() {
        agregarHoverActivo(btnSemanal);
        agregarHoverInactivo(btnDiario);
    }

    private void agregarHoverInactivo(Button btn) {
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03);  grow.setToY(1.03);
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
        Button[] todos = {navDashboard, navClientes, navInstructores, navMembresias, navAI};
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
    //  MÉTRICAS
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarMetricas() {
        // TODO: reemplazar con llamada al DAO
        // Query: SELECT COUNT(*) FROM USUARIOS WHERE tipo_usuario='CLIENTE' AND estado='ACTIVO'
        int totalMiembros    = 2842;
        int totalMesAnterior = 2537;
        double trend = ((double)(totalMiembros - totalMesAnterior) / totalMesAnterior) * 100;

        lblTotalMiembros.setText(String.format("%,d", totalMiembros));
        lblMiembrosTrend.setText(String.format("%.0f%%", trend));
        pbMiembros.setProgress((double) totalMiembros / META_MIEMBROS);

        // Query: SELECT COUNT(*) FROM REGISTROS_INGRESOS
        //        WHERE fecha = TRUNC(SYSDATE) AND hora_salida IS NULL
        lblActivosAhora.setText("184");

        // Query: SELECT NVL(SUM(valor),0) FROM PAGOS
        //        WHERE EXTRACT(MONTH FROM fecha_pago) = EXTRACT(MONTH FROM SYSDATE)
        //        AND estado_pago = 'COMPLETADO'
        double ingresosMes = 42800.0;
        double progreso    = ingresosMes / META_INGRESOS_MES;
        lblIngresos.setText(formatearDinero(ingresosMes));
        lblIngresosProgreso.setText(String.format("%.0f%%", progreso * 100));
        lblIngresosStatus.setText(progreso >= 1.0 ? "Meta Alcanzada" : String.format("%.0f%% meta", progreso * 100));
    }

    private String formatearDinero(double valor) {
        if (valor >= 1_000_000) return String.format("$%.1fM", valor / 1_000_000);
        if (valor >= 1_000)     return String.format("$%.1fk", valor / 1_000);
        return String.format("$%.0f", valor);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ASISTENCIA SEMANAL
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarAsistenciaSemanal() {
        // TODO: Query SELECT TO_CHAR(fecha,'D'), COUNT(*) FROM REGISTROS_INGRESOS
        //       WHERE fecha >= TRUNC(SYSDATE)-6 GROUP BY TO_CHAR(fecha,'D')
        int[] asistencia = {120, 185, 230, 198, 160, 245, 175};
        actualizarBarrasAsistencia(asistencia);
    }

    private void actualizarBarrasAsistencia(int[] valores) {
        Region[] barras = {barLun, barMar, barMie, barJue, barVie, barSab, barDom};
        double maxValor = 0;
        for (int v : valores) if (v > maxValor) maxValor = v;
        double alturaMax = 200.0;

        for (int i = 0; i < barras.length; i++) {
            double altura = (valores[i] / maxValor) * alturaMax;
            barras[i].setPrefHeight(altura);
            barras[i].setMaxHeight(altura);

            String color = (valores[i] == (int) maxValor) ? COLOR_ACTIVO : COLOR_INACTIVO;
            barras[i].setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

            // Animación de entrada — igual que GestionClientes
            FadeTransition ft = new FadeTransition(Duration.millis(400), barras[i]);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 60));
            ft.play();

            // Hover sobre cada barra
            final int    idx        = i;
            final String colorFinal = color;
            barras[i].setOnMouseEntered(e -> {
                barras[idx].setStyle(
                        "-fx-background-color: " + COLOR_ACTIVO + "; -fx-background-radius: 2;" +
                                "-fx-effect: dropshadow(gaussian, #D4FF00, 8, 0.3, 0, 0);");
                barras[idx].setScaleY(1.04);
            });
            barras[i].setOnMouseExited(e -> {
                barras[idx].setStyle("-fx-background-color: " + colorFinal + "; -fx-background-radius: 2;");
                barras[idx].setScaleY(1.0);
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DEMOGRAFÍA
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarDemografia() {
        // TODO: Query SELECT categoria, COUNT(*) FROM REGISTROS_INGRESOS JOIN USUARIOS...
        int menores = 33, adultos = 120, seniors = 31;
        int total   = menores + adultos + seniors;

        if (lblPctAdulto != null) lblPctAdulto.setText(String.format("%.0f%%", (adultos  * 100.0) / total));
        if (lblCntAdulto != null) lblCntAdulto.setText(adultos  + " presentes");
        if (lblPctMenor  != null) lblPctMenor.setText(String.format("%.0f%%",  (menores  * 100.0) / total));
        if (lblCntMenor  != null) lblCntMenor.setText(menores   + " presentes");
        if (lblPctSenior != null) lblPctSenior.setText(String.format("%.0f%%", (seniors  * 100.0) / total));
        if (lblCntSenior != null) lblCntSenior.setText(seniors   + " presentes");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HORAS PICO
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarHorasPico() {
        // TODO: Query SELECT EXTRACT(HOUR FROM hora_entrada), COUNT(*)
        //       FROM REGISTROS_INGRESOS GROUP BY EXTRACT(HOUR FROM hora_entrada)
        int[] horasPico = {40, 70, 100, 130, 190, 160, 120, 90, 80, 84, 110, 170, 180, 120, 60, 30};
        actualizarBarrasHorasPico(horasPico);
    }

    private void actualizarBarrasHorasPico(int[] valores) {
        Region[] barras = {h06, h07, h08, h09, h10, h11, h12, h13, h14, h15, h16, h17, h18, h19, h20, h21};
        double maxValor = 0;
        for (int v : valores) if (v > maxValor) maxValor = v;

        double alturaMax      = 192.0;
        double umbralAlto     = maxValor * 0.75;
        double umbralModerado = maxValor * 0.45;

        for (int i = 0; i < barras.length; i++) {
            double altura = (valores[i] / maxValor) * alturaMax;
            barras[i].setPrefHeight(altura);
            barras[i].setMaxHeight(altura);

            String color;
            if      (valores[i] >= umbralAlto)     color = COLOR_ALTO;
            else if (valores[i] >= umbralModerado) color = COLOR_MODERADO;
            else                                    color = COLOR_BAJO;

            barras[i].setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3 3 0 0;");

            // Animación de entrada escalonada — igual que GestionClientes
            FadeTransition ft = new FadeTransition(Duration.millis(350), barras[i]);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 40));
            ft.play();

            // Hover sobre cada barra del histograma
            final String colorFinal = color;
            final int    idx        = i;
            barras[i].setOnMouseEntered(e -> {
                barras[idx].setStyle(
                        "-fx-background-color: " + COLOR_ALTO + "; -fx-background-radius: 3 3 0 0;" +
                                "-fx-effect: dropshadow(gaussian, #D4FF00, 8, 0.3, 0, 0);");
                barras[idx].setScaleY(1.04);
            });
            barras[i].setOnMouseExited(e -> {
                barras[idx].setStyle("-fx-background-color: " + colorFinal + "; -fx-background-radius: 3 3 0 0;");
                barras[idx].setScaleY(1.0);
            });
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMACIÓN PUNTO EN VIVO — idéntica a GestionClientes
    // ═══════════════════════════════════════════════════════════════════════

    private void iniciarAnimacionPuntoVivo() {
        if (dotActivos == null) return;
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,        e -> dotActivos.setOpacity(1.0)),
                new KeyFrame(Duration.millis(600),  e -> dotActivos.setOpacity(0.2)),
                new KeyFrame(Duration.millis(1200), e -> dotActivos.setOpacity(1.0))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — BOTONES VIEW (Diario / Semanal)
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleViewDiario() {
        // TODO: cargar datos de ingresos del dia de hoy
        int[] datosDiario = {10, 25, 45, 60, 85, 70, 50, 40, 35, 38, 48, 75, 80, 55, 30, 15};
        actualizarBarrasAsistencia(datosDiario);

        btnDiario.setStyle(
                "-fx-background-color: #D4FF00; -fx-background-radius: 8;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-text-fill: black; -fx-cursor: hand;"
        );
        btnSemanal.setStyle(
                "-fx-background-color: #282a2d; -fx-background-radius: 8;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-text-fill: white; -fx-border-color: #333538; -fx-border-width: 1;" +
                        "-fx-border-radius: 8; -fx-cursor: hand;"
        );
        agregarHoverActivo(btnDiario);
        agregarHoverInactivo(btnSemanal);
    }

    @FXML
    private void handleViewSemanal() {
        cargarAsistenciaSemanal();

        btnSemanal.setStyle(
                "-fx-background-color: #D4FF00; -fx-background-radius: 8;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-text-fill: black; -fx-cursor: hand;"
        );
        btnDiario.setStyle(
                "-fx-background-color: #282a2d; -fx-background-radius: 8;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                        "-fx-text-fill: white; -fx-border-color: #333538; -fx-border-width: 1;" +
                        "-fx-border-radius: 8; -fx-cursor: hand;"
        );
        agregarHoverActivo(btnSemanal);
        agregarHoverInactivo(btnDiario);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — NAV
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handleNavDashboard()    { setNavActivo(navDashboard); }
    @FXML private void handleNavClientes()     { navegarA("/fxml/GestionClientes.fxml"); }
    @FXML private void handleNavInstructores() {  }
    @FXML private void handleNavMembresias()   {  }
    @FXML private void handleNavAI()           {  }

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
    //  HANDLERS — ACCIONES RÁPIDAS
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handleVerRegistro()     {  }
    @FXML private void handleAgregarMiembro()  { navegarA("/fxml/NuevoCliente.fxml"); }
    @FXML private void handleExportarInforme() {  }

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
        }
    }

}