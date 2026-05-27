package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.beans.binding.Bindings;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.gymbrot.dao.CitaDAO;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.model.Cita;
import org.gymbrot.service.CitaService;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class GestionCitasController implements Initializable {

    @FXML private VBox sideNav;
    @FXML private HBox topBar;
    @FXML private TextField searchField;
    @FXML private TextField searchField1;
    @FXML private Button btnAgregarCita;

    @FXML private Label lblCitasHoy;
    @FXML private Label lblPendientes;
    @FXML private Label lblCompletadas;
    @FXML private Label lblCanceladas;

    @FXML private TableView<Cita> tablaCitasProximas;
    @FXML private TableColumn<Cita, String> colProxCliente;
    @FXML private TableColumn<Cita, String> colProxInstructor;
    @FXML private TableColumn<Cita, String> colProxFecha;
    @FXML private TableColumn<Cita, String> colProxHora;
    @FXML private TableColumn<Cita, String> colProxTipo;
    @FXML private TableColumn<Cita, String> colProxEstado;
    @FXML private TableColumn<Cita, String> colProxAcciones;

    @FXML private TableView<Cita> tablaCitasAnteriores;
    @FXML private TableColumn<Cita, String> colAntCliente;
    @FXML private TableColumn<Cita, String> colAntInstructor;
    @FXML private TableColumn<Cita, String> colAntFecha;
    @FXML private TableColumn<Cita, String> colAntHora;
    @FXML private TableColumn<Cita, String> colAntTipo;
    @FXML private TableColumn<Cita, String> colAntEstado;

    private final CitaService citaService = new CitaService();
    private final CitaDAO citaDAO = new CitaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();

    private ObservableList<Cita> todasLasCitas = FXCollections.observableArrayList();
    private ObservableList<Cita> citasAnteriores = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        configurarColumnas();
        cargarCitas();
        configurarBusqueda();
        configurarBusqueda2();

        agregarAnimaciones();
    }

    private void configurarColumnas() {
        colProxCliente.setCellValueFactory(cellData -> {
            String id = cellData.getValue().getIdCliente();
            String nombre = clienteDAO.buscarPorIdString(id)
                    .map(c -> c.getNombre() + " " + c.getApellidos())
                    .orElse(id);
            return javafx.beans.binding.Bindings.createStringBinding(() -> nombre);
        });
        colProxInstructor.setCellValueFactory(cellData -> {
            String id = cellData.getValue().getIdInstructor();
            String nombre = instructorDAO.buscarPorIdString(id)
                    .map(i -> i.getNombre() + " " + i.getApellidos())
                    .orElse(id);
            return javafx.beans.binding.Bindings.createStringBinding(() -> nombre);
        });
        colProxFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colProxHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colProxTipo.setCellValueFactory(new PropertyValueFactory<>("tipoCita"));
        colProxEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colProxEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(switch (item.toUpperCase()) {
                        case "PENDIENTE" -> "-fx-text-fill: #fbbf24;";
                        case "CONFIRMADA" -> "-fx-text-fill: #60a5fa;";
                        case "COMPLETADA" -> "-fx-text-fill: #D4FF00;";
                        case "CANCELADA" -> "-fx-text-fill: #ffb4ab;";
                        default -> "-fx-text-fill: white;";
                    });
                }
            }
        });
        colProxAcciones.setCellValueFactory(data -> new javafx.beans.property.SimpleStringProperty(""));
        colProxAcciones.setCellFactory(col -> new TableCell<>() {
            private final Button btnVer      = new Button("Ver");
            private final Button btnEditar   = new Button("Ed.");
            private final Button btnCancelar = new Button("El.");

            {
                btnVer.setStyle("-fx-background-color: rgba(212,255,0,0.15); -fx-background-radius: 6;" +
                        "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700;" +
                        "-fx-text-fill: #D4FF00; -fx-border-color: #D4FF00; -fx-border-width: 1;" +
                        "-fx-border-radius: 6; -fx-cursor: hand; -fx-padding: 4 8 4 8;");
                btnVer.setOnMouseEntered(e -> btnVer.setStyle(
                        "-fx-background-color: #D4FF00; -fx-background-radius: 6;" +
                                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700;" +
                                "-fx-text-fill: black; -fx-border-color: #D4FF00; -fx-border-width: 1;" +
                                "-fx-border-radius: 6; -fx-cursor: hand; -fx-padding: 4 8 4 8;"));
                btnVer.setOnMouseExited(e -> btnVer.setStyle(
                        "-fx-background-color: rgba(212,255,0,0.15); -fx-background-radius: 6;" +
                                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 10px; -fx-font-weight: 700;" +
                                "-fx-text-fill: #D4FF00; -fx-border-color: #D4FF00; -fx-border-width: 1;" +
                                "-fx-border-radius: 6; -fx-cursor: hand; -fx-padding: 4 8 4 8;"));

                btnEditar.setStyle(
                        "-fx-background-color: rgba(96,165,250,0.15); -fx-background-radius: 6;" +
                                "-fx-border-color: #60a5fa; -fx-border-width: 1; -fx-border-radius: 6;" +
                                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                                "-fx-text-fill: #60a5fa; -fx-cursor: hand;" +
                                "-fx-padding: 4 8 4 8;");
                btnEditar.setOnMouseEntered(e -> btnEditar.setStyle(
                        "-fx-background-color: #60a5fa; -fx-background-radius: 6;" +
                                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                                "-fx-text-fill: black; -fx-border-color: #60a5fa; -fx-border-width: 1;" +
                                "-fx-border-radius: 6; -fx-cursor: hand;" +
                                "-fx-padding: 4 8 4 8;"));
                btnEditar.setOnMouseExited(e -> btnEditar.setStyle(
                        "-fx-background-color: rgba(96,165,250,0.15); -fx-background-radius: 6;" +
                                "-fx-border-color: #60a5fa; -fx-border-width: 1; -fx-border-radius: 6;" +
                                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                                "-fx-text-fill: #60a5fa; -fx-cursor: hand;" +
                                "-fx-padding: 4 8 4 8;"));

                btnCancelar.setStyle(
                        "-fx-background-color: rgba(255,180,171,0.15); -fx-background-radius: 6;" +
                                "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 6;" +
                                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                                "-fx-text-fill: #ffb4ab; -fx-cursor: hand;" +
                                "-fx-padding: 4 8 4 8;");
                btnCancelar.setOnMouseEntered(e -> btnCancelar.setStyle(
                        "-fx-background-color: #ffb4ab; -fx-background-radius: 6;" +
                                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                                "-fx-text-fill: black; -fx-border-color: #ffb4ab; -fx-border-width: 1;" +
                                "-fx-border-radius: 6; -fx-cursor: hand;" +
                                "-fx-padding: 4 8 4 8;"));
                btnCancelar.setOnMouseExited(e -> btnCancelar.setStyle(
                        "-fx-background-color: rgba(255,180,171,0.15); -fx-background-radius: 6;" +
                                "-fx-border-color: #ffb4ab; -fx-border-width: 1; -fx-border-radius: 6;" +
                                "-fx-font-family: 'Space Grotesk'; -fx-font-size: 11px;" +
                                "-fx-text-fill: #ffb4ab; -fx-cursor: hand;" +
                                "-fx-padding: 4 8 4 8;"));

                btnVer.setOnAction(e -> {
                    Cita cita = getTableView().getItems().get(getIndex());
                    handleVerCita(cita);
                });
                btnEditar.setOnAction(e -> {
                    Cita cita = getTableView().getItems().get(getIndex());
                    handleEditarCita(cita);
                });
                btnCancelar.setOnAction(e -> {
                    Cita cita = getTableView().getItems().get(getIndex());
                    handleCancelarCita(cita);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                HBox box = new HBox(6, btnVer, btnEditar, btnCancelar);
                box.setAlignment(Pos.CENTER_RIGHT);
                setGraphic(box);
                setText(null);
                setStyle("-fx-background-color: transparent; -fx-padding: 8 12 8 12;");
            }
        });

        colAntCliente.setCellValueFactory(colProxCliente.getCellValueFactory());
        colAntInstructor.setCellValueFactory(colProxInstructor.getCellValueFactory());
        colAntFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colAntHora.setCellValueFactory(new PropertyValueFactory<>("hora"));
        colAntTipo.setCellValueFactory(new PropertyValueFactory<>("tipoCita"));
        colAntEstado.setCellValueFactory(new PropertyValueFactory<>("estado"));
        colAntEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    setStyle(switch (item.toUpperCase()) {
                        case "PENDIENTE" -> "-fx-text-fill: #fbbf24;";
                        case "CONFIRMADA" -> "-fx-text-fill: #60a5fa;";
                        case "COMPLETADA" -> "-fx-text-fill: #D4FF00;";
                        case "CANCELADA" -> "-fx-text-fill: #ffb4ab;";
                        default -> "-fx-text-fill: white;";
                    });
                }
            }
        });
    }

    private void cargarCitas() {
        List<Cita> todas = citaService.listarTodas();
        todasLasCitas.setAll(todas);

        LocalDate hoy = LocalDate.now();

        List<Cita> proximas = todas.stream()
                .filter(c -> {
                return !c.getFecha().isBefore(hoy);
                })
                .collect(Collectors.toList());

        List<Cita> anteriores = todas.stream()
                .filter(c -> c.getFecha().isBefore(hoy))
                .collect(Collectors.toList());

        tablaCitasProximas.setItems(FXCollections.observableArrayList(proximas));
        citasAnteriores.setAll(anteriores);
        tablaCitasAnteriores.setItems(citasAnteriores);

        actualizarStats(todas, hoy);
    }

    private void actualizarStats(List<Cita> todas, LocalDate hoy) {
        long hoyCount = todas.stream()
                .filter(c -> c.getFecha().equals(hoy))
                .count();
        long pendientes = todas.stream()
                .filter(c -> "PENDIENTE".equalsIgnoreCase(c.getEstado()))
                .count();
        long completadas = todas.stream()
                .filter(c -> "COMPLETADA".equalsIgnoreCase(c.getEstado()))
                .count();
        long canceladas = todas.stream()
                .filter(c -> "CANCELADA".equalsIgnoreCase(c.getEstado()))
                .count();

        lblCitasHoy.setText(String.valueOf(hoyCount));
        lblPendientes.setText(String.valueOf(pendientes));
        lblCompletadas.setText(String.valueOf(completadas));
        lblCanceladas.setText(String.valueOf(canceladas));
    }

    private void configurarBusqueda() {
        FilteredList<Cita> filtradas = new FilteredList<>(todasLasCitas, p -> true);
        searchField.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV.toLowerCase().trim();
            filtradas.setPredicate(cita -> {
                if (q.isEmpty()) return true;
                String cliente = clienteDAO.buscarPorIdString(cita.getIdCliente())
                        .map(c -> (c.getNombre() + " " + c.getApellidos()).toLowerCase())
                        .orElse("");
                String instructor = instructorDAO.buscarPorIdString(cita.getIdInstructor())
                        .map(i -> (i.getNombre() + " " + i.getApellidos()).toLowerCase())
                        .orElse("");
                return cliente.contains(q) || instructor.contains(q)
                        || cita.getFecha().toString().toLowerCase().contains(q)
                        || cita.getTipoCita().toLowerCase().contains(q)
                        || cita.getEstado().toLowerCase().contains(q);
            });
            LocalDate hoy = LocalDate.now();
            List<Cita> prox = filtradas.stream()
                    .filter(c -> !c.getFecha().isBefore(hoy))
                    .collect(Collectors.toList());
            List<Cita> ant = filtradas.stream()
                    .filter(c -> c.getFecha().isBefore(hoy))
                    .collect(Collectors.toList());
            tablaCitasProximas.setItems(FXCollections.observableArrayList(prox));
            citasAnteriores.setAll(ant);
            actualizarStats(filtradas, hoy);
        });
    }

    private void configurarBusqueda2() {
        FilteredList<Cita> filtradas = new FilteredList<>(citasAnteriores, p -> true);
        searchField1.textProperty().addListener((obs, oldV, newV) -> {
            String q = newV.toLowerCase().trim();
            filtradas.setPredicate(cita -> {
                if (q.isEmpty()) return true;
                String cliente = clienteDAO.buscarPorIdString(cita.getIdCliente())
                        .map(c -> (c.getNombre() + " " + c.getApellidos()).toLowerCase())
                        .orElse("");
                String instructor = instructorDAO.buscarPorIdString(cita.getIdInstructor())
                        .map(i -> (i.getNombre() + " " + i.getApellidos()).toLowerCase())
                        .orElse("");
                return cliente.contains(q) || instructor.contains(q)
                        || cita.getFecha().toString().toLowerCase().contains(q)
                        || cita.getTipoCita().toLowerCase().contains(q)
                        || cita.getEstado().toLowerCase().contains(q);
            });
            tablaCitasAnteriores.setItems(filtradas);
        });
    }

    @FXML
    private void handleAgregarCita() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NuevaCita.fxml"));
            Parent overlay = loader.load();
            NuevaCitaController ctrl = loader.getController();

            Scene scene = sideNav.getScene();
            Parent rootActual = scene.getRoot();

            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(rootActual);
            wrapper.getChildren().add(overlay);

            ctrl.setWrapperStack(wrapper, overlay);

            scene.setRoot(wrapper);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir el formulario de cita");
        }
    }

    private void handleVerCita(Cita cita) {
        mostrarAlerta("Detalles de Cita",
                "Cliente: " + obtenerNombreCliente(cita.getIdCliente()) + "\n" +
                "Instructor: " + obtenerNombreInstructor(cita.getIdInstructor()) + "\n" +
                "Fecha: " + cita.getFecha() + "\n" +
                "Hora: " + cita.getHora() + "\n" +
                "Tipo: " + cita.getTipoCita() + "\n" +
                "Estado: " + cita.getEstado() + "\n" +
                "Notas: " + (cita.getNotas() != null ? cita.getNotas() : "Sin notas"));
    }

    private void handleEditarCita(Cita cita) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/NuevaCita.fxml"));
            Parent overlay = loader.load();
            NuevaCitaController ctrl = loader.getController();

            Scene scene = sideNav.getScene();
            Parent rootActual = scene.getRoot();

            StackPane wrapper = new StackPane();
            wrapper.getChildren().add(rootActual);
            wrapper.getChildren().add(overlay);

            ctrl.setWrapperStack(wrapper, overlay);
            ctrl.cargarCitaExistente(cita);

            scene.setRoot(wrapper);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo abrir el editor de cita");
        }
    }

    private void handleCancelarCita(Cita cita) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Cancelar Cita");
        confirm.setHeaderText("Confirmar cancelacion");
        confirm.setContentText("Desea cancelar la cita con " + obtenerNombreCliente(cita.getIdCliente()) +
                " para el " + cita.getFecha() + "?");
        confirm.showAndWait().ifPresent(r -> {
            if (r == ButtonType.OK) {
                cita.setEstado("CANCELADA");
                citaDAO.actualizar(cita);
                cargarCitas();
            }
        });
    }

    private String obtenerNombreCliente(String id) {
        return clienteDAO.buscarPorIdString(id)
                .map(c -> c.getNombre() + " " + c.getApellidos())
                .orElse(id);
    }

    private String obtenerNombreInstructor(String id) {
        return instructorDAO.buscarPorIdString(id)
                .map(i -> i.getNombre() + " " + i.getApellidos())
                .orElse(id);
    }

    private void mostrarAlerta(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String contenido) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(contenido);
        DialogPane dp = alert.getDialogPane();
        dp.setStyle("-fx-background-color: #1a1d21; -fx-font-family: 'Inter';");
        dp.lookup(".content.label").setStyle("-fx-text-fill: white;");
        alert.showAndWait();
    }

    private void agregarAnimaciones() {
        ScaleTransition grow = new ScaleTransition(Duration.millis(160), btnAgregarCita);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(160), btnAgregarCita);
        grow.setToX(1.04); grow.setToY(1.04);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btnAgregarCita.setOnMouseEntered(e -> grow.playFromStart());
        btnAgregarCita.setOnMouseExited(e -> shrink.playFromStart());
    }

    @FXML private void handleNavDashboard() { navegar("/fxml/Dashboard.fxml"); }
    @FXML private void handleNavClientes() { navegar("/fxml/GestionClientes.fxml"); }
    @FXML private void handleNavInstructores() { navegar("/fxml/GestionInstructores.fxml"); }
    @FXML private void handleNavCitas() { navegar("/fxml/GestionCitas.fxml"); }
    @FXML private void handleNavMembresias() { navegar("/fxml/GestionMembresias.fxml"); }
    @FXML private void handleNavProgreso() { navegar("/fxml/ProgresoFisico.fxml"); }
    @FXML private void handleNavAI() { navegar("/fxml/GymbroAI.fxml"); }

    private void navegar(String fxml) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            Stage stage = (Stage) sideNav.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
            mostrarError("Error", "No se pudo cargar la vista: " + fxml);
        }
    }

    @FXML
    private void handleLogout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Seguro que deseas cerrar sesion?", ButtonType.YES, ButtonType.NO);
        alert.setTitle("Cerrar sesion");
        alert.setHeaderText(null);
        alert.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.YES) navegar("/fxml/login.fxml");
        });
    }
}
