package org.gymbrot.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.gymbrot.dao.EspecialidadDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.model.Especialidad;
import org.gymbrot.model.Instructor;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class PerfilInstructorController implements Initializable {

    @FXML private ImageView imgFotoPerfil;
    @FXML private Label lblInicialesAvatar;
    @FXML private Label lblNombreInstructor;
    @FXML private Label lblEspecialidad;
    @FXML private Label lblEstadoInstructor;
    @FXML private Label lblMiembroDesde;

    @FXML private Label lblTipoDocumento;
    @FXML private Label lblCorreo;
    @FXML private Label lblTelefono;
    @FXML private Label lblFechaContratacion;
    @FXML private Label lblDisponibilidad;

    @FXML private Button btnCerrar;
    @FXML private Button btnEditar;

    private StackPane wrapperStack;
    private Parent overlayRoot;

    private InstructorDAO instructorDAO;
    private EspecialidadDAO especialidadDAO;
    private Instructor instructor;

    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        instructorDAO = new InstructorDAO();
        especialidadDAO = new EspecialidadDAO();
    }

    public void setWrapperStack(StackPane wrapper, Parent overlay) {
        this.wrapperStack = wrapper;
        this.overlayRoot = overlay;
    }

    public void setInstructor(Instructor instructor) {
        this.instructor = instructor;
        if (instructor != null) {
            Platform.runLater(this::cargarDatos);
        }
    }

    private void cargarDatos() {
        lblNombreInstructor.setText(instructor.getNombre() + " " + instructor.getApellidos());

        String fotoUrl = instructor.getFotoUrl();
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

        Especialidad esp = especialidadDAO.buscarPorId(instructor.getIdEspecialidad());
        lblEspecialidad.setText(esp != null ? esp.getNombre() : "---");

        lblEstadoInstructor.setText(instructor.getEstado());
        if ("ACTIVO".equalsIgnoreCase(instructor.getEstado()))
            lblEstadoInstructor.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #D4FF00;");
        else
            lblEstadoInstructor.setStyle("-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px; -fx-font-weight: 700; -fx-text-fill: #ef4444;");

        if (instructor.getFechaRegistro() != null)
            lblMiembroDesde.setText(instructor.getFechaRegistro().format(FMT_FECHA));

        String tipo = instructor.getTipoIdentificacion();
        String num = instructor.getNumeroIdentificacion();
        lblTipoDocumento.setText((tipo != null ? tipo : "---") + " / " + (num != null ? num : "---"));
        lblCorreo.setText(instructor.getCorreo() != null ? instructor.getCorreo() : "---");
        lblTelefono.setText(instructor.getTelefono() != null ? instructor.getTelefono() : "---");
        if (instructor.getFechaContratacion() != null)
            lblFechaContratacion.setText(instructor.getFechaContratacion().format(FMT_FECHA));
        String disp = instructor.getDisponibilidad();
        if (disp != null && !disp.isBlank()) {
            String[] partes = disp.split("\\|");
            StringBuilder dispTexto = new StringBuilder();
            if (partes.length > 0 && !partes[0].isBlank()) {
                dispTexto.append("Días: ").append(partes[0].replace(",", ", "));
            }
            if (partes.length > 1 && !partes[1].isBlank()) {
                if (!dispTexto.isEmpty()) dispTexto.append("\n");
                dispTexto.append("Horario: ").append(partes[1]);
            }
            lblDisponibilidad.setText(dispTexto.toString());
        } else {
            lblDisponibilidad.setText("No especificada");
        }
    }

    private void mostrarIniciales() {
        String nombre = instructor.getNombre();
        String apellido = instructor.getApellidos();
        String iniciales = "";
        if (nombre != null && !nombre.isBlank()) iniciales += nombre.charAt(0);
        if (apellido != null && !apellido.isBlank()) iniciales += apellido.charAt(0);
        lblInicialesAvatar.setText(iniciales.isBlank() ? "?" : iniciales.toUpperCase());
        lblInicialesAvatar.setVisible(true);
    }

    @FXML
    private void handleCerrar() {
        if (wrapperStack != null && overlayRoot != null) {
            wrapperStack.getChildren().remove(overlayRoot);
        }
    }

    @FXML
    private void handleEditar() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NuevoInstructor.fxml"));
            Parent overlay = loader.load();
            NuevoInstructorController ctrl = loader.getController();
            ctrl.setInstructor(instructor);

            StackPane wrapper = new StackPane();
            Stage stage = (Stage) btnCerrar.getScene().getWindow();
            Parent rootActual = stage.getScene().getRoot();
            if (rootActual instanceof StackPane sp && sp.getChildren().size() > 1) {
                wrapper.getChildren().add(sp.getChildren().get(0));
            } else {
                wrapper.getChildren().add(rootActual);
            }
            wrapper.getChildren().add(overlay);
            ctrl.setWrapperStack(wrapper, overlay);
            stage.getScene().setRoot(wrapper);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
