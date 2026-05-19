package org.gymbrot.model;

import java.time.LocalDate;

public class Cliente extends Usuario {

    private String direccion;
    private LocalDate fechaNacimiento;
    private byte[] huellaDactilar;

    public Cliente() {
        super();
    }

    public Cliente(String numeroIdentificacion, String tipoIdentificacion, String nombre,
                   String apellidos, String telefono, String correo, String contrasenaHash,
                   String fotoUrl, String estado, LocalDate fechaRegistro, String tipoUsuario,
                   String direccion, LocalDate fechaNacimiento, byte[] huellaDactilar) {
        super(numeroIdentificacion, tipoIdentificacion, nombre, apellidos, telefono, correo,
                contrasenaHash, fotoUrl, estado, fechaRegistro, tipoUsuario);
        this.direccion = direccion;
        this.fechaNacimiento = fechaNacimiento;
        this.huellaDactilar = huellaDactilar;
    }

    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }

    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }

    public byte[] getHuellaDactilar() { return huellaDactilar; }
    public void setHuellaDactilar(byte[] huellaDactilar) { this.huellaDactilar = huellaDactilar; }

    @Override
    public String toString() {
        return "Cliente{" +
                "numeroIdentificacion='" + getNumeroIdentificacion() + '\'' +
                ", nombre='" + getNombre() + '\'' +
                ", apellidos='" + getApellidos() + '\'' +
                ", correo='" + getCorreo() + '\'' +
                ", direccion='" + direccion + '\'' +
                ", fechaNacimiento=" + fechaNacimiento +
                ", estado='" + getEstado() + '\'' +
                '}';
    }
}
