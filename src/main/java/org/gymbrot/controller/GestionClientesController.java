package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.animation.FadeTransition;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * GestionClientesController
 *
 * Tablas Oracle que alimentan esta vista:
 *  - USUARIOS + CLIENTES          → tabla principal
 *  - REGISTROS_INGRESOS           → sesiones activas (sin hora_salida hoy)
 *  - HISTORIAL_MEMBRESIAS         → estado membresia
 *  - PAGOS                        → flujo diario
 *
 * TODO: cuando tengas el DAO, reemplaza los datos mock por llamadas reales.
 */
public class GestionClientesController implements Initializable {

    // ─── SideNav ───────────────────────────────────────────────────────────
    @FXML private VBox sideNav;
    @FXML private Button navDashboard;
    @FXML private Button navClientes;
    @FXML private Button navInstructores;
    @FXML private Button navMembresias;
    @FXML private Button navAI;

    // ─── TopBar ────────────────────────────────────────────────────────────
    @FXML private HBox topBar;
    @FXML private ImageView avatarImage;

    // ─── Stats ─────────────────────────────────────────────────────────────
    @FXML private Label lblTotalClientes;
    @FXML private Label lblTendenciaClientes;
    @FXML private Label lblSesionesActivas;
    @FXML private Label lblTasaIngreso;
    @FXML private Label lblTasaTendencia;
    @FXML private Label lblFlujoDiario;

    // ─── Toolbar ───────────────────────────────────────────────────────────
    @FXML private TextField searchField;
    @FXML private Button btnValidarHuella;
    @FXML private Button btnAgregarCliente;

    // ─── Tabla ─────────────────────────────────────────────────────────────
    @FXML private TableView<ClienteRow> tablaClientes;
    @FXML private TableColumn<ClienteRow, String> colIdentidad;
    @FXML private TableColumn<ClienteRow, String> colContacto;
    @FXML private TableColumn<ClienteRow, String> colCategoria;
    @FXML private TableColumn<ClienteRow, String> colEstado;
    @FXML private TableColumn<ClienteRow, String> colAcciones;

    // ─── Paginacion ────────────────────────────────────────────────────────
    @FXML private Label lblRegistros;
    @FXML private Button btnAnterior;
    @FXML private Button btnPag1;
    @FXML private Button btnPag2;
    @FXML private Button btnPag3;
    @FXML private Button btnSiguiente;

    // ─── Monitor Scanner + AI ──────────────────────────────────────────────
    @FXML private Rectangle dotScanner;
    @FXML private VBox logAccesos;
    @FXML private Label lblAIInsight;
    @FXML private Button btnOptimizarFlujo;

    // ─── Estado interno ────────────────────────────────────────────────────
    private ObservableList<ClienteRow> todosLosClientes;
    private FilteredList<ClienteRow> clientesFiltrados;
    private int paginaActual = 1;
    private static final int REGISTROS_POR_PAGINA = 10;

    // ═══════════════════════════════════════════════════════════════════════
    //  MODELO DE FILA
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Modelo simple de fila para la tabla.
     * TODO: reemplazar con tu clase Modelo/DTO cuando tengas el DAO.
     * Los campos vienen de: SELECT u.nombre, u.apellidos, u.correo, u.telefono,
     *   TRUNC(MONTHS_BETWEEN(SYSDATE,u.fecha_nacimiento)/12) edad,
     *   hm.activa FROM USUARIOS u JOIN CLIENTES c ON... JOIN HISTORIAL_MEMBRESIAS hm ON...
     */
    public static class ClienteRow {
        private final SimpleStringProperty id;
        private final SimpleStringProperty nombre;
        private final SimpleStringProperty correo;
        private final SimpleStringProperty telefono;
        private final SimpleStringProperty categoria;
        private final SimpleStringProperty estado;

        public ClienteRow(String id, String nombre, String correo,
                          String telefono, String categoria, String estado) {
            this.id        = new SimpleStringProperty(id);
            this.nombre    = new SimpleStringProperty(nombre);
            this.correo    = new SimpleStringProperty(correo);
            this.telefono  = new SimpleStringProperty(telefono);
            this.categoria = new SimpleStringProperty(categoria);
            this.estado    = new SimpleStringProperty(estado);
        }

