package org.gymbrot.model;

public class PlanMembresia {

    private int idPlan;
    private String nombre;
    private String descripcion;
    private double precioMensual;
    private double precioSemestral;
    private double precioAnual;
    private String beneficios;

    public PlanMembresia() {}

    public PlanMembresia(int idPlan, String nombre, String descripcion, double precioMensual,
                         double precioSemestral, double precioAnual, String beneficios) {
        this.idPlan = idPlan;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.precioMensual = precioMensual;
        this.precioSemestral = precioSemestral;
        this.precioAnual = precioAnual;
        this.beneficios = beneficios;
    }

    public int getIdPlan() { return idPlan; }
    public void setIdPlan(int idPlan) { this.idPlan = idPlan; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public double getPrecioMensual() { return precioMensual; }
    public void setPrecioMensual(double precioMensual) { this.precioMensual = precioMensual; }

    public double getPrecioSemestral() { return precioSemestral; }
    public void setPrecioSemestral(double precioSemestral) { this.precioSemestral = precioSemestral; }

    public double getPrecioAnual() { return precioAnual; }
    public void setPrecioAnual(double precioAnual) { this.precioAnual = precioAnual; }

    public String getBeneficios() { return beneficios; }
    public void setBeneficios(String beneficios) { this.beneficios = beneficios; }

    @Override
    public String toString() {
        return "PlanMembresia{" +
                "idPlan=" + idPlan +
                ", nombre='" + nombre + '\'' +
                ", precioMensual=" + precioMensual +
                ", precioSemestral=" + precioSemestral +
                ", precioAnual=" + precioAnual +
                '}';
    }
}

