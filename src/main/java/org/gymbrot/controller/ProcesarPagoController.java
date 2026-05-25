package org.gymbrot.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class ProcesarPagoController implements Initializable {

    @FXML private Label lblTransactionId;
    @FXML private Label lblMoneda;
    @FXML private Button btnCerrar;
    @FXML private ComboBox<String> cmbCliente;
    @FXML private Label lblPlanNombre;
    @FXML private Label lblPlanModalidad;
    @FXML private TextField txtMonto;
    @FXML private ComboBox<String> cmbMetodoPago;
    @FXML private DatePicker dpFechaPago;
    @FXML private RadioButton rbCompletado;
    @FXML private RadioButton rbPendiente;
    @FXML private TextField txtReferencia;
    @FXML private TextArea txtObservaciones;
    @FXML private ToggleGroup tgEstado;
    @FXML private Button btnCancelar;
    @FXML private Button btnProcesar;

    private StackPane wrapperStack;
    private Parent overlayRoot;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cmbMetodoPago.setItems(FXCollections.observableArrayList(
                "EFECTIVO", "TRANSFERENCIA", "TARJETA", "NEQUI", "DAVIPLATA"
        ));
        dpFechaPago.setValue(LocalDate.now());
        lblTransactionId.setText("TXN-" + DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").format(java.time.LocalDateTime.now()));
        lblMoneda.setText("$");
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
    private void handleProcesar() {
        if (cmbCliente.getValue() == null) {
            mostrarAlerta("Selecciona un cliente");
            return;
        }
        if (txtMonto.getText() == null || txtMonto.getText().trim().isEmpty()) {
            mostrarAlerta("Ingresa un monto");
            return;
        }
        if (cmbMetodoPago.getValue() == null) {
            mostrarAlerta("Selecciona un metodo de pago");
            return;
        }

        Alert info = new Alert(Alert.AlertType.INFORMATION);
        info.setTitle("Pago Procesado");
        info.setHeaderText(null);
        info.setContentText("Pago registrado exitosamente.");
        info.showAndWait();
        cerrarOverlay();
    }

    private void cerrarOverlay() {
        if (wrapperStack != null && overlayRoot != null) {
            wrapperStack.getChildren().remove(overlayRoot);
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validacion");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
