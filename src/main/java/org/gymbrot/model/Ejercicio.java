package org.gymbrot.model;

public class Ejercicio {

    private int idEjercicio;
    private String nombre;
    private String descripcion;
    private String grupoMuscular;
    private int series;
    private int repeticiones;
    private int duracionMinutos;
    private String nivel;
    private String recursoUrl;

    public Ejercicio() {}

    public Ejercicio(int idEjercicio, String nombre, String descripcion, String grupoMuscular,
                     int series, int repeticiones, int duracionMinutos, String nivel, String recursoUrl) {
        this.idEjercicio = idEjercicio;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.grupoMuscular = grupoMuscular;
        this.series = series;
        this.repeticiones = repeticiones;
        this.duracionMinutos = duracionMinutos;
        this.nivel = nivel;
        this.recursoUrl = recursoUrl;
    }

    public int getIdEjercicio() { return idEjercicio; }
    public void setIdEjercicio(int idEjercicio) { this.idEjercicio = idEjercicio; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getGrupoMuscular() { return grupoMuscular; }
    public void setGrupoMuscular(String grupoMuscular) { this.grupoMuscular = grupoMuscular; }

    public int getSeries() { return series; }
    public void setSeries(int series) { this.series = series; }

    public int getRepeticiones() { return repeticiones; }
    public void setRepeticiones(int repeticiones) { this.repeticiones = repeticiones; }

    public int getDuracionMinutos() { return duracionMinutos; }
    public void setDuracionMinutos(int duracionMinutos) { this.duracionMinutos = duracionMinutos; }

    public String getNivel() { return nivel; }
    public void setNivel(String nivel) { this.nivel = nivel; }

    public String getRecursoUrl() { return recursoUrl; }
    public void setRecursoUrl(String recursoUrl) { this.recursoUrl = recursoUrl; }

    @Override
    public String toString() {
        return "Ejercicio{" +
                "idEjercicio=" + idEjercicio +
                ", nombre='" + nombre + '\'' +
                ", grupoMuscular='" + grupoMuscular + '\'' +
                ", series=" + series +
                ", repeticiones=" + repeticiones +
                ", duracionMinutos=" + duracionMinutos +
                ", nivel='" + nivel + '\'' +
                '}';
    }
}

