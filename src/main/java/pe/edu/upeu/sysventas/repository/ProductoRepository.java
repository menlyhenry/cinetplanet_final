package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.model.Producto;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProductoRepository extends AbstractJdbcRepository<Producto, Long> {
    @Override protected String tabla() { return "Producto"; }
    @Override protected String columnaId() { return "idProducto"; }
    @Override protected String[] columnas() { return new String[]{"nombre", "precio"}; }
    @Override protected String seleccion() { return "SELECT p.idProducto, p.nombre, p.precio FROM Producto p"; }
    @Override protected String alias() { return "p"; }

    @Override
    protected Producto mapear(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getLong(1));
        p.setNombre(rs.getString(2));
        p.setPrecio(rs.getDouble(3));
        return p;
    }

    @Override
    protected void asignar(PreparedStatement ps, Producto p, int i) throws SQLException {
        ps.setString(i, p.getNombre());
        ps.setDouble(i + 1, p.getPrecio());
    }

    @Override protected Long getId(Producto entity) { return entity.getIdProducto(); }
    @Override protected void setId(Producto entity, Long id) { entity.setIdProducto(id); }
    @Override protected Long generateId() { return siguienteId(); }

    public void seedData() {
    }
}
