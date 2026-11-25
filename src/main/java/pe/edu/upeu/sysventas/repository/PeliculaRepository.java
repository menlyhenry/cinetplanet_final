package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.Pelicula;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class PeliculaRepository extends AbstractJdbcRepository<Pelicula, Long> {
    @Override protected String tabla() { return "Pelicula"; }
    @Override protected String columnaId() { return "idPelicula"; }
    @Override protected String[] columnas() { return new String[]{"titulo", "genero", "duracion"}; }
    @Override protected String seleccion() { return "SELECT " + JdbcMapeo.SEL_PELICULA + " FROM Pelicula p"; }
    @Override protected String alias() { return "p"; }
    @Override protected Pelicula mapear(ResultSet rs) throws SQLException { return JdbcMapeo.pelicula(rs, 1); }

    @Override
    protected void asignar(PreparedStatement ps, Pelicula p, int i) throws SQLException {
        ps.setString(i, p.getTitulo());
        ps.setString(i + 1, p.getGenero());
        ps.setInt(i + 2, p.getDuracion());
    }

    @Override protected Long getId(Pelicula entity) { return entity.getIdPelicula(); }
    @Override protected void setId(Pelicula entity, Long id) { entity.setIdPelicula(id); }
    @Override protected Long generateId() { return siguienteId(); }

    public void seedData() {
    }
}
