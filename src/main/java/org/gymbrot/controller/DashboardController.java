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

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
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

    // ─── DAOs ──────────────────────────────────────────────────────────────
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final RegistroIngresoDAO registroIngresoDAO = new RegistroIngresoDAO();
    private final PagoDAO pagoDAO = new PagoDAO();

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
        List<Cliente> clientes = clienteDAO.listarTodos();
        int totalActive = 0;
        for (Cliente c : clientes) {
            if ("ACTIVO".equalsIgnoreCase(c.getEstado())) totalActive++;
        }
        lblTotalMiembros.setText(String.valueOf(totalActive));
        lblMiembrosTrend.setText(clientes.size() + " total");
        pbMiembros.setProgress(Math.min(1.0, (double) totalActive / META_MIEMBROS));

        LocalDate hoy = LocalDate.now();
        List<RegistroIngreso> ingresosHoy = registroIngresoDAO.listarPorFecha(hoy);
        long activosAhora = ingresosHoy.stream().filter(r -> r.getHoraSalida() == null).count();
        lblActivosAhora.setText(String.valueOf(activosAhora));

        double ingresosMes = 0;
        List<org.gymbrot.model.Pago> pagos = pagoDAO.listarTodos();
        LocalDate inicioMes = hoy.withDayOfMonth(1);
        for (var pago : pagos) {
            if (pago.getFechaPago() != null && !pago.getFechaPago().isBefore(inicioMes)
                    && "EXITOSO".equalsIgnoreCase(pago.getEstadoPago())) {
                ingresosMes += pago.getValor();
            }
        }
        lblIngresos.setText(formatearDinero(ingresosMes));
        lblIngresosProgreso.setVisible(false);
        lblIngresosStatus.setVisible(false);
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
        int[] asistencia = new int[7];
        LocalDate hoy = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate dia = hoy.minusDays(i);
            List<RegistroIngreso> ingresos = registroIngresoDAO.listarPorFecha(dia);
            asistencia[6 - i] = ingresos.size();
        }
        actualizarBarrasAsistencia(asistencia);
    }

    private void actualizarBarrasAsistencia(int[] valores) {
        Region[] barras = {barLun, barMar, barMie, barJue, barVie, barSab, barDom};
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
        LocalDate hoy = LocalDate.now();
        List<RegistroIngreso> ingresosHoy = registroIngresoDAO.listarPorFecha(hoy);
        int menores = 0, adultos = 0, seniors = 0;
        for (RegistroIngreso ri : ingresosHoy) {
            Cliente c = clienteDAO.buscarPorId(ri.getIdCliente());
            if (c == null || c.getFechaNacimiento() == null) continue;
            int edad = (int) ChronoUnit.YEARS.between(c.getFechaNacimiento(), hoy);
            if (edad < 18)       menores++;
            else if (edad <= 55) adultos++;
            else                 seniors++;
        }
        int total = menores + adultos + seniors;
        if (total == 0) total = 1;

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
    @FXML private void handleNavProgreso()     {  }

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