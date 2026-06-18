package org.gymbrot.model;

import java.time.LocalDate;

public class Usuario {

    private String numeroIdentificacion;
    private String tipoIdentificacion;
    private String nombre;
    private String apellidos;
    private String telefono;
    private String correo;
    private String contrasenaHash;
    private String fotoUrl;
    private String estado;
    private LocalDate fechaRegistro;
    private String tipoUsuario;

    public Usuario() {}

    public Usuario(String numeroIdentificacion, String tipoIdentificacion, String nombre,
                   String apellidos, String telefono, String correo, String contrasenaHash,
                   String fotoUrl, String estado, LocalDate fechaRegistro, String tipoUsuario) {
        this.numeroIdentificacion = numeroIdentificacion;
        this.tipoIdentificacion = tipoIdentificacion;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.telefono = telefono;
        this.correo = correo;
        this.contrasenaHash = contrasenaHash;
        this.fotoUrl = fotoUrl;
        this.estado = estado;
        this.fechaRegistro = fechaRegistro;
        this.tipoUsuario = tipoUsuario;
    }

    public String getNumeroIdentificacion() { return numeroIdentificacion; }
    public void setNumeroIdentificacion(String numeroIdentificacion) { this.numeroIdentificacion = numeroIdentificacion; }

    public String getTipoIdentificacion() { return tipoIdentificacion; }
    public void setTipoIdentificacion(String tipoIdentificacion) { this.tipoIdentificacion = tipoIdentificacion; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getCorreo() { return correo; }
    public void setCorreo(String correo) { this.correo = correo; }

    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public LocalDate getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDate fechaRegistro) { this.fechaRegistro = fechaRegistro; }

    public String getTipoUsuario() { return tipoUsuario; }
    public void setTipoUsuario(String tipoUsuario) { this.tipoUsuario = tipoUsuario; }

    @Override
    public String toString() {
        return "Usuario{" +
                "numeroIdentificacion='" + numeroIdentificacion + '\'' +
                ", nombre='" + nombre + '\'' +
                ", apellidos='" + apellidos + '\'' +
                ", correo='" + correo + '\'' +
                ", estado='" + estado + '\'' +
                ", tipoUsuario='" + tipoUsuario + '\'' +
                '}';
    }
}
