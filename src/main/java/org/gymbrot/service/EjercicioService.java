package org.gymbrot.service;

import org.gymbrot.dao.EjercicioDAO;
import org.gymbrot.model.Ejercicio;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.List;

public class EjercicioService {

    private final EjercicioDAO ejercicioDAO;

    public EjercicioService() {
        this.ejercicioDAO = new EjercicioDAO();
    }

    public List<Ejercicio> listarEjercicios() {
        try {
            return ejercicioDAO.listarTodos();
        } catch (Exception e) {
            System.err.println("Error en listarEjercicios: " + e.getMessage());
            return List.of();
        }
    }

    public List<Ejercicio> buscarPorGrupo(String grupoMuscular) {
        try {
            if (grupoMuscular == null || grupoMuscular.isBlank()) {
                return ejercicioDAO.listarTodos();
            }
            return ejercicioDAO.buscarPorGrupoMuscular(grupoMuscular);
        } catch (Exception e) {
            System.err.println("Error en buscarPorGrupo: " + e.getMessage());
            return List.of();
        }
    }

    public List<Ejercicio> buscarPorNivel(String nivel) {
        try {
            if (nivel == null || nivel.isBlank()) {
                return ejercicioDAO.listarTodos();
            }
            return ejercicioDAO.buscarPorNivel(nivel);
        } catch (Exception e) {
            System.err.println("Error en buscarPorNivel: " + e.getMessage());
            return List.of();
        }
    }

    public Ejercicio buscarPorId(int id) {
        try {
            return ejercicioDAO.buscarPorId(id);
        } catch (Exception e) {
            System.err.println("Error en buscarPorId: " + e.getMessage());
            return null;
        }
    }

    public boolean insertar(Ejercicio ejercicio) {
        String sql = "{call PKG_GYMBROT.SP_CREAR_EJERCICIO(?,?,?,?,?,?,?,?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setString(1, ejercicio.getNombre());
            cs.setString(2, ejercicio.getDescripcion());
            cs.setString(3, ejercicio.getGrupoMuscular());
            if (ejercicio.getSeries() > 0) {
                cs.setInt(4, ejercicio.getSeries());
            } else {
                cs.setNull(4, Types.INTEGER);
            }
            if (ejercicio.getRepeticiones() > 0) {
                cs.setInt(5, ejercicio.getRepeticiones());
            } else {
                cs.setNull(5, Types.INTEGER);
            }
            if (ejercicio.getDuracionMinutos() > 0) {
                cs.setInt(6, ejercicio.getDuracionMinutos());
            } else {
                cs.setNull(6, Types.INTEGER);
            }
            cs.setString(7, ejercicio.getNivel());
            cs.setString(8, ejercicio.getRecursoUrl());
            cs.registerOutParameter(9, Types.INTEGER);
            cs.registerOutParameter(10, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(9);
            String mensaje = cs.getString(10);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en insertar: " + e.getMessage());
            return false;
        }
    }

    public boolean actualizar(Ejercicio ejercicio) {
        String sql = "{call PKG_GYMBROT.SP_ACTUALIZAR_EJERCICIO(?,?,?,?,?,?,?,?,?,?,?)}";
        try (Connection conn = DatabaseConnection.getInstance();
             CallableStatement cs = conn.prepareCall(sql)) {

            cs.setInt(1, ejercicio.getIdEjercicio());
            cs.setString(2, ejercicio.getNombre());
            cs.setString(3, ejercicio.getDescripcion());
            cs.setString(4, ejercicio.getGrupoMuscular());
            if (ejercicio.getSeries() > 0) {
                cs.setInt(5, ejercicio.getSeries());
            } else {
                cs.setNull(5, Types.INTEGER);
            }
            if (ejercicio.getRepeticiones() > 0) {
                cs.setInt(6, ejercicio.getRepeticiones());
            } else {
                cs.setNull(6, Types.INTEGER);
            }
            if (ejercicio.getDuracionMinutos() > 0) {
                cs.setInt(7, ejercicio.getDuracionMinutos());
            } else {
                cs.setNull(7, Types.INTEGER);
            }
            cs.setString(8, ejercicio.getNivel());
            cs.setString(9, ejercicio.getRecursoUrl());
            cs.registerOutParameter(10, Types.INTEGER);
            cs.registerOutParameter(11, Types.VARCHAR);
            cs.execute();

            int codigo = cs.getInt(10);
            String mensaje = cs.getString(11);
            System.out.println(mensaje);
            return codigo == 1;

        } catch (SQLException e) {
            System.err.println("Error en actualizar: " + e.getMessage());
            return false;
        }
    }



    public boolean eliminar(int idEjercicio) {
        try {
            return ejercicioDAO.eliminar(idEjercicio);
        } catch (Exception e) {
            System.err.println("Error en eliminar: " + e.getMessage());
            return false;
        }
    }

    public List<String> listarGruposMusculares() {
        try {
            return ejercicioDAO.listarTodos().stream()
                    .map(Ejercicio::getGrupoMuscular)
                    .distinct()
                    .sorted()
                    .toList();
        } catch (Exception e) {
            System.err.println("Error en listarGruposMusculares: " + e.getMessage());
            return List.of();
        }
    }

    public List<String> listarNiveles() {
        try {
            return ejercicioDAO.listarTodos().stream()
                    .map(Ejercicio::getNivel)
                    .distinct()
                    .sorted()
                    .toList();
        } catch (Exception e) {
            System.err.println("Error en listarNiveles: " + e.getMessage());
            return List.of("PRINCIPIANTE", "INTERMEDIO", "AVANZADO");
        }
    }
}
