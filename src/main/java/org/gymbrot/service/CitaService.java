package org.gymbrot.service;

import org.gymbrot.dao.CitaDAO;
import org.gymbrot.dao.ClienteDAO;
import org.gymbrot.dao.InstructorDAO;
import org.gymbrot.model.Cita;
import org.gymbrot.model.Instructor;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
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
        if (!validarYProgramar(cita)) return false;

        int id = citaDAO.insertarYRetornarId(cita);
        if (id > 0) {
            System.out.println("✓ Cita programada exitosamente. ID: #" + id);
            return true;
        }
        return false;
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
        if (cita.getFecha() == null || cita.getFecha().isBefore(LocalDate.now())) {
            System.err.println("✗ La fecha no puede ser anterior a hoy.");
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
        if (cita.getFecha() == null || cita.getFecha().isBefore(LocalDate.now())) {
            System.err.println("✗ La fecha no puede ser anterior a hoy.");
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
    public boolean diaDisponible(String disponibilidad, DayOfWeek dia) {
        if (disponibilidad == null || disponibilidad.trim().isEmpty()) return true;
        String disp = disponibilidad.toLowerCase().trim();
        String nombreDia = switch (dia) {
            case MONDAY    -> "lunes";
            case TUESDAY   -> "martes";
            case WEDNESDAY -> "miercoles";
            case THURSDAY  -> "jueves";
            case FRIDAY    -> "viernes";
            case SATURDAY  -> "sabado";
            case SUNDAY    -> "domingo";
        };
        String abrevDia = nombreDia.substring(0, 3);

        String parteDias = disp;
        if (disp.contains("|")) {
            String[] partes = disp.split("\\|");
            if (partes.length > 0) {
                parteDias = partes[0].trim();
            } else {
                parteDias = "";
            }
        }

        if (parteDias.contains(" a ")) {
            String[] partes = parteDias.split(" a ");
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

        if (parteDias.contains(nombreDia) || parteDias.contains(abrevDia)) return true;

        String[] diasLista = parteDias.split(",");
        for (String d : diasLista) {
            d = d.trim();
            if (d.equals(abrevDia) || d.equals(nombreDia)) return true;
        }
        return false;
    }

    private int diaANumero(String dia) {
        return switch (dia.trim()) {
            case "lunes", "lun"     -> 1;
            case "martes", "mar"    -> 2;
            case "miercoles", "mie" -> 3;
            case "jueves", "jue"    -> 4;
            case "viernes", "vie"   -> 5;
            case "sabado", "sab"    -> 6;
            case "domingo", "dom"   -> 7;
            default                 -> -1;
        };
    }

    // ── VALIDAR HORA ──────────────────────────────────────────────────────
    public boolean horaDisponible(String disponibilidad, LocalTime hora) {
        if (disponibilidad == null) return true;
        String disp = disponibilidad.toLowerCase();

        String parteHora = disp.contains("|") ? disp.split("\\|")[1].trim() : disp;

        if (parteHora.contains("mañana") || parteHora.contains("manana")) {
            LocalTime horaInicio = LocalTime.of(6, 0);
            LocalTime horaFin    = LocalTime.of(12, 0);
            return !hora.isBefore(horaInicio) && hora.isBefore(horaFin);
        }
        if (parteHora.contains("tarde")) {
            LocalTime horaInicio = LocalTime.of(12, 0);
            LocalTime horaFin    = LocalTime.of(18, 0);
            return !hora.isBefore(horaInicio) && hora.isBefore(horaFin);
        }
        if (parteHora.contains("noche")) {
            LocalTime horaInicio = LocalTime.of(18, 0);
            LocalTime horaFin    = LocalTime.of(23, 0);
            return !hora.isBefore(horaInicio) && hora.isBefore(horaFin);
        }

        java.util.regex.Pattern rangoPattern = java.util.regex.Pattern.compile(
                "(\\d{1,2})(am|pm)-(\\d{1,2})(am|pm)");
        java.util.regex.Matcher m = rangoPattern.matcher(parteHora);
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

        String sql = "{call PKG_GYMBROT_PROC.SP_CANCELAR_CITA(?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, idCita);
            cs.registerOutParameter(2, Types.INTEGER);
            cs.registerOutParameter(3, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(2);
            String mensaje = cs.getString(3);
            System.out.println("[cancelarCita] " + mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en cancelarCita: " + e.getMessage());
            return false;
        }
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
