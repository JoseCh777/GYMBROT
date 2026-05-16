package org.gymbrot.model;


import java.time.LocalDate;

public class Administrador extends Usuario {

    private String rol;

    public Administrador() {
        super();
    }

    public Administrador(String numeroIdentificacion, String tipoIdentificacion, String nombre,
                         String apellidos, String telefono, String correo, String contrasenaHash,
                         String fotoUrl, String estado, LocalDate fechaRegistro, String tipoUsuario,
                         String rol) {
        super(numeroIdentificacion, tipoIdentificacion, nombre, apellidos, telefono, correo,
                contrasenaHash, fotoUrl, estado, fechaRegistro, tipoUsuario);
        this.rol = rol;
    }

    public String getRol() { return rol; }
    public void setRol(String rol) { this.rol = rol; }

    @Override
    public String toString() {
        return "Administrador{" +
                "numeroIdentificacion='" + getNumeroIdentificacion() + '\'' +
                ", nombre='" + getNombre() + '\'' +
                ", apellidos='" + getApellidos() + '\'' +
                ", correo='" + getCorreo() + '\'' +
                ", rol='" + rol + '\'' +
                '}';
    }
}
