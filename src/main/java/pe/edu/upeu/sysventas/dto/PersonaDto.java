package pe.edu.upeu.sysventas.dto;

public class PersonaDto {
    private String dni;
    private String nombre;
    private String apellidoPaterno;
    private String apellidoMaterno;

    public String getDni() { return dni; }
    public void setDni(String dni) { this.dni = dni; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getApellidoPaterno() { return apellidoPaterno; }
    public void setApellidoPaterno(String apellidoPaterno) { this.apellidoPaterno = apellidoPaterno; }
    public String getApellidoMaterno() { return apellidoMaterno; }
    public void setApellidoMaterno(String apellidoMaterno) { this.apellidoMaterno = apellidoMaterno; }

    public String getNombreCompleto() {
        return (nombre + " " + apellidoPaterno + " " + apellidoMaterno).trim().replaceAll("\\s+", " ");
    }
}
