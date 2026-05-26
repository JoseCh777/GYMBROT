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
import org.gymbrot.dao.*;
import org.gymbrot.model.*;
import org.gymbrot.service.DashboardService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

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

    // ─── Métricas ──────────────────────────────────────────────────────────
    @FXML private Label lblTotalMiembros;
    @FXML private Label lblActivosAhora;
    @FXML private Rectangle dotActivos;
    @FXML private Label lblIngresos;
    @FXML private Label lblIngresosStatus;

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

    // ─── Días Semana ───────────────────────────────────────────────────────
    @FXML private Label lblDiaLun;
    @FXML private Label lblDiaMar;
    @FXML private Label lblDiaMie;
    @FXML private Label lblDiaJue;
    @FXML private Label lblDiaVie;
    @FXML private Label lblDiaSab;
    @FXML private Label lblDiaDom;

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

    // ─── DAOs / Services ────────────────────────────────────────────────────
    private final RegistroIngresoDAO registroIngresoDAO = new RegistroIngresoDAO();
    private final DashboardService dashboardService = new DashboardService();

    // ─── Constantes ────────────────────────────────────────────────────────
    private static final String COLOR_ACTIVO   = "#D4FF00";
    private static final String COLOR_INACTIVO = "#2a4a50";
    private static final String COLOR_ALTO     = "#D4FF00";
    private static final String COLOR_MODERADO = "#3a5a60";
    private static final String COLOR_BAJO     = "#2a2d30";
    private static final double META_INGRESOS_MES = 50000.0;

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
        Button[] inactivos = {navClientes, navInstructores, navMembresias, navProgreso, navAI};
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
    //  MÉTRICAS
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarMetricas() {
        int totalActive = dashboardService.contarMiembrosActivos();
        lblTotalMiembros.setText(String.valueOf(totalActive));

        int activosAhora = dashboardService.contarActivosHoy();
        lblActivosAhora.setText(String.valueOf(activosAhora));

        double ingresosMes = dashboardService.ingresosMesActual();
        lblIngresos.setText(formatearDinero(ingresosMes));
        lblIngresosStatus.setVisible(false);
    }

    private String formatearDinero(double valor) {
        return String.format("$%,.0f", valor);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ASISTENCIA SEMANAL
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarAsistenciaSemanal() {
        int[] asistencia = new int[7];
        LocalDate hoy = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            int diaSemana = dia.getDayOfWeek().getValue() - 1; // 0=LUN, 6=DOM
            List<RegistroIngreso> ingresos = registroIngresoDAO.listarPorFecha(dia);
            asistencia[diaSemana] = ingresos.size();
        }
        actualizarBarrasAsistencia(asistencia);
    }

    private void actualizarBarrasAsistencia(int[] valores) {
        Region[] barras = {barLun, barMar, barMie, barJue, barVie, barSab, barDom};
        String[] diasSemana = {"LUN", "MAR", "MIE", "JUE", "VIE", "SAB", "DOM"};
        double maxValor = 0;
        for (int v : valores) if (v > maxValor) maxValor = v;
        double alturaMax = 200.0;
        int hoyIdx = LocalDate.now().getDayOfWeek().getValue() - 1;

        for (int i = 0; i < barras.length; i++) {
            double altura = maxValor > 0 ? (valores[i] / maxValor) * alturaMax : 0;
            barras[i].setPrefHeight(altura);
            barras[i].setMaxHeight(altura);

            String color = (i == hoyIdx) ? COLOR_ACTIVO : COLOR_INACTIVO;
            barras[i].setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");

            // Animación de entrada — igual que GestionClientes
            FadeTransition ft = new FadeTransition(Duration.millis(400), barras[i]);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 60));
            ft.play();

            // Tooltip con número de ingresos — instalado en el VBox padre (target más ancho)
            Tooltip t = new Tooltip(diasSemana[i] + " — " + valores[i] + " ingresos");
            t.setShowDelay(Duration.millis(200));
            Tooltip.install(barras[i].getParent(), t);

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

        // Resaltar el día de hoy
        Label[] dias = {lblDiaLun, lblDiaMar, lblDiaMie, lblDiaJue, lblDiaVie, lblDiaSab, lblDiaDom};
        for (int i = 0; i < dias.length; i++) {
            if (i == hoyIdx) {
                dias[i].setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: " + COLOR_ACTIVO + ";");
            } else {
                dias[i].setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700; -fx-text-fill: #6b7280;");
            }
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DEMOGRAFÍA
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarDemografia() {
        int[] data = dashboardService.cargarDemografia();
        int menores = data[0], adultos = data[1], adultosMayores = data[2];
        int total = menores + adultos + adultosMayores;
        if (total == 0) total = 1;

        if (lblPctAdulto != null) lblPctAdulto.setText(String.format("%.0f%%", (adultos  * 100.0) / total));
        if (lblCntAdulto != null) lblCntAdulto.setText(adultos  + " miembros");
        if (lblPctMenor  != null) lblPctMenor.setText(String.format("%.0f%%",  (menores  * 100.0) / total));
        if (lblCntMenor  != null) lblCntMenor.setText(menores   + " miembros");
        if (lblPctSenior != null) lblPctSenior.setText(String.format("%.0f%%", (adultosMayores  * 100.0) / total));
        if (lblCntSenior != null) lblCntSenior.setText(adultosMayores   + " miembros");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HORAS PICO
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarHorasPico() {
        LocalDate hoy = LocalDate.now();
        List<RegistroIngreso> ingresos = registroIngresoDAO.listarPorFecha(hoy);
        int[] horasPico = new int[16];
        for (RegistroIngreso ri : ingresos) {
            if (ri.getHoraEntrada() != null) {
                int h = ri.getHoraEntrada().getHour();
                if (h >= 6 && h <= 21) horasPico[h - 6]++;
            }
        }
        actualizarBarrasHorasPico(horasPico);
    }

    private void actualizarBarrasHorasPico(int[] valores) {
        Region[] barras = {h06, h07, h08, h09, h10, h11, h12, h13, h14, h15, h16, h17, h18, h19, h20, h21};
        String[] horas = {"06:00","07:00","08:00","09:00","10:00","11:00","12:00","13:00",
                          "14:00","15:00","16:00","17:00","18:00","19:00","20:00","21:00"};
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

            // Tooltip con número de ingresos por hora
            Tooltip t = new Tooltip(horas[i] + " — " + valores[i] + " ingresos");
            t.setShowDelay(Duration.millis(200));
            Tooltip.install(barras[i], t);

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
    //  ANIMACIÓN PUNTO EN VIVO
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
        LocalDate hoy = LocalDate.now();
        List<RegistroIngreso> ingresos = registroIngresoDAO.listarPorFecha(hoy);
        int[] datosDiario = new int[7];
        for (int i = 0; i < 7; i++) datosDiario[i] = 0;
        for (RegistroIngreso ri : ingresos) {
            if (ri.getHoraEntrada() != null) {
                int diaSemana = ri.getHoraEntrada().getDayOfWeek().getValue() - 1;
                if (diaSemana >= 0 && diaSemana < 7) datosDiario[diaSemana]++;
            }
        }
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
    @FXML private void handleNavInstructores() { navegarA("/fxml/GestionInstructores.fxml"); }
    @FXML private void handleNavMembresias()   { navegarA("/fxml/GestionMembresias.fxml");}
    @FXML private void handleNavAI()           { navegarA("/fxml/GymbroAI.fxml"); }
    @FXML private void handleNavProgreso()     { navegarA("/fxml/ProgresoFisico.fxml"); }

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
    //  HANDLERS — ACCIONES RÁPIDAS
    // ═══════════════════════════════════════════════════════════════════════

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