package org.gymbrot.service;

import org.gymbrot.dao.CitaDAO;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.model.Cita;
import org.gymbrot.model.Instructor;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class CitaService {

    private CitaDAO citaDAO;
    private ClienteDAO clienteDAO;
    private InstructorDAO instructorDAO;

    public CitaService() {
        this.citaDAO = new CitaDAO();
        this.clienteDAO = new ClienteDAO();
        this.instructorDAO = new InstructorDAO();
    }

    // ── LISTAR POR CLIENTE ────────────────────────────────────────────────
    public List<Cita> listarPorCliente(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return null;
        }
        return citaDAO.listarPorCliente(idCliente);
    }

    // ── LISTAR POR INSTRUCTOR ─────────────────────────────────────────────
    public List<Cita> listarPorInstructor(String idInstructor) {
        if (idInstructor == null || idInstructor.trim().isEmpty()) {
            System.err.println("✗ ID de instructor inválido.");
            return null;
        }
        return citaDAO.listarPorInstructor(idInstructor);
    }

    // ── LISTAR POR FECHA ──────────────────────────────────────────────────
    public List<Cita> listarPorFecha(LocalDate fecha) {
        if (fecha == null) {
            System.err.println("✗ Fecha inválida.");
            return null;
        }
        return citaDAO.listarPorFecha(fecha);
    }

    // ── PROGRAMAR CITA (retorna boolean) ──────────────────────────────────
    public boolean programarCita(Cita cita) {
        return validarYProgramar(cita) && citaDAO.insertar(cita);
    }

    // ── PROGRAMAR CITA Y RETORNAR ID ──────────────────────────────────────
    public int programarCitaYRetornarId(Cita cita) {
        if (!validarYProgramar(cita)) return -1;
        int id = citaDAO.insertarYRetornarId(cita);
        if (id > 0) System.out.println("✓ Cita programada exitosamente.");
        return id;
    }

    // ── VALIDACIONES COMPARTIDAS ──────────────────────────────────────────
    private boolean validarYProgramar(Cita cita) {
        if (cita.getIdCliente() == null || cita.getIdCliente().trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return false;
        }
        if (cita.getIdInstructor() == null || cita.getIdInstructor().trim().isEmpty()) {
            System.err.println("✗ ID de instructor inválido.");
            return false;
        }
        if (clienteDAO.buscarPorId(cita.getIdCliente()) == null) {
            System.err.println("✗ No se encontró el cliente.");
            return false;
        }
        Instructor instructor = instructorDAO.buscarPorId(cita.getIdInstructor());
        if (instructor == null) {
            System.err.println("✗ No se encontró el instructor.");
            return false;
        }
        if (cita.getFecha() == null || !cita.getFecha().isAfter(LocalDate.now())) {
            System.err.println("✗ La fecha debe ser posterior a hoy.");
            return false;
        }
        if (cita.getHora() == null) {
            System.err.println("✗ La hora no puede estar vacía.");
            return false;
        }

        // ✅ Validar que el día esté en la disponibilidad del instructor
        if (!diaDisponible(instructor.getDisponibilidad(), cita.getFecha().getDayOfWeek())) {
            System.err.println("✗ El instructor no trabaja ese día. Disponibilidad: " + instructor.getDisponibilidad());
            return false;
        }

        // ✅ Validar que la hora esté dentro del rango del instructor
        if (!horaDisponible(instructor.getDisponibilidad(), cita.getHora())) {
            System.err.println("✗ La hora está fuera del horario del instructor. Disponibilidad: " + instructor.getDisponibilidad());
            return false;
        }

        // Verificar conflicto de horario
        List<Cita> citasInstructor = citaDAO.listarPorInstructor(cita.getIdInstructor());
        for (Cita c : citasInstructor) {
            if (c.getFecha().equals(cita.getFecha()) &&
                    c.getHora().equals(cita.getHora()) &&
                    c.getEstado().equals("PENDIENTE")) {
                System.err.println("✗ El instructor ya tiene una cita en ese horario.");
                return false;
            }
        }
        cita.setEstado("PENDIENTE");
        return true;
    }

    // ── VALIDAR DÍA ───────────────────────────────────────────────────────
    private boolean diaDisponible(String disponibilidad, DayOfWeek dia) {
        if (disponibilidad == null) return true;
        String disp = disponibilidad.toLowerCase();

        // Mapear día de la semana a texto en español
        String nombreDia = switch (dia) {
            case MONDAY    -> "lunes";
            case TUESDAY   -> "martes";
            case WEDNESDAY -> "miercoles";
            case THURSDAY  -> "jueves";
            case FRIDAY    -> "viernes";
            case SATURDAY  -> "sabado";
            case SUNDAY    -> "domingo";
        };

        // Manejar rangos: "lunes a viernes", "lunes a sabado"
        if (disp.contains(" a ")) {
            String[] partes = disp.split(" a ");
            if (partes.length >= 2) {
                String diaInicio = partes[0].trim().replaceAll("[^a-z]", "");
                // Extraer solo el primer día de la parte después de "a"
                String diaFin = partes[1].trim().split("[\\s,]")[0].replaceAll("[^a-z]", "");
                int inicio = diaANumero(diaInicio);
                int fin = diaANumero(diaFin);
                int diaNum = diaANumero(nombreDia);
                if (inicio >= 0 && fin >= 0 && diaNum >= 0) {
                    return diaNum >= inicio && diaNum <= fin;
                }
            }
        }

        // Manejar días específicos: "martes, jueves y sabado"
        return disp.contains(nombreDia);
    }

    private int diaANumero(String dia) {
        return switch (dia.trim()) {
            case "lunes"      -> 1;
            case "martes"     -> 2;
            case "miercoles"  -> 3;
            case "jueves"     -> 4;
            case "viernes"    -> 5;
            case "sabado"     -> 6;
            case "domingo"    -> 7;
            default           -> -1;
        };
    }

    // ── VALIDAR HORA ──────────────────────────────────────────────────────
    private boolean horaDisponible(String disponibilidad, LocalTime hora) {
        if (disponibilidad == null) return true;
        String disp = disponibilidad.toLowerCase();

        // Buscar patrón de hora: "6am-2pm", "8am-12pm", "2pm-10pm"
        java.util.regex.Pattern rangoPattern = java.util.regex.Pattern.compile(
            "(\\d{1,2})(am|pm)-(\\d{1,2})(am|pm)");
        java.util.regex.Matcher m = rangoPattern.matcher(disp);

        if (m.find()) {
            int hInicio = Integer.parseInt(m.group(1));
            String ampmInicio = m.group(2);
            int hFin = Integer.parseInt(m.group(3));
            String ampmFin = m.group(4);

            // Convertir a formato 24h
            if (ampmInicio.equals("pm") && hInicio != 12) hInicio += 12;
            if (ampmInicio.equals("am") && hInicio == 12) hInicio = 0;
            if (ampmFin.equals("pm") && hFin != 12) hFin += 12;
            if (ampmFin.equals("am") && hFin == 12) hFin = 0;

            LocalTime horaInicio = LocalTime.of(hInicio, 0);
            LocalTime horaFin = LocalTime.of(hFin, 0);

            return !hora.isBefore(horaInicio) && hora.isBefore(horaFin);
        }
        return true;
    }

    // ── CANCELAR CITA ─────────────────────────────────────────────────────
    public boolean cancelarCita(int idCita) {
        if (idCita <= 0) {
            System.err.println("✗ ID de cita inválido.");
            return false;
        }
        Cita cita = citaDAO.buscarPorId(idCita);
        if (cita == null) {
            System.err.println("✗ No se encontró la cita con ID: " + idCita);
            return false;
        }
        if (!cita.getEstado().equals("PENDIENTE")) {
            System.err.println("✗ Solo se pueden cancelar citas en estado PENDIENTE.");
            return false;
        }
        cita.setEstado("CANCELADA");
        boolean resultado = citaDAO.actualizar(cita);
        if (resultado) System.out.println("✓ Cita cancelada exitosamente.");
        return resultado;
    }

    // ── COMPLETAR CITA ────────────────────────────────────────────────────
    public boolean completarCita(int idCita) {
        if (idCita <= 0) {
            System.err.println("✗ ID de cita inválido.");
            return false;
        }
        Cita cita = citaDAO.buscarPorId(idCita);
        if (cita == null) {
            System.err.println("✗ No se encontró la cita con ID: " + idCita);
            return false;
        }
        if (!cita.getEstado().equals("PENDIENTE")) {
            System.err.println("✗ Solo se pueden completar citas en estado PENDIENTE.");
            return false;
        }
        cita.setEstado("COMPLETADA");
        boolean resultado = citaDAO.actualizar(cita);
        if (resultado) System.out.println("✓ Cita completada exitosamente.");
        return resultado;
    }
}