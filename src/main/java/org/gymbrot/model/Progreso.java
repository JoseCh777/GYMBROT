package org.gymbrot.model;


import java.time.LocalDate;

public class Progreso {

    private int idProgreso;
    private String idCliente;
    private LocalDate fechaRegistro;
    private double peso;
    private double altura;
    private double imc;
    private double porcentajeGrasa;
    private double masaMuscular;
    private String objetivo;
    private String observaciones;

    public Progreso() {}

    public Progreso(int idProgreso, String idCliente, LocalDate fechaRegistro, double peso,
                    double altura, double imc, double porcentajeGrasa, double masaMuscular,
                    String objetivo, String observaciones) {
        this.idProgreso = idProgreso;
        this.idCliente = idCliente;
        this.fechaRegistro = fechaRegistro;
        this.peso = peso;
        this.altura = altura;
        this.imc = imc;
        this.porcentajeGrasa = porcentajeGrasa;
        this.masaMuscular = masaMuscular;
        this.objetivo = objetivo;
        this.observaciones = observaciones;
    }

    public int getIdProgreso() { return idProgreso; }
    public void setIdProgreso(int idProgreso) { this.idProgreso = idProgreso; }

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public double getPeso() { return peso; }
    public void setPeso(double peso) { this.peso = peso; }

    public double getAltura() { return altura; }
    public void setAltura(double altura) { this.altura = altura; }

    public double getImc() { return imc; }
    public void setImc(double imc) { this.imc = imc; }

    public double getPorcentajeGrasa() { return porcentajeGrasa; }
    public void setPorcentajeGrasa(double porcentajeGrasa) { this.porcentajeGrasa = porcentajeGrasa; }

    public double getMasaMuscular() { return masaMuscular; }
    public void setMasaMuscular(double masaMuscular) { this.masaMuscular = masaMuscular; }

    public String getObjetivo() { return objetivo; }
    public void setObjetivo(String objetivo) { this.objetivo = objetivo; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    @Override
    public String toString() {
        return "Progreso{" +
                "idProgreso=" + idProgreso +
                ", idCliente='" + idCliente + '\'' +
                ", fechaRegistro=" + fechaRegistro +
                ", peso=" + peso +
                ", altura=" + altura +
                ", imc=" + imc +
                ", porcentajeGrasa=" + porcentajeGrasa +
                ", masaMuscular=" + masaMuscular +
                ", objetivo='" + objetivo + '\'' +
                '}';
    }
}
