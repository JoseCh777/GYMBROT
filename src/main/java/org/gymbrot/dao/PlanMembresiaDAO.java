package org.gymbrot.dao;

import org.gymbrot.model.PlanMembresia;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlanMembresiaDAO {

    private Connection getConexion() throws SQLException  {
        return DatabaseConnection.getInstance();
    }

    // ── INSERTAR ──────────────────────────────────────────────────────────
    public boolean insertar(PlanMembresia p) {
        String sql = """
                INSERT INTO PLANES_MEMBRESIAS
                    (nombre, descripcion, precio_mensual,
                     precio_semestral, precio_anual, beneficios)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setDouble(3, p.getPrecioMensual());
            ps.setDouble(4, p.getPrecioSemestral());
            ps.setDouble(5, p.getPrecioAnual());
            ps.setString(6, p.getBeneficios());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar plan: " + e.getMessage());
            return false;
        }
    }
    // ── ACTUALIZAR ────────────────────────────────────────────────────────
    public boolean actualizar(PlanMembresia p) {
        String sql = """
                UPDATE PLANES_MEMBRESIAS
                SET nombre = ?, descripcion = ?, precio_mensual = ?,
                    precio_semestral = ?, precio_anual = ?, beneficios = ?
                WHERE id_plan = ?
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, p.getNombre());
            ps.setString(2, p.getDescripcion());
            ps.setDouble(3, p.getPrecioMensual());
            ps.setDouble(4, p.getPrecioSemestral());
            ps.setDouble(5, p.getPrecioAnual());
            ps.setString(6, p.getBeneficios());
            ps.setInt(7, p.getIdPlan());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar plan: " + e.getMessage());
            return false;
        }
    }
    // ── ELIMINAR ──────────────────────────────────────────────────────────
    public boolean eliminar(int idPlan) {
        String sql = "DELETE FROM PLANES_MEMBRESIAS WHERE id_plan = ?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idPlan);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error al eliminar plan: " + e.getMessage());
            return false;
        }
    }
    // ── BUSCAR POR ID ─────────────────────────────────────────────────────
    public PlanMembresia buscarPorId(int idPlan) {
        String sql = "SELECT * FROM PLANES_MEMBRESIAS WHERE id_plan = ?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idPlan);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar plan: " + e.getMessage());
        }
        return null;
    }
    // ── LISTAR TODOS ──────────────────────────────────────────────────────
    public List<PlanMembresia> listarTodos() {
        List<PlanMembresia> lista = new ArrayList<>();
        String sql = "SELECT * FROM PLANES_MEMBRESIAS ORDER BY nombre";
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.err.println("Error al listar planes: " + e.getMessage());
        }
        return lista;
    }
    // ── MAPEAR ResultSet → PlanMembresia ──────────────────────────────────
    private PlanMembresia mapear(ResultSet rs) throws SQLException {
        PlanMembresia p = new PlanMembresia();
        p.setIdPlan(rs.getInt("id_plan"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setPrecioMensual(rs.getDouble("precio_mensual"));
        p.setPrecioSemestral(rs.getDouble("precio_semestral"));
        p.setPrecioAnual(rs.getDouble("precio_anual"));
        p.setBeneficios(rs.getString("beneficios"));
        return p;
    }
}