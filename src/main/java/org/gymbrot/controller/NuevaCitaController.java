package org.gymbrot.controller;

import javafx.animation.ScaleTransition;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.HistorialMembresiaDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.model.Cita;
import org.gymbrot.model.Cliente;
import org.gymbrot.model.Instructor;
import org.gymbrot.service.CitaService;
import org.gymbrot.util.AlertaPersonalizada;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;


public class NuevaCitaController implements Initializable {

    @FXML private Button btnCerrar;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    @FXML private ComboBox<String> cmbCliente;
    @FXML private ComboBox<String> cmbInstructor;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<String> cmbHora;
    @FXML private ComboBox<String> cmbTipoCita;
    @FXML private TextArea txtNotas;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final InstructorDAO instructorDAO = new InstructorDAO();
    private final HistorialMembresiaDAO historialMembresiaDAO = new HistorialMembresiaDAO();
    private final CitaService citaService = new CitaService();

    private StackPane wrapperStack;
    private Parent overlayRoot;

    // Almacena las IDs para lookup desde el nombre visible
    private List<Cliente> clientes;
    private List<Instructor> instructores;

    private Cita citaEditando;
    private final List<String> todosLosHorarios = new ArrayList<>();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarClientes();
        cargarInstructores();
        configurarHoras();
        configurarTiposCita();
        configurarAnimaciones();
        configurarFiltros();
    }

    private void cargarClientes() {
        clientes = clienteDAO.listarTodos();
        for (Cliente c : clientes) {
            String nombre = c.getNombre() + " " + (c.getApellidos() != null ? c.getApellidos() : "");
            String info = c.getNumeroIdentificacion() + " — " + nombre;
            cmbCliente.getItems().add(info);
        }
    }

    private void cargarInstructores() {
        instructores = instructorDAO.listarTodos();
        for (Instructor i : instructores) {
            String nombre = i.getNombre() + " " + (i.getApellidos() != null ? i.getApellidos() : "");
            String info = i.getNumeroIdentificacion() + " — " + nombre;
            cmbInstructor.getItems().add(info);
        }
    }

    private void configurarHoras() {
        todosLosHorarios.clear();
        cmbHora.getItems().clear();
        for (int h = 6; h <= 22; h++) {
            String hhmm = String.format("%02d:00", h);
            String hh30 = String.format("%02d:30", h);
            todosLosHorarios.add(hhmm);
            todosLosHorarios.add(hh30);
            cmbHora.getItems().add(hhmm);
            cmbHora.getItems().add(hh30);
        }
    }

    private void configurarTiposCita() {
        cmbTipoCita.getItems().addAll("EVALUACION", "SEGUIMIENTO", "NUTRICION", "CONSULTA");
    }

    private void configurarAnimaciones() {
        ScaleTransition grow = new ScaleTransition(Duration.millis(160), btnGuardar);
        ScaleTransition shrink = new ScaleTransition(Duration.millis(160), btnGuardar);
        grow.setToX(1.04); grow.setToY(1.04);
        shrink.setToX(1.0); shrink.setToY(1.0);
        btnGuardar.setOnMouseEntered(e -> grow.playFromStart());
        btnGuardar.setOnMouseExited(e -> shrink.playFromStart());

        ScaleTransition growC = new ScaleTransition(Duration.millis(160), btnCerrar);
        ScaleTransition shrinkC = new ScaleTransition(Duration.millis(160), btnCerrar);
        growC.setToX(1.15); growC.setToY(1.15);
        shrinkC.setToX(1.0); shrinkC.setToY(1.0);
        btnCerrar.setOnMouseEntered(e -> growC.playFromStart());
        btnCerrar.setOnMouseExited(e -> shrinkC.playFromStart());
    }

    private void configurarFiltros() {
        cmbInstructor.valueProperty().addListener((obs, oldV, newV) -> filtrarHoras());
        dpFecha.valueProperty().addListener((obs, oldV, newV) -> filtrarHoras());
    }

    private void filtrarHoras() {
        String instrValue = cmbInstructor.getValue();
        LocalDate fecha = dpFecha.getValue();
        String idInstructor;
        if (instrValue != null) idInstructor = instrValue.split(" — ")[0].trim();
        else {
            idInstructor = null;
        }

        cmbHora.getItems().clear();

        for (String h : todosLosHorarios) {
            boolean incluir = true;

            if (idInstructor != null) {
                Instructor instructor = instructores.stream()
                        .filter(i -> i.getNumeroIdentificacion().equals(idInstructor))
                        .findFirst().orElse(null);
                if (instructor != null && instructor.getDisponibilidad() != null) {
                    String disp = instructor.getDisponibilidad();
                    String[] partes = disp.split("\\|");
                    if (partes.length >= 2) {
                        // Filtrar por horario
                        String turno = partes[1].trim();
                        LocalTime[] rango = HORARIO_MAP.get(turno);
                        if (rango != null) {
                            LocalTime hora = LocalTime.parse(h, DateTimeFormatter.ofPattern("HH:mm"));
                            if (hora.isBefore(rango[0]) || hora.isAfter(rango[1])) {
                                incluir = false;
                            }
                        }

                        // Filtrar por día si hay fecha seleccionada
                        if (incluir && fecha != null) {
                            DayOfWeek diaSel = fecha.getDayOfWeek();
                            String[] diasDisp = partes[0].split(",");
                            boolean diaValido = false;
                            for (String d : diasDisp) {
                                DayOfWeek dw = DIA_MAP.get(d.trim());
                                if (dw == diaSel) { diaValido = true; break; }
                            }
                            if (!diaValido) incluir = false;
                        }
                    }
                }
            }

            if (incluir) cmbHora.getItems().add(h);
        }
    }

    public void setWrapperStack(StackPane wrapper, Parent overlayRoot) {
        this.wrapperStack = wrapper;
        this.overlayRoot = overlayRoot;
    }

    public void seleccionarInstructor(String instructorId) {
        for (String item : cmbInstructor.getItems()) {
            if (item.startsWith(instructorId)) {
                cmbInstructor.setValue(item);
                break;
            }
        }
    }

    public void seleccionarCliente(String clienteId) {
        for (String item : cmbCliente.getItems()) {
            if (item.startsWith(clienteId)) {
                cmbCliente.setValue(item);
                break;
            }
        }
    }

    public void cargarCitaExistente(Cita cita) {
        this.citaEditando = cita;
        seleccionarCliente(cita.getIdCliente());
        seleccionarInstructor(cita.getIdInstructor());
        if (cita.getFecha() != null) {
            dpFecha.setValue(cita.getFecha());
        }
        filtrarHoras();
        cmbHora.setValue(cita.getHora().format(DateTimeFormatter.ofPattern("HH:mm")));
        cmbTipoCita.setValue(cita.getTipoCita());
        txtNotas.setText(cita.getNotas());
        btnGuardar.setText("ACTUALIZAR CITA");
    }

    private static final Map<String, DayOfWeek> DIA_MAP = Map.of(
            "LUN", DayOfWeek.MONDAY, "MAR", DayOfWeek.TUESDAY,
            "MIE", DayOfWeek.WEDNESDAY, "JUE", DayOfWeek.THURSDAY,
            "VIE", DayOfWeek.FRIDAY, "SAB", DayOfWeek.SATURDAY,
            "DOM", DayOfWeek.SUNDAY
    );

    private static final Map<String, LocalTime[]> HORARIO_MAP = Map.of(
            "Mañana",   new LocalTime[]{LocalTime.of(6, 0),  LocalTime.of(12, 0)},
            "Tarde",    new LocalTime[]{LocalTime.of(12, 0), LocalTime.of(18, 0)},
            "Noche",    new LocalTime[]{LocalTime.of(18, 0), LocalTime.of(22, 0)},
            "Completo", new LocalTime[]{LocalTime.of(6, 0),  LocalTime.of(22, 0)}
    );

    private boolean validarMembresia(String idCliente) {
        return historialMembresiaDAO.buscarActiva(idCliente) != null;
    }

    private boolean validarDisponibilidadInstructor(String idInstructor, LocalDate fecha, LocalTime hora) {
        Instructor instructor = instructores.stream()
                .filter(i -> i.getNumeroIdentificacion().equals(idInstructor))
                .findFirst().orElse(null);
        if (instructor == null || instructor.getDisponibilidad() == null) return false;

        String disp = instructor.getDisponibilidad();
        String[] partes = disp.split("\\|");
        if (partes.length < 2) return false;

        // Validar día
        DayOfWeek diaSeleccionado = fecha.getDayOfWeek();
        String[] diasDisp = partes[0].split(",");
        boolean diaValido = false;
        for (String d : diasDisp) {
            DayOfWeek dw = DIA_MAP.get(d.trim());
            if (dw == diaSeleccionado) { diaValido = true; break; }
        }
        if (!diaValido) return false;

        // Validar horario
        String horario = partes[1].trim();
        LocalTime[] rango = HORARIO_MAP.get(horario);
        if (rango == null) return false;
        return !hora.isBefore(rango[0]) && !hora.isAfter(rango[1]);
    }

    @FXML
    private void handleGuardar() {
        if (cmbCliente.getValue() == null) {
            mostrarAlerta("Selecciona un cliente");
            return;
        }
        if (cmbInstructor.getValue() == null) {
            mostrarAlerta("Selecciona un instructor");
            return;
        }
        if (dpFecha.getValue() == null) {
            mostrarAlerta("Selecciona una fecha");
            return;
        }
        if (cmbHora.getValue() == null) {
            mostrarAlerta("Selecciona una hora");
            return;
        }
        if (cmbTipoCita.getValue() == null) {
            mostrarAlerta("Selecciona el tipo de cita");
            return;
        }

        String idCliente = cmbCliente.getValue().split(" — ")[0].trim();
        String idInstructor = cmbInstructor.getValue().split(" — ")[0].trim();
        LocalDate fecha = dpFecha.getValue();
        LocalTime hora = LocalTime.parse(cmbHora.getValue(), DateTimeFormatter.ofPattern("HH:mm"));

        // Validación 1: fecha no puede ser anterior a hoy
        if (fecha.isBefore(LocalDate.now())) {
            mostrarAlerta("No se puede agendar una cita en el pasado.");
            return;
        }

        // Validación 2: cliente debe tener membresía activa
        if (!validarMembresia(idCliente)) {
            mostrarAlerta("El cliente no tiene una membresía activa. No se puede agendar la cita.");
            return;
        }

        // Validación 3: hora debe estar dentro de la disponibilidad del instructor
        if (!validarDisponibilidadInstructor(idInstructor, fecha, hora)) {
            mostrarAlerta("El instructor no está disponible en la fecha u hora seleccionada.");
            return;
        }

        if (citaEditando != null) {
            citaEditando.setIdCliente(idCliente);
            citaEditando.setIdInstructor(idInstructor);
            citaEditando.setFecha(fecha);
            citaEditando.setHora(hora);
            citaEditando.setTipoCita(cmbTipoCita.getValue());
            citaEditando.setNotas(txtNotas.getText() != null ? txtNotas.getText().trim() : null);

            boolean ok = citaService.actualizarCita(citaEditando);
            if (ok) {
                mostrarInfo("Cita actualizada exitosamente.");
                cerrarOverlay();
            } else {
                mostrarAlerta("Error al actualizar la cita.");
            }
        } else {
            Cita cita = new Cita();
            cita.setIdCliente(idCliente);
            cita.setIdInstructor(idInstructor);
            cita.setFecha(fecha);
            cita.setHora(hora);
            cita.setTipoCita(cmbTipoCita.getValue());
            cita.setEstado("PENDIENTE");
            cita.setNotas(txtNotas.getText() != null ? txtNotas.getText().trim() : null);

            boolean ok = citaService.programarCita(cita);
            if (ok) {
                mostrarInfo("La cita ha sido agendada exitosamente.");
                cerrarOverlay();
            } else {
                mostrarAlerta("Error al agendar la cita. Intenta de nuevo.");
            }
        }
    }

    @FXML
    private void handleCerrar() {
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

    private void mostrarInfo(String mensaje) {
        AlertaPersonalizada.info("Informacion", mensaje);
    }
}
