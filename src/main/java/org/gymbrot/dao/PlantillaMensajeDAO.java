package org.gymbrot.dao;

import org.gymbrot.model.PlantillaMensaje;
import org.gymbrot.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class PlantillaMensajeDAO {

    private Connection getConexion() {
        return DatabaseConnection.getInstance();
    }

    // ── LISTAR ACTIVAS ────────────────────────────────────────────────────
    public List<PlantillaMensaje> listarActivas() {
        List<PlantillaMensaje> lista = new ArrayList<>();
        String sql = """
                SELECT * FROM PLANTILLAS_MENSAJE
                WHERE activa = 1
                ORDER BY nombre
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) lista.add(mapear(rs));
        } catch (SQLException e) {
            System.err.println("Error al listar plantillas activas: " + e.getMessage());
        }
        return lista;
    }

    // ── BUSCAR POR TIPO ───────────────────────────────────────────────────
    public PlantillaMensaje buscarPorTipo(String tipo) {
        String sql = """
                SELECT * FROM PLANTILLAS_MENSAJE
                WHERE tipo = ? AND activa = 1
                """;
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setString(1, tipo);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar plantilla por tipo: " + e.getMessage());
        }
        return null;
    }
    // ── BUSCAR POR ID ─────────────────────────────────────────────────────
    public PlantillaMensaje buscarPorId(int idPlantilla) {
        String sql = "SELECT * FROM PLANTILLAS_MENSAJE WHERE id_plantilla = ?";
        try (PreparedStatement ps = getConexion().prepareStatement(sql)) {
            ps.setInt(1, idPlantilla);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapear(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error al buscar plantilla por id: " + e.getMessage());
        }
        return null;
    }

    // ── MAPEAR ResultSet → PlantillaMensaje ───────────────────────────────
    private PlantillaMensaje mapear(ResultSet rs) throws SQLException {
        PlantillaMensaje p = new PlantillaMensaje();
        p.setIdPlantilla(rs.getInt("id_plantilla"));
        p.setNombre(rs.getString("nombre"));
        p.setTipo(rs.getString("tipo"));
        p.setAsunto(rs.getString("asunto"));
        p.setCuerpoHtml(rs.getString("cuerpo_html"));
        p.setCuerpoTexto(rs.getString("cuerpo_texto"));
        p.setVariablesDisponibles(rs.getString("variables_disponibles"));
        p.setActiva(rs.getInt("activa") == 1);
        return p;
    }
}