package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.Usuario;
import pe.edu.upeu.sysventas.util.PasswordUtil;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class UsuarioRepository extends AbstractJdbcRepository<Usuario, Long> {
    @Override protected String tabla() { return "Usuario"; }
    @Override protected String columnaId() { return "id_usuario"; }
    @Override protected String[] columnas() { return new String[]{"usuario", "clave", "id_perfil", "estado"}; }
    @Override protected String seleccion() { return "SELECT " + JdbcMapeo.SEL_USUARIO + " FROM " + JdbcMapeo.FROM_USUARIO; }
    @Override protected String alias() { return "u"; }
    @Override protected Usuario mapear(ResultSet rs) throws SQLException { return JdbcMapeo.usuario(rs, 1); }

    @Override
    protected void asignar(PreparedStatement ps, Usuario u, int i) throws SQLException {
        ps.setString(i, u.getUsuario());
        ps.setString(i + 1, u.getClave());
        ps.setLong(i + 2, u.getIdPerfil().getIdPerfil());
        ps.setString(i + 3, u.getEstado());
    }

    @Override protected Long getId(Usuario entity) { return entity.getIdUsuario(); }
    @Override protected void setId(Usuario entity, Long id) { entity.setIdUsuario(id); }
    @Override protected Long generateId() { return siguienteId(); }

    public void seedData() {
    }

    public Optional<Usuario> findByUsuarioAndClave(String usuario, String clave) {
        for (Usuario u : consultar("WHERE u.usuario = ?", usuario)) {
            if (PasswordUtil.verificar(clave, u.getClave()) && "ACTIVO".equals(u.getEstado())) {
                return Optional.of(u);
            }
        }
        return Optional.empty();
    }
}
