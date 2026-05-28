package org.gymbrot.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;

import org.gymbrot.dao.CitaDAO;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.model.Cita;
import org.gymbrot.model.Cliente;
import org.gymbrot.util.AlertaPersonalizada;

import java.io.File;
import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

public class PerfilCitaController implements Initializable {

    @FXML private Label lblBadgeEstado;
    @FXML private Label lblTituloCita;
    @FXML private ImageView imgFotoCliente;
    @FXML private Label lblInicialesAvatar;
    @FXML private Label lblNombreCliente;
    @FXML private Label lblInstructor;
    @FXML private Label lblFecha;
    @FXML private Label lblHora;
    @FXML private Label lblTipo;
    @FXML private Label lblDetalleCliente;
    @FXML private Label lblDetalleInstructor;
    @FXML private Label lblDetalleFechaHora;
    @FXML private Label lblDetalleTipo;
    @FXML private Label lblNotas;
    @FXML private Label lblEstadoActual;
    @FXML private Button btnCompletar;
    @FXML private Button btnCancelar;
    @FXML private Button btnCerrar;

    private final CitaDAO citaDAO = new CitaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();

    private StackPane wrapperStack;
    private Parent overlayRoot;
    private Cita cita;
    private Runnable onClose;

    @Override
    public void initialize(URL url, ResourceBundle rb) {}

    public void setWrapperStack(StackPane wrapper, Parent overlay) {
        this.wrapperStack = wrapper;
        this.overlayRoot = overlay;
        btnCerrar.setOnAction(e -> cerrar());
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    private void cerrar() {
        if (wrapperStack != null && overlayRoot != null) {
            wrapperStack.getChildren().remove(overlayRoot);
        }
        if (onClose != null) onClose.run();
    }

    public void setCita(Cita cita) {
        this.cita = cita;
        cargarDatos();
    }

    private void cargarDatos() {
        if (cita == null) return;

        Optional<Cliente> optCliente = clienteDAO.buscarPorIdString(cita.getIdCliente());
        String nombreCliente = optCliente
                .map(c -> c.getNombre() + " " + c.getApellidos())
                .orElse(cita.getIdCliente());
        String nombreInstructor = instructorDAO.buscarPorIdString(cita.getIdInstructor())
                .map(i -> i.getNombre() + " " + i.getApellidos())
                .orElse(cita.getIdInstructor());

        String estado = cita.getEstado().toUpperCase();

        String colorEstado = switch (estado) {
            case "PENDIENTE" -> "#fbbf24";
            case "CONFIRMADA" -> "#60a5fa";
            case "COMPLETADA" -> "#D4FF00";
            case "CANCELADA" -> "#ffb4ab";
            default -> "white";
        };

        lblBadgeEstado.setText(estado);
        lblBadgeEstado.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: " + colorEstado + ";");

        lblTituloCita.setText(cita.getTipoCita() + " — " + cita.getFecha());

        // ── Avatar ──
        String[] partes = nombreCliente.split(" ");
        String iniciales = partes.length >= 2
                ? partes[0].charAt(0) + "" + partes[1].charAt(0)
                : nombreCliente.substring(0, Math.min(2, nombreCliente.length()));
        String iniciais = iniciales.toUpperCase();

        if (optCliente.isPresent()) {
            String fotoUrl = optCliente.get().getFotoUrl();
            if (fotoUrl != null && !fotoUrl.isBlank()) {
                try {
                    Image img = new Image(new File(fotoUrl).toURI().toString(), false);
                    if (!img.isError()) {
                        imgFotoCliente.setImage(img);
                        lblInicialesAvatar.setVisible(false);
                    } else {
                        lblInicialesAvatar.setText(iniciais);
                        lblInicialesAvatar.setVisible(true);
                    }
                } catch (Exception e) {
                    lblInicialesAvatar.setText(iniciais);
                    lblInicialesAvatar.setVisible(true);
                }
            } else {
                lblInicialesAvatar.setText(iniciais);
                lblInicialesAvatar.setVisible(true);
            }
        } else {
            lblInicialesAvatar.setText(iniciais);
            lblInicialesAvatar.setVisible(true);
        }

        lblNombreCliente.setText(nombreCliente);
        lblInstructor.setText(nombreInstructor);
        lblFecha.setText(cita.getFecha().toString());
        lblHora.setText(cita.getHora().toString());
        lblTipo.setText(cita.getTipoCita());

        lblDetalleCliente.setText(nombreCliente);
        lblDetalleInstructor.setText(nombreInstructor);
        lblDetalleFechaHora.setText(cita.getFecha() + " — " + cita.getHora());
        lblDetalleTipo.setText(cita.getTipoCita());

        lblNotas.setText(cita.getNotas() != null && !cita.getNotas().isBlank()
                ? cita.getNotas() : "Sin notas registradas.");

        lblEstadoActual.setText(estado);
        lblEstadoActual.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 12px; -fx-font-weight: 700; -fx-text-fill: " + colorEstado + "; -fx-padding: 4 12 4 12;");

        boolean editable = !"CANCELADA".equals(estado) && !"COMPLETADA".equals(estado);
        btnCompletar.setVisible(editable);
        btnCompletar.setManaged(editable);
        btnCancelar.setVisible(editable);
        btnCancelar.setManaged(editable);

        btnCompletar.setOnAction(e -> {
            cita.setEstado("COMPLETADA");
            citaDAO.actualizar(cita);
            cerrar();
        });

        btnCancelar.setOnAction(e -> {
            if (AlertaPersonalizada.confirmar("Cancelar Cita",
                    "Cancelar cita con " + nombreCliente + " del " + cita.getFecha() + "?")) {
                cita.setEstado("CANCELADA");
                citaDAO.actualizar(cita);
                cerrar();
            }
        });
    }
}
