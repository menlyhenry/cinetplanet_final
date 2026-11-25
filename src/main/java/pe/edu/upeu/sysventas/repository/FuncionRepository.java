package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.Funcion;
import pe.edu.upeu.sysventas.model.Pelicula;
import pe.edu.upeu.sysventas.model.Sala;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class FuncionRepository extends AbstractJdbcRepository<Funcion, Long> {
    @Override protected String tabla() { return "Funcion"; }
    @Override protected String columnaId() { return "idFuncion"; }
    @Override protected String[] columnas() { return new String[]{"fecha", "horaInicio", "precioEntrada", "idSala", "idPelicula"}; }
    @Override protected String seleccion() { return "SELECT " + JdbcMapeo.SEL_FUNCION + " FROM " + JdbcMapeo.FROM_FUNCION; }
    @Override protected String alias() { return "f"; }
    @Override protected Funcion mapear(ResultSet rs) throws SQLException { return JdbcMapeo.funcion(rs, 1); }

    @Override
    protected void asignar(PreparedStatement ps, Funcion f, int i) throws SQLException {
        JdbcMapeo.poner(ps, i, f.getFecha());
        JdbcMapeo.poner(ps, i + 1, f.getHoraInicio());
        ps.setDouble(i + 2, f.getPrecioEntrada());
        ps.setLong(i + 3, f.getSala().getIdSala());
        ps.setLong(i + 4, f.getPelicula().getIdPelicula());
    }

    @Override protected Long getId(Funcion entity) { return entity.getIdFuncion(); }
    @Override protected void setId(Funcion entity, Long id) { entity.setIdFuncion(id); }
    @Override protected Long generateId() { return siguienteId(); }

    public void seedData(List<Sala> salas, List<Pelicula> peliculas) {
    }
}
