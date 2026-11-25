package pe.edu.upeu.sysventas.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {
    private Long idUsuario;
    @NotBlank(message = "El usuario es obligatorio")
    private String usuario;
    private String clave;
    private Perfil idPerfil;
    private String estado;

    public String getPerfilNombre() {
        return idPerfil == null ? "" : idPerfil.getNombre();
    }
}
