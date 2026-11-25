package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.Sala;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SalaRepository extends AbstractJdbcRepository<Sala, Long> {
    @Override protected String tabla() { return "Sala"; }
    @Override protected String columnaId() { return "idSala"; }
    @Override protected String[] columnas() { return new String[]{"numero", "capacidad", "tipo"}; }
    @Override protected String seleccion() { return "SELECT " + JdbcMapeo.SEL_SALA + " FROM Sala s"; }
    @Override protected String alias() { return "s"; }
    @Override protected Sala mapear(ResultSet rs) throws SQLException { return JdbcMapeo.sala(rs, 1); }

    @Override
    protected void asignar(PreparedStatement ps, Sala s, int i) throws SQLException {
        ps.setInt(i, s.getNumero());
        ps.setInt(i + 1, s.getCapacidad());
        ps.setString(i + 2, s.getTipo());
    }

    @Override protected Long getId(Sala entity) { return entity.getIdSala(); }
    @Override protected void setId(Sala entity, Long id) { entity.setIdSala(id); }
    @Override protected Long generateId() { return siguienteId(); }

    public void seedData() {
    }
}