        public String getId()        { return id.get(); }
        public String getNombre()    { return nombre.get(); }
        public String getCorreo()    { return correo.get(); }
        public String getTelefono()  { return telefono.get(); }
        public String getCategoria() { return categoria.get(); }
        public String getEstado()    { return estado.get(); }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  INITIALIZE
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarAnimacionesNav();
        setNavActivo(navClientes);
        cargarStats();
        configurarTabla();
        cargarDatosMock();
        configurarBuscador();
        cargarLogAccesos();
        cargarAIInsight();
        iniciarAnimacionScanner();
        configurarAnimacionesBotones();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  STATS
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarStats() {
        // TODO: reemplazar con DAO
        // int total = clienteDAO.contarClientesActivos();
        // Query: SELECT COUNT(*) FROM USUARIOS WHERE tipo_usuario='CLIENTE' AND estado='ACTIVO'
        lblTotalClientes.setText("1,250");
        lblTendenciaClientes.setText("12% vs mes anterior");

        // Query: SELECT COUNT(*) FROM REGISTROS_INGRESOS
        //        WHERE fecha = TRUNC(SYSDATE) AND hora_salida IS NULL
        lblSesionesActivas.setText("42");

        // Query: (ingresos hoy / capacidad maxima) * 100
        lblTasaIngreso.setText("88%");
        lblTasaTendencia.setText("2% hoy");

        // Query: SELECT COUNT(*) FROM REGISTROS_INGRESOS WHERE fecha = TRUNC(SYSDATE)
        lblFlujoDiario.setText("156");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  TABLA
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarTabla() {
        // Estilo de la tabla
        tablaClientes.setStyle(
                "-fx-background-color: #1a1d21;" +
                        "-fx-border-width: 0;" +
                        "-fx-table-cell-border-color: #1f2125;"
        );
        tablaClientes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Forzar color oscuro en header de columnas (JavaFX pone blanco por defecto con inline CSS)
        tablaClientes.skinProperty().addListener((obs, oldSkin, newSkin) -> {
            if (newSkin != null) {
                tablaClientes.lookupAll(".column-header, .column-header-background, .filler").forEach(node ->
                        node.setStyle("-fx-background-color: #282a2d; -fx-border-color: #1f2125; -fx-border-width: 0 0 1 0;")
                );
            }
        });

        // ── Columna Identidad: nombre + ID ──
        colIdentidad.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getNombre()));
        colIdentidad.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String nombre, boolean empty) {
                super.updateItem(nombre, empty);
                if (empty || nombre == null) {
                    setGraphic(null);
                    return;
                }
                ClienteRow row = getTableView().getItems().get(getIndex());

                // Avatar con iniciales
                StackPane avatar = new StackPane();
                avatar.setPrefSize(40, 40);
                avatar.setMinSize(40, 40);
                avatar.setStyle("-fx-background-color: #282a2d; -fx-background-radius: 8;" +
                        "-fx-border-color: #333538; -fx-border-width: 1; -fx-border-radius: 8;");
                Label iniciales = new Label(getIniciales(nombre));
                iniciales.setStyle("-fx-font-family: 'Lexend'; -fx-font-size: 13px;" +
                        "-fx-font-weight: 700; -fx-text-fill: #D4FF00;");
                avatar.getChildren().add(iniciales);

                VBox info = new VBox(2);
                Label lblNombre = new Label(nombre);
                lblNombre.setStyle("-fx-font-family: 'Lexend'; -fx-font-size: 14px;" +
                        "-fx-font-weight: 600; -fx-text-fill: white;");
                Label lblId = new Label("ID: " + row.getId());
                lblId.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                        "-fx-text-fill: #6b7280;");
                info.getChildren().addAll(lblNombre, lblId);

