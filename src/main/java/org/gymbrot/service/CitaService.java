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

    // ── LISTAR TODAS ──────────────────────────────────────────────────────
    public List<Cita> listarTodas() {
        return citaDAO.listarTodas();
    }

    // ── LISTAR POR CLIENTE ────────────────────────────────────────────────
    public List<Cita> listarPorCliente(String idCliente) {
        if (idCliente == null || idCliente.trim().isEmpty()) return null;
        return citaDAO.listarPorCliente(idCliente);
    }

    // ── LISTAR POR INSTRUCTOR ─────────────────────────────────────────────
    public List<Cita> listarPorInstructor(String idInstructor) {
        if (idInstructor == null || idInstructor.trim().isEmpty()) return null;
        return citaDAO.listarPorInstructor(idInstructor);
    }

    // ── LISTAR POR FECHA ──────────────────────────────────────────────────
    public List<Cita> listarPorFecha(LocalDate fecha) {
        if (fecha == null) return null;
        return citaDAO.listarPorFecha(fecha);
    }

    // ── PROGRAMAR CITA (retorna boolean) ──────────────────────────────────
    public boolean programarCita(Cita cita) {
        return validarYProgramar(cita) && citaDAO.insertar(cita);
    }

    // ── PROGRAMAR CITA Y RETORNAR ID (para cliente) ───────────────────────
    public int programarCitaYRetornarId(Cita cita) {
        if (!validarYProgramar(cita)) return -1;
        int id = citaDAO.insertarYRetornarId(cita);
        if (id > 0) System.out.println("✓ Cita programada exitosamente.");
        return id;
    }

    // ── PROGRAMAR CITA DESDE ADMIN (retorna código de error específico) ───
    // Códigos: id > 0 = éxito | -1 = cliente inválido | -2 = instructor inválido
    //          -3 = cliente no existe | -4 = instructor no existe
    //          -5 = fecha inválida | -6 = hora inválida
    //          -7 = instructor no trabaja ese día | -8 = hora fuera de horario
    //          -9 = conflicto de horario
    public int programarCitaAdminYRetornarId(Cita cita) {
        if (cita.getIdCliente() == null || cita.getIdCliente().trim().isEmpty()) {
            System.err.println("✗ ID de cliente inválido.");
            return -1;
        }
        if (cita.getIdInstructor() == null || cita.getIdInstructor().trim().isEmpty()) {
            System.err.println("✗ ID de instructor inválido.");
            return -2;
        }
        if (clienteDAO.buscarPorId(cita.getIdCliente()) == null) {
            System.err.println("✗ No se encontró el cliente.");
            return -3;
        }
        Instructor instructor = instructorDAO.buscarPorId(cita.getIdInstructor());
        if (instructor == null) {
            System.err.println("✗ No se encontró el instructor.");
            return -4;
        }
        if (cita.getFecha() == null || !cita.getFecha().isAfter(LocalDate.now())) {
            System.err.println("✗ La fecha debe ser posterior a hoy.");
            return -5;
        }
        if (cita.getHora() == null) {
            System.err.println("✗ La hora no puede estar vacía.");
            return -6;
        }
        if (!diaDisponible(instructor.getDisponibilidad(), cita.getFecha().getDayOfWeek())) {
            System.err.println("✗ El instructor no trabaja ese día. Disponibilidad: " + instructor.getDisponibilidad());
            return -7;
        }
        if (!horaDisponible(instructor.getDisponibilidad(), cita.getHora())) {
            System.err.println("✗ La hora está fuera del horario del instructor. Disponibilidad: " + instructor.getDisponibilidad());
            return -8;
        }
        // Verificar conflicto de horario
        List<Cita> citasInstructor = citaDAO.listarPorInstructor(cita.getIdInstructor());
        for (Cita c : citasInstructor) {
            if (c.getFecha().equals(cita.getFecha()) &&
                    c.getHora().equals(cita.getHora()) &&
                    c.getEstado().equals("PENDIENTE")) {
                System.err.println("✗ El instructor ya tiene una cita en ese horario.");
                return -9;
            }
        }
        cita.setEstado("PENDIENTE");
        int id = citaDAO.insertarYRetornarId(cita);
        if (id > 0) System.out.println("✓ Cita admin programada exitosamente. ID: #" + id);
        return id;
    }

    // ── VALIDACIONES PARA CLIENTE ─────────────────────────────────────────
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
        if (!diaDisponible(instructor.getDisponibilidad(), cita.getFecha().getDayOfWeek())) {
            System.err.println("✗ El instructor no trabaja ese día. Disponibilidad: " + instructor.getDisponibilidad());
            return false;
        }
        if (!horaDisponible(instructor.getDisponibilidad(), cita.getHora())) {
            System.err.println("✗ La hora está fuera del horario del instructor. Disponibilidad: " + instructor.getDisponibilidad());
            return false;
        }
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
        String nombreDia = switch (dia) {
            case MONDAY    -> "lunes";
            case TUESDAY   -> "martes";
            case WEDNESDAY -> "miercoles";
            case THURSDAY  -> "jueves";
            case FRIDAY    -> "viernes";
            case SATURDAY  -> "sabado";
            case SUNDAY    -> "domingo";
        };
        if (disp.contains(" a ")) {
            String[] partes = disp.split(" a ");
            if (partes.length >= 2) {
                String diaInicio = partes[0].trim().replaceAll("[^a-z]", "");
                String diaFin = partes[1].trim().split("[\\s,]")[0].replaceAll("[^a-z]", "");
                int inicio = diaANumero(diaInicio);
                int fin = diaANumero(diaFin);
                int diaNum = diaANumero(nombreDia);
                if (inicio >= 0 && fin >= 0 && diaNum >= 0) {
                    return diaNum >= inicio && diaNum <= fin;
                }
            }
        }
        return disp.contains(nombreDia);
    }

    private int diaANumero(String dia) {
        return switch (dia.trim()) {
            case "lunes"     -> 1;
            case "martes"    -> 2;
            case "miercoles" -> 3;
            case "jueves"    -> 4;
            case "viernes"   -> 5;
            case "sabado"    -> 6;
            case "domingo"   -> 7;
            default          -> -1;
        };
    }

    // ── VALIDAR HORA ──────────────────────────────────────────────────────
    private boolean horaDisponible(String disponibilidad, LocalTime hora) {
        if (disponibilidad == null) return true;
        String disp = disponibilidad.toLowerCase();
        java.util.regex.Pattern rangoPattern = java.util.regex.Pattern.compile(
                "(\\d{1,2})(am|pm)-(\\d{1,2})(am|pm)");
        java.util.regex.Matcher m = rangoPattern.matcher(disp);
        if (m.find()) {
            int hInicio = Integer.parseInt(m.group(1));
            String ampmInicio = m.group(2);
            int hFin = Integer.parseInt(m.group(3));
            String ampmFin = m.group(4);
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

    // ── ACTUALIZAR CITA ───────────────────────────────────────────────────
    public boolean actualizarCita(Cita cita) {
        try {
            return citaDAO.actualizar(cita);
        } catch (Exception e) {
            System.err.println("Error en actualizarCita: " + e.getMessage());
            return false;
        }
    }

    // ── CANCELAR CITA ─────────────────────────────────────────────────────
    public boolean cancelarCita(int idCita) {
        if (idCita <= 0) return false;

        System.out.println("[cancelarCita] Buscando cita id=" + idCita);
        Cita cita = citaDAO.buscarPorId(idCita);
        System.out.println("[cancelarCita] encontrada=" + (cita != null));

        if (cita == null) return false;

        System.out.println("[cancelarCita] estado=" + cita.getEstado());
        if (!cita.getEstado().equals("PENDIENTE")) return false;

        boolean resultado = citaDAO.actualizarEstado(idCita, "CANCELADA");
        System.out.println("[cancelarCita] resultado=" + resultado);
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
        boolean resultado = citaDAO.actualizarEstado(idCita, "COMPLETADA");
        if (resultado) System.out.println("✓ Cita completada exitosamente.");
        return resultado;
    }
}
