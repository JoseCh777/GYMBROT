package org.gymbrot.model;

public class RutinaEjercicio {

    private int idRutina;
    private int idEjercicio;
    private int orden;
    private String diaSemana;
    private String notasInstructor;

    public RutinaEjercicio() {}

    public RutinaEjercicio(int idRutina, int idEjercicio, int orden,
                           String diaSemana, String notasInstructor) {
        this.idRutina = idRutina;
        this.idEjercicio = idEjercicio;
        this.orden = orden;
        this.diaSemana = diaSemana;
        this.notasInstructor = notasInstructor;
    }

    public int getIdRutina() { return idRutina; }
    public void setIdRutina(int idRutina) { this.idRutina = idRutina; }

    public int getIdEjercicio() { return idEjercicio; }
    public void setIdEjercicio(int idEjercicio) { this.idEjercicio = idEjercicio; }

    public int getOrden() { return orden; }
    public void setOrden(int orden) { this.orden = orden; }

    public String getDiaSemana() { return diaSemana; }
    public void setDiaSemana(String diaSemana) { this.diaSemana = diaSemana; }

    public String getNotasInstructor() { return notasInstructor; }
    public void setNotasInstructor(String notasInstructor) { this.notasInstructor = notasInstructor; }

    @Override
    public String toString() {
        return "RutinaEjercicio{" +
                "idRutina=" + idRutina +
                ", idEjercicio=" + idEjercicio +
                ", orden=" + orden +
                ", diaSemana='" + diaSemana + '\'' +
                '}';
    }
}
