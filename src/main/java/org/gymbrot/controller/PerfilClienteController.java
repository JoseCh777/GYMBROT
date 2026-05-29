package org.gymbrot.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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

import java.io.File;
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

    private record DatosMembresia(String plan, String vencimiento) {}
    private record DatosIngresos(int total, String ultimaFecha, String ultimaHora, ObservableList<RegistroIngreso> items) {}

    private void cargarDatos() {
        // ── UI inmediato (sin JDBC) ──
        lblNombreCliente.setText(cliente.getNombre() + " " + cliente.getApellidos());

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

        if (cliente.getFechaRegistro() != null)
            lblMiembroDesde.setText(cliente.getFechaRegistro().format(FMT_FECHA));
        lblEstadoCliente.setText(cliente.getEstado());
        if ("ACTIVO".equalsIgnoreCase(cliente.getEstado()))
            lblEstadoCliente.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #D4FF00;");
        else
            lblEstadoCliente.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #ef4444;");

        String tipo = cliente.getTipoIdentificacion();
        String num  = cliente.getNumeroIdentificacion();
        lblTipoDocumento.setText((tipo != null ? tipo : "---") + " / " + (num != null ? num : "---"));
        lblCorreo.setText(cliente.getCorreo() != null ? cliente.getCorreo() : "---");
        lblTelefono.setText(cliente.getTelefono() != null ? cliente.getTelefono() : "---");

        if (cliente.getFechaNacimiento() != null) {
            int edad = Period.between(cliente.getFechaNacimiento(), LocalDate.now()).getYears();
            lblCategoriaEdad.setText(edad + " años");
            lblRangoEdad.setText(categoriaEdad(edad));
        } else {
            lblCategoriaEdad.setText("---");
            lblRangoEdad.setText("");
        }

        // ── JDBC en background ──
        String id = cliente.getNumeroIdentificacion();
        Task<Void> task = new Task<>() {
            private DatosMembresia datosMem;
            private DatosIngresos datosIng;

            @Override
            protected Void call() {
                datosMem = cargarMembresia(id);
                datosIng = cargarIngresos(id);
                return null;
            }

            @Override
            protected void succeeded() {
                if (datosMem != null) {
                    lblPlanMembresia.setText(datosMem.plan());
                    lblVencimiento.setText(datosMem.vencimiento());
                }
                if (datosIng != null) {
                    lblIngresosTotales.setText(String.valueOf(datosIng.total()));
                    lblUltimaVisita.setText(datosIng.ultimaFecha());
                    lblHoraVisita.setText(datosIng.ultimaHora());
                    tablaAccesos.setItems(datosIng.items());
                }
            }
        };
        new Thread(task).start();
    }

    private DatosMembresia cargarMembresia(String id) {
        HistorialMembresia h = historialDAO.buscarActiva(id);
        if (h != null) {
            Membresia m = membresiaDAO.buscarPorId(h.getIdMembresia());
            if (m != null) {
                return new DatosMembresia(
                        m.getTipoMembresia(),
                        m.getFechaVencimiento() != null ? m.getFechaVencimiento().format(FMT_FECHA) : "---"
                );
            }
        }
        return new DatosMembresia("Sin membresía activa", "---");
    }

    private DatosIngresos cargarIngresos(String id) {
        List<RegistroIngreso> ingresos = ingresoDAO.listarPorCliente(id);
        if (ingresos == null || ingresos.isEmpty()) {
            return new DatosIngresos(0, "---", "", FXCollections.observableArrayList());
        }
        RegistroIngreso ultimo = ingresos.get(0);
        return new DatosIngresos(
                ingresos.size(),
                ultimo.getFecha() != null ? ultimo.getFecha().format(FMT_FECHA) : "---",
                ultimo.getHoraEntrada() != null ? ultimo.getHoraEntrada().format(FMT_HORA) : "",
                FXCollections.observableArrayList(ingresos)
        );
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

            if (wrapperStack != null && overlayRoot != null) {
                wrapperStack.getChildren().remove(overlayRoot);
                wrapperStack.getChildren().add(overlay);
                ctrl.setWrapperStack(wrapperStack, overlay);
            } else {
                StackPane wrapper = new StackPane();
                Stage stage = (Stage) btnCerrar.getScene().getWindow();
                Parent rootActual = stage.getScene().getRoot();
                wrapper.getChildren().add(rootActual);
                wrapper.getChildren().add(overlay);
                stage.getScene().setRoot(wrapper);
                ctrl.setWrapperStack(wrapper, overlay);
            }
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