                HBox cell = new HBox(12, avatar, info);
                cell.setAlignment(Pos.CENTER_LEFT);
                setGraphic(cell);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 8 12 8 12;");
            }
        });

        // ── Columna Contacto ──
        colContacto.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCorreo()));
        colContacto.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String correo, boolean empty) {
                super.updateItem(correo, empty);
                if (empty || correo == null) { setGraphic(null); return; }
                ClienteRow row = getTableView().getItems().get(getIndex());

                VBox info = new VBox(3);
                Label lblCorreo = new Label(correo);
                lblCorreo.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-text-fill: #e2e2e6;");
                Label lblTel = new Label(row.getTelefono());
                lblTel.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #6b7280;");
                info.getChildren().addAll(lblCorreo, lblTel);

                setGraphic(info);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 8 12 8 12;");
            }
        });

        // ── Columna Categoria ──
        colCategoria.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getCategoria()));
        colCategoria.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String cat, boolean empty) {
                super.updateItem(cat, empty);
                if (empty || cat == null) { setGraphic(null); return; }

                Label badge = new Label(cat);
                badge.setStyle("-fx-background-color: #282a2d; -fx-background-radius: 20;" +
                        "-fx-border-color: #333538; -fx-border-width: 1; -fx-border-radius: 20;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                        "-fx-font-weight: 700; -fx-text-fill: #9cf0ff;" +
                        "-fx-padding: 4 12 4 12;");

                HBox wrapper = new HBox(badge);
                wrapper.setAlignment(Pos.CENTER);
                setGraphic(wrapper);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 8 12 8 12;");
            }
        });

        // ── Columna Estado ──
        colEstado.setCellValueFactory(data ->
                new SimpleStringProperty(data.getValue().getEstado()));
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String estado, boolean empty) {
                super.updateItem(estado, empty);
                if (empty || estado == null) { setGraphic(null); return; }

                boolean activo = estado.equalsIgnoreCase("ACTIVO");
                Label lbl = new Label(activo ? "Entrada Segura" : "Membresia Vencida");
                lbl.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                        "-fx-font-weight: 700; -fx-text-fill: " +
                        (activo ? "#D4FF00" : "#ffb4ab") + ";");

                HBox wrapper = new HBox(lbl);
                wrapper.setAlignment(Pos.CENTER);
                setGraphic(wrapper);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 8 12 8 12;");
            }
        });

        // ── Columna Acciones ──
        colAcciones.setCellValueFactory(data -> new SimpleStringProperty(""));
        colAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnEditar  = new Button("Ed.");
            private final Button btnEliminar = new Button("El.");

            {
                btnEditar.setStyle(
                        "-fx-background-color: transparent; -fx-background-radius: 6;" +
                                "-fx-border-color: #333538; -fx-border-width: 1; -fx-border-radius: 6;" +
                                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                                "-fx-text-fill: #6b7280; -fx-cursor: hand;" +
                                "-fx-pref-width: 32; -fx-pref-height: 32;"
                );
                btnEliminar.setStyle(
                        "-fx-background-color: transparent; -fx-background-radius: 6;" +
                                "-fx-border-color: #333538; -fx-border-width: 1; -fx-border-radius: 6;" +
                                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                                "-fx-text-fill: #6b7280; -fx-cursor: hand;" +
                                "-fx-pref-width: 32; -fx-pref-height: 32;"
                );

                // Hover editar → amarillo
                btnEditar.setOnMouseEntered(e -> btnEditar.setStyle(
                        btnEditar.getStyle().replace("-fx-text-fill: #6b7280", "-fx-text-fill: #D4FF00")
                                .replace("-fx-border-color: #333538", "-fx-border-color: #D4FF00")));
                btnEditar.setOnMouseExited(e -> btnEditar.setStyle(
                        btnEditar.getStyle().replace("-fx-text-fill: #D4FF00", "-fx-text-fill: #6b7280")
                                .replace("-fx-border-color: #D4FF00", "-fx-border-color: #333538")));

                // Hover eliminar → rojo
                btnEliminar.setOnMouseEntered(e -> btnEliminar.setStyle(
                        btnEliminar.getStyle().replace("-fx-text-fill: #6b7280", "-fx-text-fill: #ffb4ab")
                                .replace("-fx-border-color: #333538", "-fx-border-color: #ffb4ab")));
                btnEliminar.setOnMouseExited(e -> btnEliminar.setStyle(
                        btnEliminar.getStyle().replace("-fx-text-fill: #ffb4ab", "-fx-text-fill: #6b7280")
                                .replace("-fx-border-color: #ffb4ab", "-fx-border-color: #333538")));

                btnEditar.setOnAction(e -> {
                    ClienteRow row = getTableView().getItems().get(getIndex());
                    handleEditarCliente(row);
                });
                btnEliminar.setOnAction(e -> {
                    ClienteRow row = getTableView().getItems().get(getIndex());
                    handleEliminarCliente(row);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, btnEditar, btnEliminar);
                box.setAlignment(Pos.CENTER_RIGHT);
                setGraphic(box);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 8 12 8 12;");
            }
        });

        // Estilo de filas alternas
        tablaClientes.setRowFactory(tv -> {
            TableRow<ClienteRow> row = new TableRow<>();
            row.setStyle("-fx-background-color: transparent;");
            row.setOnMouseEntered(e -> {
                if (!row.isEmpty())
                    row.setStyle("-fx-background-color: #1f2226;");
            });
            row.setOnMouseExited(e -> row.setStyle("-fx-background-color: transparent;"));
            return row;
        });
    }

    private void cargarDatosMock() {
        // TODO: reemplazar con llamada al DAO
        // List<ClienteRow> lista = clienteDAO.listarClientes(paginaActual, REGISTROS_POR_PAGINA);
        // Query sugerida:
        // SELECT u.numero_identificacion, u.nombre||' '||u.apellidos, u.correo, u.telefono,
        //   CASE WHEN TRUNC(MONTHS_BETWEEN(SYSDATE,u.fecha_nacimiento)/12) < 18 THEN 'Juvenil'
        //        WHEN TRUNC(MONTHS_BETWEEN(SYSDATE,u.fecha_nacimiento)/12) <= 55 THEN 'Adulto Elite'
        //        ELSE 'Senior Pro' END categoria,
        //   CASE WHEN hm.activa = 1 THEN 'ACTIVO' ELSE 'VENCIDO' END estado
        // FROM USUARIOS u JOIN CLIENTES c ON u.numero_identificacion = c.numero_identificacion
        // LEFT JOIN HISTORIAL_MEMBRESIAS hm ON hm.id_cliente = u.numero_identificacion AND hm.activa = 1
        // WHERE u.tipo_usuario = 'CLIENTE'
        // ORDER BY u.nombre OFFSET ? ROWS FETCH NEXT ? ROWS ONLY

        todosLosClientes = FXCollections.observableArrayList(
                new ClienteRow("KP-8842", "Elena Rodriguez",  "e.rodriguez@email.com", "612 345 678", "Adulto Elite", "ACTIVO"),
                new ClienteRow("KP-2195", "Marco Valles",     "m.valles@fitness.com",  "655 890 123", "Senior Pro",   "ACTIVO"),
                new ClienteRow("KP-4432", "Lucia Mendez",     "l.mendez@sport.es",     "677 211 004", "Juvenil",      "VENCIDO"),
                new ClienteRow("KP-1023", "Carlos Herrera",   "c.herrera@gym.co",      "310 222 3344","Adulto Elite", "ACTIVO"),
                new ClienteRow("KP-5541", "Andrea Gomez",     "a.gomez@email.com",     "315 888 9900","Juvenil",      "ACTIVO"),
                new ClienteRow("KP-3310", "Roberto Suarez",   "r.suarez@mail.co",      "300 111 2200","Senior Pro",   "VENCIDO"),
                new ClienteRow("KP-7790", "Diana Torres",     "d.torres@fit.com",      "321 444 5566","Adulto Elite", "ACTIVO"),
                new ClienteRow("KP-6612", "Felipe Cardona",   "f.cardona@sport.co",    "318 777 8800","Adulto Elite", "ACTIVO"),
                new ClienteRow("KP-9901", "Valentina Rios",   "v.rios@gym.co",         "312 333 4400","Juvenil",      "ACTIVO"),
                new ClienteRow("KP-0045", "Sergio Morales",   "s.morales@email.com",   "317 555 6600","Senior Pro",   "ACTIVO")
        );

        clientesFiltrados = new FilteredList<>(todosLosClientes, p -> true);
        tablaClientes.setItems(clientesFiltrados);
        actualizarLabelRegistros();
    }

    private void configurarBuscador() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            String filtro = newVal.toLowerCase().trim();
            clientesFiltrados.setPredicate(cliente -> {
                if (filtro.isEmpty()) return true;
                return cliente.getNombre().toLowerCase().contains(filtro)
                        || cliente.getId().toLowerCase().contains(filtro)
                        || cliente.getCorreo().toLowerCase().contains(filtro);
            });
            actualizarLabelRegistros();
        });
    }

    private void actualizarLabelRegistros() {
        int total     = todosLosClientes.size();
        int mostrados = clientesFiltrados.size();
        lblRegistros.setText("Mostrando " + mostrados + " de " + total + " registros");
    }

    private String getIniciales(String nombre) {
        String[] partes = nombre.trim().split(" ");
        if (partes.length >= 2)
            return String.valueOf(partes[0].charAt(0)) + partes[1].charAt(0);
        return nombre.isEmpty() ? "?" : String.valueOf(nombre.charAt(0));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  LOG DE ACCESOS (Monitor Scanner)
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarLogAccesos() {
        // TODO: reemplazar con datos reales de REGISTROS_INGRESOS
        // Query: SELECT u.nombre||' '||u.apellidos, ri.hora_entrada, ri.metodo_verificacion
        //        FROM REGISTROS_INGRESOS ri JOIN USUARIOS u ON ri.id_cliente = u.numero_identificacion
        //        WHERE ri.fecha = TRUNC(SYSDATE) ORDER BY ri.hora_entrada DESC FETCH FIRST 5 ROWS ONLY
        agregarEntradaLog("Roberto Gomez", "Terminal Principal", "Hace 2 minutos", true);
        agregarEntradaLog("Sara Wilson",   "Terminal Norte",     "Hace 5 minutos", true);
    }

    private void agregarEntradaLog(String nombre, String terminal, String tiempo, boolean exitoso) {
        HBox fila = new HBox(16);
        fila.setAlignment(Pos.CENTER_LEFT);
        fila.setStyle("-fx-background-color: #1e2023; -fx-background-radius: 8;" +
                "-fx-border-color: #1f2125; -fx-border-width: 1; -fx-border-radius: 8;" +
                "-fx-padding: 14 16 14 16;");

        // Icono
        StackPane icono = new StackPane();
        icono.setPrefSize(48, 48);
        icono.setMinSize(48, 48);
        icono.setStyle("-fx-background-color: " + (exitoso ? "#0a2a30" : "#2a0a0a") +
                "; -fx-background-radius: 24;");
        Label ico = new Label(exitoso ? "H" : "X");
        ico.setStyle("-fx-font-family: 'Lexend'; -fx-font-size: 16px; -fx-font-weight: 700;" +
                "-fx-text-fill: " + (exitoso ? "#bdf4ff" : "#ffb4ab") + ";");
        icono.getChildren().add(ico);

        // Info
        VBox info = new VBox(3);
        HBox.setHgrow(info, Priority.ALWAYS);
        Label lblNombre = new Label("Acceso concedido: " + nombre);
        lblNombre.setStyle("-fx-font-family: 'Lexend'; -fx-font-size: 14px;" +
                "-fx-font-weight: 600; -fx-text-fill: white;");
        Label lblTerminal = new Label(terminal + " • " + tiempo);
        lblTerminal.setStyle("-fx-font-family: 'Inter'; -fx-font-size: 12px; -fx-text-fill: #6b7280;");
        info.getChildren().addAll(lblNombre, lblTerminal);

        // Estado
        Label lblEstado = new Label(exitoso ? "EXITOSO" : "DENEGADO");
        lblEstado.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700;" +
                "-fx-text-fill: " + (exitoso ? "#D4FF00" : "#ffb4ab") + ";");

        fila.getChildren().addAll(icono, info, lblEstado);

        // Animacion de entrada
        fila.setOpacity(0);
        logAccesos.getChildren().add(fila);
        FadeTransition ft = new FadeTransition(Duration.millis(400), fila);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  AI INSIGHT
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarAIInsight() {
        // TODO: conectar con llamada real a Gymbrot AI / SESIONES_GYMBROT
        lblAIInsight.setText(
                "He notado un incremento del 15% en el flujo de clientes entre las 18:00 y 20:00. " +
                        "Sugiero habilitar un segundo terminal de escaneo para evitar cuellos de botella."
        );
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMACION PUNTO SCANNER
    // ═══════════════════════════════════════════════════════════════════════

    private void iniciarAnimacionScanner() {
        Timeline pulse = new Timeline(
                new KeyFrame(Duration.ZERO,        e -> dotScanner.setOpacity(1.0)),
                new KeyFrame(Duration.millis(600),  e -> dotScanner.setOpacity(0.2)),
                new KeyFrame(Duration.millis(1200), e -> dotScanner.setOpacity(1.0))
        );
        pulse.setCycleCount(Timeline.INDEFINITE);
        pulse.play();
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  ANIMACIONES
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarAnimacionesNav() {
        Button[] inactivos = {navDashboard, navInstructores, navMembresias, navAI};
        for (Button btn : inactivos) agregarHoverInactivo(btn);
        agregarHoverActivo(navClientes);
    }

    private void configurarAnimacionesBotones() {
        agregarHoverActivo(btnAgregarCliente);
        agregarHoverInactivo(btnValidarHuella);
        agregarHoverInactivo(btnOptimizarFlujo);
    }

    private void agregarHoverInactivo(Button btn) {
        ScaleTransition grow   = new ScaleTransition(Duration.millis(180), btn);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(180), btn);
        grow.setToX(1.03); grow.setToY(1.03);
        shrink.setToX(1.0); shrink.setToY(1.0);

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
    //  HANDLERS — ACCIONES DE TABLA
    // ═══════════════════════════════════════════════════════════════════════

    private void handleEditarCliente(ClienteRow row) {
        // TODO: navegar a vista de edicion pasando el ID del cliente
        // navegarA("/fxml/EditarCliente.fxml");
        mostrarInfo("Editar", "Editando cliente: " + row.getNombre());
    }

    private void handleEliminarCliente(ClienteRow row) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Eliminar a " + row.getNombre() + "?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirmar eliminacion");
        confirm.setHeaderText(null);
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) {
                // TODO: clienteDAO.eliminar(row.getId());
                todosLosClientes.remove(row);
                actualizarLabelRegistros();
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — TOOLBAR
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleAgregarCliente() {
        navegarA("/fxml/NuevoCliente.fxml");
    }

    @FXML
    private void handleValidarHuella() {
        // TODO: integrar con lector de huella
        mostrarInfo("Validacion Biometrica", "Conecta el lector de huella para continuar.");
    }

    @FXML
    private void handleOptimizarFlujo() {
        mostrarInfo("Gymbrot AI", "Analizando patrones de flujo para optimizacion...");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — PAGINACION
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handlePaginaAnterior() { if (paginaActual > 1) { paginaActual--; cargarDatosMock(); } }
    @FXML private void handlePagina1() { paginaActual = 1; cargarDatosMock(); }
    @FXML private void handlePagina2() { paginaActual = 2; cargarDatosMock(); }
    @FXML private void handlePagina3() { paginaActual = 3; cargarDatosMock(); }
    @FXML private void handlePaginaSiguiente() { paginaActual++; cargarDatosMock(); }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS — NAV
    // ═══════════════════════════════════════════════════════════════════════

    @FXML private void handleNavDashboard()    { navegarA("/fxml/Dashboard.fxml"); }
    @FXML private void handleNavClientes()     { /* ya estamos aqui */ }
    @FXML private void handleNavInstructores() { mostrarProximamente("Instructores"); }
    @FXML private void handleNavMembresias()   { mostrarProximamente("Membresias"); }
    @FXML private void handleNavAI()           { mostrarProximamente("Gymbrot AI"); }
    @FXML private void handleNavConfig()       { mostrarProximamente("Configuracion"); }

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
    //  UTILIDADES
    // ═══════════════════════════════════════════════════════════════════════

    private void navegarA(String rutaFxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(rutaFxml));
            Stage stage = (Stage) sideNav.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarInfo("Error de navegacion", "No se pudo cargar: " + rutaFxml);
        }
    }

    private void mostrarProximamente(String seccion) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Proximamente");
        alert.setHeaderText(seccion);
        alert.setContentText("Esta seccion estara disponible pronto.");
        alert.showAndWait();
    }

    private void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}