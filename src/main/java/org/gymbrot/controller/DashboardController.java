package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * DashboardController — Titan Command
 *
 * Estructura lista para conectar con Oracle cuando agregues las capas DAO/Service.
 * Busca los comentarios "TODO: reemplazar con llamada al DAO" para saber dónde conectar.
 *
 * Tablas que alimentan este dashboard:
 *  - USUARIOS + CLIENTES          → Total de miembros
 *  - REGISTROS_INGRESOS           → Activos ahora (ingresos del día sin hora_salida)
 *  - PAGOS                        → Ingresos del mes
 *  - REGISTROS_INGRESOS por fecha → Asistencia semanal y horas pico
 *  - CLIENTES + USUARIOS          → Demografía (por fecha_nacimiento)
 *  - HISTORIAL_MEMBRESIAS         → Membresías activas
 */
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
    @FXML private TextField searchField;
    @FXML private ImageView avatarImage;

    // ─── Métricas ──────────────────────────────────────────────────────────
    @FXML private Label lblTotalMiembros;      // USUARIOS WHERE tipo_usuario = 'CLIENTE'
    @FXML private Label lblMiembrosTrend;      // Comparación con mes anterior
    @FXML private ProgressBar pbMiembros;      // progreso hacia meta de miembros

    @FXML private Label lblActivosAhora;       // REGISTROS_INGRESOS WHERE fecha = HOY AND hora_salida IS NULL
    @FXML private Rectangle dotActivos;        // punto pulsante EN VIVO

    @FXML private Label lblIngresos;           // SUM(valor) FROM PAGOS WHERE mes actual
    @FXML private Label lblIngresosStatus;     // "Meta Alcanzada" o "En progreso"
    @FXML private Label lblIngresosProgreso;   // porcentaje hacia la meta

    // ─── Gráfica Asistencia Semanal ────────────────────────────────────────
    @FXML private HBox chartAsistencia;
    @FXML private Button btnDiario;
    @FXML private Button btnSemanal;

    // Barras del chart — altura viene de COUNT(*) en REGISTROS_INGRESOS por día
    @FXML private Region barLun;
    @FXML private Region barMar;
    @FXML private Region barMie;
    @FXML private Region barJue;
    @FXML private Region barVie;
    @FXML private Region barSab;
    @FXML private Region barDom;

    // ─── Demografía ────────────────────────────────────────────────────────
    // Calculado desde USUARIOS.fecha_nacimiento + REGISTROS_INGRESOS (activos hoy)
    @FXML private Label lblPctAdulto;     // 18-55 años
    @FXML private Label lblCntAdulto;
    @FXML private Label lblPctMenor;      // < 18 años
    @FXML private Label lblCntMenor;
    @FXML private Label lblPctSenior;     // > 55 años
    @FXML private Label lblCntSenior;

    // ─── Horas Pico ────────────────────────────────────────────────────────
    // COUNT(*) FROM REGISTROS_INGRESOS GROUP BY EXTRACT(HOUR FROM hora_entrada)
    @FXML private HBox chartHorasPico;
    @FXML private Region h06, h07, h08, h09, h10, h11;
    @FXML private Region h12, h13, h14, h15, h16, h17;
    @FXML private Region h18, h19, h20, h21;

    // ─── Constantes de diseño ──────────────────────────────────────────────
    private static final String COLOR_ACTIVO   = "#D4FF00";
    private static final String COLOR_INACTIVO = "#2a4a50";
    private static final String COLOR_ALTO     = "#D4FF00";
    private static final String COLOR_MODERADO = "#3a5a60";
    private static final String COLOR_BAJO     = "#2a2d30";

    // Meta mensual de ingresos (reemplazar con config de BD cuando exista)
    private static final double META_INGRESOS_MES = 50000.0;
    // Meta de miembros totales
    private static final int META_MIEMBROS = 3500;

    // ═══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════════════════
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

        // 1. Animaciones de hover en botones del nav
        configurarAnimacionesNav();

        // 2. Cargar datos en las métricas
        cargarMetricas();

        // 3. Cargar gráfica de asistencia semanal
        cargarAsistenciaSemanal();

        // 4. Cargar demografía
        cargarDemografia();

        // 5. Cargar horas pico
        cargarHorasPico();

        // 6. Animación del punto "EN VIVO"
        iniciarAnimacionPuntoVivo();

        // 7. Marcar el nav activo
        setNavActivo(navDashboard);
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMACIONES NAV
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarAnimacionesNav() {
        // Botones inactivos — escala + cambio de color
        Button[] botonesInactivos = {navClientes, navInstructores, navMembresias, navAI};
        for (Button btn : botonesInactivos) {
            agregarHoverInactivo(btn);
        }
        // Botón activo — escala suave
        agregarHoverActivo(navDashboard);
    }

    /**
     * Hover para botones inactivos del nav:
     * - Al entrar: crece ligeramente y aclara el fondo
     * - Al salir: regresa al estado original
     * - Al click: se comprime brevemente
     */
    private void agregarHoverInactivo(Button btn) {
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition press  = new ScaleTransition(Duration.millis(80),  btn);
        ScaleTransition release = new ScaleTransition(Duration.millis(80), btn);

        grow.setToX(1.03);
        grow.setToY(1.03);
        shrink.setToX(1.0);
        shrink.setToY(1.0);
        press.setToX(0.96);
        press.setToY(0.96);
        release.setToX(1.0);
        release.setToY(1.0);

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

        btn.setOnMousePressed(e  -> press.playFromStart());
        btn.setOnMouseReleased(e -> release.playFromStart());
    }

    /**
     * Hover para el botón activo (amarillo):
     * - Leve brillo al pasar
     * - Compresión al hacer click
     */
    private void agregarHoverActivo(Button btn) {
        ScaleTransition grow    = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink  = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition press   = new ScaleTransition(Duration.millis(80),  btn);
        ScaleTransition release = new ScaleTransition(Duration.millis(80),  btn);

        grow.setToX(1.03);
        grow.setToY(1.03);
        shrink.setToX(1.0);
        shrink.setToY(1.0);
        press.setToX(0.97);
        press.setToY(0.97);
        release.setToX(1.0);
        release.setToY(1.0);

        btn.setOnMouseEntered(e  -> grow.playFromStart());
        btn.setOnMouseExited(e   -> shrink.playFromStart());
        btn.setOnMousePressed(e  -> press.playFromStart());
        btn.setOnMouseReleased(e -> release.playFromStart());
    }

    /** Marca visualmente el botón activo en el nav */
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
        // ── Total de Miembros ──
        // TODO: reemplazar con llamada al DAO
        // int total = clienteDAO.contarClientesActivos();
        // Query sugerida:
        // SELECT COUNT(*) FROM USUARIOS WHERE tipo_usuario = 'CLIENTE' AND estado = 'ACTIVO'
        int totalMiembros = 2842;
        int totalMesAnterior = 2537; // para calcular el trend
        double trend = ((double)(totalMiembros - totalMesAnterior) / totalMesAnterior) * 100;

        lblTotalMiembros.setText(String.format("%,d", totalMiembros));
        lblMiembrosTrend.setText(String.format("%.0f%%", trend));
        pbMiembros.setProgress((double) totalMiembros / META_MIEMBROS);

        // ── Activos Ahora ──
        // TODO: reemplazar con llamada al DAO
        // int activos = ingresoDAO.contarActivosAhora();
        // Query sugerida:
        // SELECT COUNT(*) FROM REGISTROS_INGRESOS
        // WHERE fecha = TRUNC(SYSDATE) AND hora_salida IS NULL AND estado_verificacion = 'VERIFICADO'
        int activosAhora = 184;
        lblActivosAhora.setText(String.valueOf(activosAhora));

        // ── Ingresos del Mes ──
        // TODO: reemplazar con llamada al DAO
        // double ingresos = pagoDAO.sumarIngresosMesActual();
        // Query sugerida:
        // SELECT NVL(SUM(valor), 0) FROM PAGOS
        // WHERE EXTRACT(MONTH FROM fecha_pago) = EXTRACT(MONTH FROM SYSDATE)
        // AND EXTRACT(YEAR FROM fecha_pago) = EXTRACT(YEAR FROM SYSDATE)
        // AND estado_pago = 'COMPLETADO'
        double ingresosMes = 42800.0;
        double progreso = ingresosMes / META_INGRESOS_MES;

        lblIngresos.setText(formatearDinero(ingresosMes));
        lblIngresosProgreso.setText(String.format("%.0f%%", progreso * 100));

        if (progreso >= 1.0) {
            lblIngresosStatus.setText("Meta Alcanzada");
            lblIngresosStatus.setStyle(lblIngresosStatus.getStyle()
                    .replace("-fx-text-fill: #D4FF00", "-fx-text-fill: #D4FF00"));
        } else {
            lblIngresosStatus.setText(String.format("%.0f%% de la meta", progreso * 100));
        }
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
        // TODO: reemplazar con llamada al DAO
        // int[] datos = ingresoDAO.contarIngresosPorDiaSemana();
        // Query sugerida:
        // SELECT TO_CHAR(fecha, 'D') as dia, COUNT(*) as cantidad
        // FROM REGISTROS_INGRESOS
        // WHERE fecha >= TRUNC(SYSDATE) - 6
        // GROUP BY TO_CHAR(fecha, 'D')
        // ORDER BY TO_CHAR(fecha, 'D')

        // Datos mock [LUN, MAR, MIE, JUE, VIE, SAB, DOM]
        int[] asistencia = {120, 185, 230, 198, 160, 245, 175};
        actualizarBarrasAsistencia(asistencia);
    }

    /**
     * Normaliza los valores al máximo y actualiza la altura de cada barra.
     * La altura máxima visual es 200px.
     */
    private void actualizarBarrasAsistencia(int[] valores) {
        Region[] barras = {barLun, barMar, barMie, barJue, barVie, barSab, barDom};
        double maxValor = 0;
        for (int v : valores) if (v > maxValor) maxValor = v;

        double alturaMax = 200.0;

        for (int i = 0; i < barras.length; i++) {
            double altura = (valores[i] / maxValor) * alturaMax;
            barras[i].setPrefHeight(altura);
            barras[i].setMaxHeight(altura);

            // La barra más alta va en amarillo, las demás en azul
            if (valores[i] == (int) maxValor) {
                barras[i].setStyle("-fx-background-color: " + COLOR_ACTIVO + "; -fx-background-radius: 2;");
            } else {
                barras[i].setStyle("-fx-background-color: " + COLOR_INACTIVO + "; -fx-background-radius: 2;");
            }

            // Animación de entrada (fade + slide desde abajo)
            FadeTransition ft = new FadeTransition(Duration.millis(400), barras[i]);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 60));
            ft.play();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  DEMOGRAFÍA
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarDemografia() {
        // TODO: reemplazar con llamada al DAO
        // Map<String, Integer> demo = clienteDAO.getDemografiaActivos();
        // Query sugerida:
        // SELECT
        //   SUM(CASE WHEN TRUNC(MONTHS_BETWEEN(SYSDATE, u.fecha_nacimiento)/12) < 18 THEN 1 ELSE 0 END) as menores,
        //   SUM(CASE WHEN TRUNC(MONTHS_BETWEEN(SYSDATE, u.fecha_nacimiento)/12) BETWEEN 18 AND 55 THEN 1 ELSE 0 END) as adultos,
        //   SUM(CASE WHEN TRUNC(MONTHS_BETWEEN(SYSDATE, u.fecha_nacimiento)/12) > 55 THEN 1 ELSE 0 END) as seniors
        // FROM REGISTROS_INGRESOS ri
        // JOIN USUARIOS u ON ri.id_cliente = u.numero_identificacion
        // WHERE ri.fecha = TRUNC(SYSDATE) AND ri.hora_salida IS NULL

        int menores = 33;
        int adultos = 120;
        int seniors = 31;
        int total   = menores + adultos + seniors;

        lblPctAdulto.setText(String.format("%.0f%%", (adultos * 100.0) / total));
        lblCntAdulto.setText(adultos + " presentes");

        lblPctMenor.setText(String.format("%.0f%%", (menores * 100.0) / total));
        lblCntMenor.setText(menores + " presentes");

        lblPctSenior.setText(String.format("%.0f%%", (seniors * 100.0) / total));
        lblCntSenior.setText(seniors + " presentes");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HORAS PICO
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarHorasPico() {
        // TODO: reemplazar con llamada al DAO
        // int[] datos = ingresoDAO.contarIngresosPorHora();
        // Query sugerida:
        // SELECT EXTRACT(HOUR FROM hora_entrada) as hora, COUNT(*) as cantidad
        // FROM REGISTROS_INGRESOS
        // WHERE fecha >= TRUNC(SYSDATE) - 30
        // GROUP BY EXTRACT(HOUR FROM hora_entrada)
        // ORDER BY hora

        // Índice 0 = 06:00, índice 1 = 07:00, ... índice 15 = 21:00
        int[] horasPico = {40, 70, 100, 130, 190, 160, 120, 90, 80, 84, 110, 170, 180, 120, 60, 30};
        actualizarBarrasHorasPico(horasPico);
    }

    private void actualizarBarrasHorasPico(int[] valores) {
        Region[] barras = {h06, h07, h08, h09, h10, h11, h12, h13, h14, h15, h16, h17, h18, h19, h20, h21};
        double maxValor = 0;
        for (int v : valores) if (v > maxValor) maxValor = v;

        double alturaMax = 192.0;
        double umbralAlto     = maxValor * 0.75;
        double umbralModerado = maxValor * 0.45;

        for (int i = 0; i < barras.length; i++) {
            double altura = (valores[i] / maxValor) * alturaMax;
            barras[i].setPrefHeight(altura);
            barras[i].setMaxHeight(altura);

            String color;
            if (valores[i] >= umbralAlto)     color = COLOR_ALTO;
            else if (valores[i] >= umbralModerado) color = COLOR_MODERADO;
            else                               color = COLOR_BAJO;

            barras[i].setStyle("-fx-background-color: " + color + "; -fx-background-radius: 3 3 0 0;");

            // Animación de entrada escalonada
            FadeTransition ft = new FadeTransition(Duration.millis(350), barras[i]);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setDelay(Duration.millis(i * 40));
            ft.play();
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMACIÓN PUNTO EN VIVO
    // ═══════════════════════════════════════════════════════════════════════

    private void iniciarAnimacionPuntoVivo() {
        // Pulso de opacidad cada segundo
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,       e -> dotActivos.setOpacity(1.0)),
                new KeyFrame(Duration.millis(600), e -> dotActivos.setOpacity(0.2)),
                new KeyFrame(Duration.millis(1200),e -> dotActivos.setOpacity(1.0))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — BOTONES VIEW (Diario / Semanal)
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleViewDiario() {
        // TODO: cargar datos de ingresos del día de hoy por hora
        // int[] datos = ingresoDAO.contarIngresosPorHoraHoy();
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
    }

    @FXML
    private void handleViewSemanal() {
        // TODO: cargar datos de la semana
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
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — NAV
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleNavDashboard() {
        setNavActivo(navDashboard);
    }

    @FXML
    private void handleNavClientes() {
        setNavActivo(navClientes);
        navegarA("/fxml/GestionClientes.fxml");
    }

    @FXML
    private void handleNavInstructores() {
        setNavActivo(navInstructores);
        // TODO: navegarA("/fxml/Instructores.fxml");
    }

    @FXML
    private void handleNavMembresias() {
        setNavActivo(navMembresias);
        // TODO: navegarA("/fxml/Membresias.fxml");
    }

    @FXML
    private void handleNavAI() {
        setNavActivo(navAI);
        // TODO: navegarA("/fxml/GymbroAI.fxml");
    }

    @FXML
    private void handleNavConfig() {

    }

    @FXML
    private void handleLogout() {
        // TODO: limpiar sesión y volver al Login
        // Stage stage = (Stage) sideNav.getScene().getWindow();
        // Parent root = FXMLLoader.load(getClass().getResource("/fxml/Login.fxml"));
        // stage.setScene(new Scene(root));
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Seguro que deseas cerrar sesion?",
                ButtonType.YES, ButtonType.NO);
        alert.setTitle("Cerrar sesion");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                // navegarA("/fxml/Login.fxml");
                System.out.println("Logout — implementar navegacion al Login");
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — ACCIONES RÁPIDAS
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleVerRegistro() {
        // TODO: abrir modal o navegar a vista de registro detallado
        // Tabla: REGISTROS_INGRESOS JOIN USUARIOS
    }

    @FXML
    private void handleAgregarMiembro() {
        // TODO: abrir modal de registro rápido
        // INSERT INTO USUARIOS + CLIENTES + HISTORIAL_MEMBRESIAS
    }

    @FXML
    private void handleExportarInforme() {
        // TODO: generar PDF con datos del mes
        // Datos de: PAGOS, REGISTROS_INGRESOS, HISTORIAL_MEMBRESIAS
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Placeholder mientras no existen las otras vistas.
     * Eliminar cuando tengas las vistas implementadas.
     */


    /**
     * Navegar a otra vista manteniendo el mismo Stage.
     * Descomentar y usar cuando tengas las vistas listas.
     *
     * @param rutaFxml ruta relativa al resources, ej: "/fxml/Clientes.fxml"
     */

    private void navegarA(String rutaFxml) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource(rutaFxml));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) sideNav.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (java.io.IOException e) {
            e.printStackTrace();
             Alert alert = new Alert(Alert.AlertType.ERROR,
                    "Error al cargar la vista: " + e.getMessage(),
                    ButtonType.OK);
            alert.setTitle("Error de navegación");
        }
    }
}