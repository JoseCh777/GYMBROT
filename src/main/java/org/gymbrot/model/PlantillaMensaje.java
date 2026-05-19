package org.gymbrot.model;

public class PlantillaMensaje {

    private int idPlantilla;
    private String nombre;
    private String tipo;
    private String asunto;
    private String cuerpoHtml;
    private String cuerpoTexto;
    private String variablesDisponibles;
    private boolean activa;

    public PlantillaMensaje() {}

    public PlantillaMensaje(int idPlantilla, String nombre, String tipo, String asunto,
                            String cuerpoHtml, String cuerpoTexto,
                            String variablesDisponibles, boolean activa) {
        this.idPlantilla = idPlantilla;
        this.nombre = nombre;
        this.tipo = tipo;
        this.asunto = asunto;
        this.cuerpoHtml = cuerpoHtml;
        this.cuerpoTexto = cuerpoTexto;
        this.variablesDisponibles = variablesDisponibles;
        this.activa = activa;
    }

    public int getIdPlantilla() { return idPlantilla; }
    public void setIdPlantilla(int idPlantilla) { this.idPlantilla = idPlantilla; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getAsunto() { return asunto; }
    public void setAsunto(String asunto) { this.asunto = asunto; }

    public String getCuerpoHtml() { return cuerpoHtml; }
    public void setCuerpoHtml(String cuerpoHtml) { this.cuerpoHtml = cuerpoHtml; }

    public String getCuerpoTexto() { return cuerpoTexto; }
    public void setCuerpoTexto(String cuerpoTexto) { this.cuerpoTexto = cuerpoTexto; }

    public String getVariablesDisponibles() { return variablesDisponibles; }
    public void setVariablesDisponibles(String variablesDisponibles) { this.variablesDisponibles = variablesDisponibles; }

    public boolean isActiva() { return activa; }
    public void setActiva(boolean activa) { this.activa = activa; }

    @Override
    public String toString() {
        return "PlantillaMensaje{" +
                "idPlantilla=" + idPlantilla +
                ", nombre='" + nombre + '\'' +
                ", tipo='" + tipo + '\'' +
                ", asunto='" + asunto + '\'' +
                ", activa=" + activa +
                '}';
    }
}
