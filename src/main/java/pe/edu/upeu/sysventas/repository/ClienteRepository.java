package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.Cliente;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ClienteRepository extends AbstractJdbcRepository<Cliente, String> {
    @Override protected String tabla() { return "Cliente"; }
    @Override protected String columnaId() { return "dniruc"; }
    @Override protected String[] columnas() { return new String[]{"nombres", "repLegal", "tipoDocumento"}; }
    @Override protected String seleccion() { return "SELECT " + JdbcMapeo.SEL_CLIENTE + " FROM Cliente c"; }
    @Override protected String alias() { return "c"; }
    @Override protected Cliente mapear(ResultSet rs) throws SQLException { return JdbcMapeo.cliente(rs, 1); }

    @Override
    protected void asignar(PreparedStatement ps, Cliente c, int i) throws SQLException {
        ps.setString(i, c.getNombres());
        String rep = c.getRepLegal();
        ps.setString(i + 1, rep == null || rep.isBlank() ? null : rep);
        ps.setString(i + 2, c.getTipoDocumento().name());
    }

    @Override protected String getId(Cliente entity) { return entity.getDniruc(); }
    @Override protected void setId(Cliente entity, String id) { entity.setDniruc(id); }
    @Override protected String generateId() { return null; }

    public void seedData() {
    }
}
