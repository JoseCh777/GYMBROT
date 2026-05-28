package org.gymbrot.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.HistorialMembresiaDAO;
import org.gymbrot.dao.MembresiaDAO;
import org.gymbrot.dao.RegistroIngresoDAO;
import org.gymbrot.model.Cliente;
import org.gymbrot.model.HistorialMembresia;
import org.gymbrot.model.Membresia;
import org.gymbrot.model.RegistroIngreso;
import org.gymbrot.Main;
import org.gymbrot.service.HuellaService;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class PerfilClienteController implements Initializable {

    // ─── Sidebar ──────────────────────────────────────────────────────────
    @FXML private ImageView imgFotoPerfil;
    @FXML private Label     lblInicialesAvatar;
    @FXML private Label     lblPlanMembresia;
    @FXML private Label     lblMiembroDesde;
    @FXML private Label     lblEstadoCliente;

    // ─── KPIs ─────────────────────────────────────────────────────────────
    @FXML private Label lblIngresosTotales;
    @FXML private Label lblUltimaVisita;
    @FXML private Label lblHoraVisita;
    @FXML private Label lblVencimiento;

    // ─── Credenciales ─────────────────────────────────────────────────────
    @FXML private Label lblNombreCliente;
    @FXML private Label lblTipoDocumento;
    @FXML private Label lblCorreo;
    @FXML private Label lblTelefono;
    @FXML private Label lblCategoriaEdad;
    @FXML private Label lblRangoEdad;

    // ─── Tabla accesos ────────────────────────────────────────────────────
    @FXML private TableView<RegistroIngreso> tablaAccesos;
    @FXML private TableColumn<RegistroIngreso, String>  colFecha;
    @FXML private TableColumn<RegistroIngreso, String>  colHoraEntrada;
    @FXML private TableColumn<RegistroIngreso, String>  colHoraSalida;
    @FXML private TableColumn<RegistroIngreso, String>  colPuntoAcceso;
    @FXML private TableColumn<RegistroIngreso, String>  colDuracion;
    @FXML private TableColumn<RegistroIngreso, String>  colEstadoAcceso;

    // ─── Botones ──────────────────────────────────────────────────────────
    @FXML private Button btnCerrar;
    @FXML private Button btnEditarPerfil;
    @FXML private Button btnVerProgreso;

    // ─── Overlay ──────────────────────────────────────────────────────────
    private StackPane wrapperStack;
    private Parent    overlayRoot;

    // ─── Servicios / DAOs ────────────────────────────────────────────────
    private ClienteDAO         clienteDAO;
    private HistorialMembresiaDAO historialDAO;
    private MembresiaDAO       membresiaDAO;
    private RegistroIngresoDAO ingresoDAO;

    private Cliente cliente;

    private static final DateTimeFormatter FMT_FECHA     = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FMT_HORA      = DateTimeFormatter.ofPattern("hh:mm a");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        clienteDAO   = new ClienteDAO();
        historialDAO = new HistorialMembresiaDAO();
        membresiaDAO = new MembresiaDAO();
        ingresoDAO   = new RegistroIngresoDAO();

        configurarTabla();
    }

    public void setWrapperStack(StackPane wrapper, Parent overlay) {
        this.wrapperStack = wrapper;
        this.overlayRoot  = overlay;
    }

    public void setCliente(Cliente cliente) {
        this.cliente = cliente;
        if (cliente != null) {
            Platform.runLater(this::cargarDatos);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CARGA DE DATOS
    // ═══════════════════════════════════════════════════════════════════════

    private void cargarDatos() {
        // ── Header ──
        lblNombreCliente.setText(cliente.getNombre() + " " + cliente.getApellidos());

        // ── Avatar ──
        String fotoUrl = cliente.getFotoUrl();
        if (fotoUrl != null && !fotoUrl.isBlank()) {
            try {
                Image img = new Image(new File(fotoUrl).toURI().toString(), false);
                if (!img.isError()) {
                    imgFotoPerfil.setImage(img);
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

        // ── Sidebar ──
        if (cliente.getFechaRegistro() != null)
            lblMiembroDesde.setText(cliente.getFechaRegistro().format(FMT_FECHA));
        lblEstadoCliente.setText(cliente.getEstado());
        if ("ACTIVO".equalsIgnoreCase(cliente.getEstado()))
            lblEstadoCliente.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #D4FF00;");
        else
            lblEstadoCliente.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #ef4444;");

        // ── Membresía ──
        cargarMembresia();

        // ── Credenciales ──
        String tipo = cliente.getTipoIdentificacion();
        String num  = cliente.getNumeroIdentificacion();
        lblTipoDocumento.setText((tipo != null ? tipo : "---") + " / " + (num != null ? num : "---"));
        lblCorreo.setText(cliente.getCorreo() != null ? cliente.getCorreo() : "---");
        lblTelefono.setText(cliente.getTelefono() != null ? cliente.getTelefono() : "---");

        // ── Edad / Categoría ──
        if (cliente.getFechaNacimiento() != null) {
            int edad = Period.between(cliente.getFechaNacimiento(), LocalDate.now()).getYears();
            lblCategoriaEdad.setText(edad + " años");
            lblRangoEdad.setText(categoriaEdad(edad));
        } else {
            lblCategoriaEdad.setText("---");
            lblRangoEdad.setText("");
        }

        // ── Ingresos ──
        cargarIngresos();
    }

    private void cargarMembresia() {
        HistorialMembresia h = historialDAO.buscarActiva(cliente.getNumeroIdentificacion());
        if (h != null) {
            Membresia m = membresiaDAO.buscarPorId(h.getIdMembresia());
            if (m != null) {
                lblPlanMembresia.setText(m.getTipoMembresia());
                if (m.getFechaVencimiento() != null)
                    lblVencimiento.setText(m.getFechaVencimiento().format(FMT_FECHA));
            }
        } else {
            lblPlanMembresia.setText("Sin membresía activa");
            lblVencimiento.setText("---");
        }
    }

    private void cargarIngresos() {
        List<RegistroIngreso> ingresos = ingresoDAO.listarPorCliente(cliente.getNumeroIdentificacion());
        if (ingresos == null || ingresos.isEmpty()) {
            lblIngresosTotales.setText("0");
            lblUltimaVisita.setText("---");
            lblHoraVisita.setText("");
            return;
        }

        lblIngresosTotales.setText(String.valueOf(ingresos.size()));

        RegistroIngreso ultimo = ingresos.get(0);
        if (ultimo.getFecha() != null)
            lblUltimaVisita.setText(ultimo.getFecha().format(FMT_FECHA));
        if (ultimo.getHoraEntrada() != null)
            lblHoraVisita.setText(ultimo.getHoraEntrada().format(FMT_HORA));

        ObservableList<RegistroIngreso> items = FXCollections.observableArrayList(ingresos);
        tablaAccesos.setItems(items);
    }

    private void mostrarIniciales() {
        String nombre  = cliente.getNombre();
        String apellido = cliente.getApellidos();
        String iniciales = "";
        if (nombre != null && !nombre.isBlank()) iniciales += nombre.charAt(0);
        if (apellido != null && !apellido.isBlank()) iniciales += apellido.charAt(0);
        lblInicialesAvatar.setText(iniciales.isBlank() ? "?" : iniciales.toUpperCase());
        lblInicialesAvatar.setVisible(true);
    }

    private String categoriaEdad(int edad) {
        if (edad < 12)           return "Niño";
        if (edad < 18)           return "Adolescente";
        if (edad < 30)           return "Joven";
        if (edad < 50)           return "Adulto";
        if (edad < 65)           return "Adulto Mayor";
        return "Senior";
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  CONFIGURACIÓN TABLA
    // ═══════════════════════════════════════════════════════════════════════

    private void configurarTabla() {
        colFecha.setCellValueFactory(celda -> {
            LocalDate f = celda.getValue().getFecha();
            return new javafx.beans.property.SimpleStringProperty(
                    f != null ? f.format(FMT_FECHA) : "---");
        });
        colHoraEntrada.setCellValueFactory(celda -> {
            var h = celda.getValue().getHoraEntrada();
            return new javafx.beans.property.SimpleStringProperty(
                    h != null ? h.format(FMT_HORA) : "---");
        });
        colHoraSalida.setCellValueFactory(celda -> {
            var h = celda.getValue().getHoraSalida();
            return new javafx.beans.property.SimpleStringProperty(
                    h != null ? h.format(FMT_HORA) : "—");
        });
        colPuntoAcceso.setCellValueFactory(celda -> {
            var metodo = celda.getValue().getMetodoVerificacion();
            return new javafx.beans.property.SimpleStringProperty(
                    metodo != null ? metodo : "Entrada Principal");
        });
        colDuracion.setCellValueFactory(celda -> {
            var entrada = celda.getValue().getHoraEntrada();
            var salida  = celda.getValue().getHoraSalida();
            if (entrada != null && salida != null) {
                long min = java.time.Duration.between(entrada, salida).toMinutes();
                return new javafx.beans.property.SimpleStringProperty(
                        String.format("%dh %02dm", min / 60, min % 60));
            }
            return new javafx.beans.property.SimpleStringProperty("En curso");
        });
        colEstadoAcceso.setCellValueFactory(celda ->
                new javafx.beans.property.SimpleStringProperty(
                        celda.getValue().getEstadoVerificacion() != null
                                ? celda.getValue().getEstadoVerificacion() : "---"));
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS
    // ═══════════════════════════════════════════════════════════════════════

    @FXML
    private void handleCerrar() {
        if (wrapperStack != null && overlayRoot != null) {
            wrapperStack.getChildren().remove(overlayRoot);
        }
    }

    @FXML
    private void handleEditarPerfil() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NuevoCliente.fxml"));
            Parent overlay = loader.load();
            NuevoClienteController ctrl = loader.getController();
            ctrl.setCliente(cliente);

            StackPane wrapper = new StackPane();
            Stage stage = (Stage) btnCerrar.getScene().getWindow();
            Parent rootActual = stage.getScene().getRoot();
            if (rootActual instanceof StackPane sp && sp.getChildren().size() > 1) {
                wrapper.getChildren().add(sp.getChildren().get(0));
            } else {
                wrapper.getChildren().add(rootActual);
            }
            wrapper.getChildren().add(overlay);
            stage.getScene().setRoot(wrapper);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleVerProgreso() {
        ProgresoFisicoController.pendingClienteId = cliente.getNumeroIdentificacion();
        if (wrapperStack != null && overlayRoot != null) {
            wrapperStack.getChildren().remove(overlayRoot);
        }
        Main.navegarA("/fxml/ProgresoFisico.fxml");
    }

    // ═══════════════════════════════════════════════════════════════════════
    //  HANDLERS - TABLA
    // ═══════════════════════════════════════════════════════════════════════
    //  PUENTE CON VERIFICACIÓN — abrir desde biometrico o manual
    // ═══════════════════════════════════════════════════════════════════════

    public static void abrirConCliente(Cliente cliente, StackPane wrapper, Parent overlayActual) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    PerfilClienteController.class.getResource("/fxml/PerfilCliente.fxml"));
            Parent overlay = loader.load();
            PerfilClienteController ctrl = loader.getController();
            ctrl.setWrapperStack(wrapper, overlay);
            ctrl.setCliente(cliente);

            wrapper.getChildren().remove(overlayActual);
            wrapper.getChildren().add(overlay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirOverlay(String rutaFxml) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFxml));
            Parent overlay = loader.load();
            StackPane wrapper = new StackPane();
            Stage stage = (Stage) btnCerrar.getScene().getWindow();

            Parent rootActual = stage.getScene().getRoot();
            if (rootActual instanceof StackPane sp && sp.getChildren().size() > 1) {
                wrapper.getChildren().add(sp.getChildren().get(0));
            } else {
                wrapper.getChildren().add(rootActual);
            }
            wrapper.getChildren().add(overlay);
            stage.getScene().setRoot(wrapper);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
