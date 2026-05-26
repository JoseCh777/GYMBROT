package org.gymbrot.service;

import org.gymbrot.util.DatabaseConnection;
import java.sql.*;

public class DashboardService {

    private Connection conn;

    public DashboardService() {
        try {
            this.conn = DatabaseConnection.getInstance();
        } catch (SQLException e) {
            System.err.println("Error conexión DashboardService: " + e.getMessage());
        }
    }

    public int contarMiembrosActivos() {
        String sql = "{? = call PKG_GYMBROT.FN_CONTAR_MIEMBROS_ACTIVOS}";
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.execute();
            return cs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error FN_CONTAR_MIEMBROS_ACTIVOS: " + e.getMessage());
            return 0;
        }
    }

    public int contarActivosHoy() {
        String sql = "{? = call PKG_GYMBROT.FN_CONTAR_ACTIVOS_HOY}";
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.INTEGER);
            cs.execute();
            return cs.getInt(1);
        } catch (SQLException e) {
            System.err.println("Error FN_CONTAR_ACTIVOS_HOY: " + e.getMessage());
            return 0;
        }
    }

    public double ingresosMesActual() {
        String sql = "{? = call PKG_GYMBROT.FN_INGRESOS_MES_ACTUAL}";
        try (CallableStatement cs = conn.prepareCall(sql)) {
            cs.registerOutParameter(1, Types.DOUBLE);
            cs.execute();
            return cs.getDouble(1);
        } catch (SQLException e) {
            System.err.println("Error FN_INGRESOS_MES_ACTUAL: " + e.getMessage());
            return 0;
        }
    }

    public int[] cargarDemografia() {
        String sql = "SELECT " +
                "COUNT(CASE WHEN PKG_GYMBROT.FN_CALCULAR_EDAD(c.fecha_nacimiento) < 18 THEN 1 END) as menores, " +
                "COUNT(CASE WHEN PKG_GYMBROT.FN_CALCULAR_EDAD(c.fecha_nacimiento) BETWEEN 18 AND 64 THEN 1 END) as adultos, " +
                "COUNT(CASE WHEN PKG_GYMBROT.FN_CALCULAR_EDAD(c.fecha_nacimiento) >= 65 THEN 1 END) as adultos_mayores " +
                "FROM CLIENTES c JOIN USUARIOS u ON u.numero_identificacion = c.id_cliente " +
                "WHERE u.estado = 'ACTIVO'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return new int[]{rs.getInt("menores"), rs.getInt("adultos"), rs.getInt("adultos_mayores")};
            }
        } catch (SQLException e) {
            System.err.println("Error cargarDemografia: " + e.getMessage());
        }
        return new int[]{0, 0, 0};
    }
}
