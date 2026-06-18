package org.gymbrot.controller;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.HistorialMembresiaDAO;
import org.gymbrot.dao.MembresiaDAO;
import org.gymbrot.dao.PagoDAO;
import org.gymbrot.model.Cliente;
import org.gymbrot.model.HistorialMembresia;
import org.gymbrot.model.Membresia;
import org.gymbrot.model.Pago;
import org.gymbrot.model.PlanMembresia;
import org.gymbrot.util.AlertaPersonalizada;
import org.gymbrot.util.ValidacionUtil;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class PagoMembresiaController implements Initializable {

    @FXML private Label lblMoneda;
    @FXML private Button btnCerrar;
    @FXML private TextField txtBuscarCliente;
    @FXML private ListView<String> lvClientes;
    @FXML private Label lblPlanNombre;
    @FXML private Label lblPlanModalidad;
    @FXML private TextField txtMonto;
    @FXML private ComboBox<String> cmbMetodoPago;
    @FXML private DatePicker dpFechaPago;
    @FXML private TextField txtReferencia;
    @FXML private TextArea txtObservaciones;
    @FXML private Button btnCancelar;
    @FXML private Button btnProcesar;

    private StackPane wrapperStack;
    private Parent overlayRoot;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final MembresiaDAO membresiaDAO = new MembresiaDAO();
    private final HistorialMembresiaDAO historialMembresiaDAO = new HistorialMembresiaDAO();
    private final PagoDAO pagoDAO = new PagoDAO();
    private final Map<String, Cliente> clientesMap = new HashMap<>();
    private Cliente clienteSeleccionado;
    private PlanMembresia planSeleccionado;
    private String modalidadSeleccionada;
    private boolean seleccionando;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbMetodoPago.setItems(FXCollections.observableArrayList(
                "EFECTIVO", "TRANSFERENCIA", "TARJETA", "NEQUI", "DAVIPLATA"
        ));
        dpFechaPago.setValue(LocalDate.now());
        lblMoneda.setText("$");
        ValidacionUtil.soloDecimales(txtMonto);
        cargarClientes();
        configurarBuscador();
    }

    private void cargarClientes() {
        List<Cliente> clientes = clienteDAO.listarTodos();
        for (Cliente c : clientes) {
            clientesMap.put(c.getNombre() + " " + c.getApellidos() + " (" + c.getNumeroIdentificacion() + ")", c);
        }
    }

    private void configurarBuscador() {
        txtBuscarCliente.textProperty().addListener((obs, old, val) -> {
            if (val == null || val.isBlank()) {
                lvClientes.setVisible(false);
                return;
            }
            if (seleccionando) return;
            if (clientesMap.containsKey(val.trim())) {
                lvClientes.setVisible(false);
                return;
            }
            List<String> filtrados = clientesMap.keySet().stream()
                    .filter(k -> k.toLowerCase().contains(val.toLowerCase()))
                    .limit(10)
                    .collect(Collectors.toList());
            lvClientes.getItems().setAll(filtrados);
            lvClientes.setVisible(!filtrados.isEmpty());
        });

        lvClientes.setOnMouseClicked(e -> {
            String seleccion = lvClientes.getSelectionModel().getSelectedItem();
            if (seleccion != null) {
                seleccionarCliente(seleccion);
            }
        });

        lvClientes.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.ENTER) {
                String seleccion = lvClientes.getSelectionModel().getSelectedItem();
                if (seleccion != null) {
                    seleccionarCliente(seleccion);
                }
            }
        });

        txtBuscarCliente.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused && !seleccionando) {
                Platform.runLater(() -> {
                    if (!lvClientes.isFocused() && !txtBuscarCliente.isFocused()) {
                        lvClientes.setVisible(false);
                    }
                });
            }
        });

        lvClientes.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-text-fill: white; -fx-background-color: #121417; -fx-font-family: 'Inter'; -fx-font-size: 13px; -fx-padding: 8 12 8 12;");
                }
            }
        });
        lvClientes.setFixedCellSize(36);
    }

    private void seleccionarCliente(String display) {
        seleccionando = true;
        txtBuscarCliente.setText(display);
        clienteSeleccionado = clientesMap.get(display);
        lvClientes.setVisible(false);
        seleccionando = false;
    }

    public void setPlan(PlanMembresia plan, String modalidad, double precio) {
        lblPlanNombre.setText(plan.getNombre());
        lblPlanModalidad.setText("Plan " + modalidad.substring(0, 1) + modalidad.substring(1).toLowerCase());
        txtMonto.setText(String.valueOf(Math.round(precio)));
        this.planSeleccionado = plan;
        this.modalidadSeleccionada = modalidad;
    }

    public void setWrapperStack(StackPane wrapper, Parent overlayRoot) {
        this.wrapperStack = wrapper;
        this.overlayRoot = overlayRoot;
    }

    @FXML
    private void handleCerrar() {
        cerrarOverlay();
    }

    @FXML
    private void handleCancelar() {
        cerrarOverlay();
    }

    @FXML
    private void handleProcesar() throws SQLException {
        if (clienteSeleccionado == null) {
            mostrarAlerta("Selecciona un cliente de la lista de busqueda");
            return;
        }
        if (planSeleccionado == null) {
            mostrarAlerta("No hay un plan seleccionado");
            return;
        }
        if (txtMonto.getText() == null || txtMonto.getText().trim().isEmpty()) {
            mostrarAlerta("Ingresa un monto");
            return;
        }
        double valor;
        try {
            valor = Double.parseDouble(txtMonto.getText().trim());
        } catch (NumberFormatException e) {
            mostrarAlerta("El monto debe ser un numero valido");
            return;
        }
        if (cmbMetodoPago.getValue() == null) {
            mostrarAlerta("Selecciona un metodo de pago");
            return;
        }

        String estado = "EXITOSO";
        int dias = switch (modalidadSeleccionada) {
            case "MENSUAL"   -> 30;
            case "SEMESTRAL" -> 180;
            case "ANUAL"     -> 365;
            default -> 30;
        };

        LocalDate hoy = LocalDate.now();
        Membresia m = new Membresia();
        m.setIdPlan(planSeleccionado.getIdPlan());
        m.setTipoMembresia(planSeleccionado.getNombre());
        m.setModalidadPago(modalidadSeleccionada);
        m.setValor(valor);
        m.setFechaInicio(hoy);
        m.setFechaVencimiento(hoy.plusDays(dias));
        m.setEstado("ACTIVA");

        int idMembresia = membresiaDAO.insertarYRetornarId(m);
        if (idMembresia < 0) {
            mostrarAlerta("Error al crear la membresia en la base de datos");
            return;
        }

        historialMembresiaDAO.desactivarPorCliente(clienteSeleccionado.getNumeroIdentificacion());

        HistorialMembresia h = new HistorialMembresia();
        h.setIdCliente(clienteSeleccionado.getNumeroIdentificacion());
        h.setIdMembresia(idMembresia);
        h.setFechaAsignacion(hoy);
        h.setActiva(true);
        if (!historialMembresiaDAO.insertar(h)) {
            System.err.println("ERROR: No se pudo insertar el historial de membresia para id_membresia=" + idMembresia);
            mostrarAlerta("Error al activar la membresia para el cliente. Contacta al administrador.");
            return;
        }

        Pago p = new Pago();
        p.setIdMembresia(idMembresia);
        p.setIdCliente(clienteSeleccionado.getNumeroIdentificacion());
        p.setFechaPago(dpFechaPago.getValue() != null ? dpFechaPago.getValue() : hoy);
        p.setValor(valor);
        p.setMetodoPago(cmbMetodoPago.getValue());
        p.setEstadoPago(estado);
        p.setReferenciaTransaccion(txtReferencia.getText().trim());
        p.setObservaciones(txtObservaciones.getText().trim());
        if (!pagoDAO.insertar(p)) {
            System.err.println("ERROR: No se pudo insertar el pago para id_membresia=" + idMembresia);
            mostrarAlerta("La membresia se creo pero hubo un error al registrar el pago. Revisa el historial.");
            return;
        }

        AlertaPersonalizada.exito("Pago Procesado",
                "Membresia " + planSeleccionado.getNombre()
                + " activada para " + clienteSeleccionado.getNombre()
                + " " + clienteSeleccionado.getApellidos() + ".");
        cerrarOverlay();
    }

    private void cerrarOverlay() {
        if (wrapperStack != null && overlayRoot != null) {
            wrapperStack.getChildren().remove(overlayRoot);
        }
    }

    private void mostrarAlerta(String mensaje) {
        AlertaPersonalizada.error("Validacion", mensaje);
    }
}
