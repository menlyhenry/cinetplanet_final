package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.enums.TipoPago;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.model.Venta;
import pe.edu.upeu.sysventas.model.VentaDetalle;
import pe.edu.upeu.sysventas.config.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class VentaRepository extends AbstractJdbcRepository<Venta, Long> {
    private long sequenceDetalle = -1;

    @Override protected String tabla() { return "Venta"; }
    @Override protected String columnaId() { return "idVenta"; }

    @Override
    protected String[] columnas() {
        return new String[]{"fechaVenta", "cantidadEntradas", "total", "tipoPago", "dniruc", "idFuncion", "id_usuario"};
    }

    @Override
    protected String seleccion() {
        return "SELECT v.idVenta, v.fechaVenta, v.cantidadEntradas, v.total, v.tipoPago, "
                + JdbcMapeo.SEL_CLIENTE + ", " + JdbcMapeo.SEL_FUNCION + ", " + JdbcMapeo.SEL_USUARIO
                + " FROM Venta v"
                + " JOIN Cliente c ON c.dniruc = v.dniruc"
                + " JOIN Funcion f ON f.idFuncion = v.idFuncion"
                + " JOIN Sala s ON s.idSala = f.idSala"
                + " JOIN Pelicula p ON p.idPelicula = f.idPelicula"
                + " JOIN Usuario u ON u.id_usuario = v.id_usuario"
                + " JOIN Perfil pf ON pf.id_perfil = u.id_perfil";
    }

    @Override protected String alias() { return "v"; }

    @Override
    protected Venta mapear(ResultSet rs) throws SQLException {
        Venta v = new Venta();
        v.setIdVenta(rs.getLong(1));
        v.setFechaVenta(JdbcMapeo.fechaHora(rs.getString(2)));
        v.setCantidadEntradas(rs.getInt(3));
        v.setTotal(rs.getDouble(4));
        String pago = rs.getString(5);
        v.setTipoPago(pago == null ? null : TipoPago.valueOf(pago));
        v.setCliente(JdbcMapeo.cliente(rs, 6));
        v.setFuncion(JdbcMapeo.funcion(rs, 10));
        v.setUsuario(JdbcMapeo.usuario(rs, 22));
        v.setDetalles(new ArrayList<>());
        v.setAsientos(new ArrayList<>());
        return v;
    }

    @Override
    protected void asignar(PreparedStatement ps, Venta v, int i) throws SQLException {
        JdbcMapeo.poner(ps, i, v.getFechaVenta());
        ps.setInt(i + 1, v.getCantidadEntradas());
        ps.setDouble(i + 2, v.getTotal());
        ps.setString(i + 3, v.getTipoPago().name());
        ps.setString(i + 4, v.getCliente().getDniruc());
        ps.setLong(i + 5, v.getFuncion().getIdFuncion());
        ps.setLong(i + 6, v.getUsuario().getIdUsuario());
    }

    @Override
    protected void alCargar(Connection c, Venta venta) throws SQLException {
        String sql = "SELECT d.idDetalle, d.cantidad, d.precioUnitario, p.idProducto, p.nombre, p.precio"
                + " FROM DetalleVenta d JOIN Producto p ON p.idProducto = d.idProducto"
                + " WHERE d.idVenta = ? ORDER BY d.idDetalle";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, venta.getIdVenta());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Producto producto = new Producto();
                    producto.setIdProducto(rs.getLong(4));
                    producto.setNombre(rs.getString(5));
                    producto.setPrecio(rs.getDouble(6));
                    VentaDetalle d = new VentaDetalle();
                    d.setIdDetalle(rs.getLong(1));
                    d.setCantidad(rs.getInt(2));
                    d.setPrecioUnitario(rs.getDouble(3));
                    d.setProducto(producto);
                    venta.getDetalles().add(d);
                }
            }
        }
        try (PreparedStatement ps = c.prepareStatement("SELECT asiento FROM AsientoVenta WHERE idVenta = ? ORDER BY idAsiento")) {
            ps.setLong(1, venta.getIdVenta());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    venta.getAsientos().add(rs.getString(1));
                }
            }
        }
    }

    @Override
    protected void alGuardar(Connection c, Venta venta) throws SQLException {
        if (venta.getDetalles() != null) {
            String sql = "INSERT INTO DetalleVenta (idDetalle, cantidad, precioUnitario, idVenta, idProducto) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(sql)) {
                for (VentaDetalle d : venta.getDetalles()) {
                    ps.setLong(1, d.getIdDetalle());
                    ps.setInt(2, d.getCantidad());
                    ps.setDouble(3, d.getPrecioUnitario());
                    ps.setLong(4, venta.getIdVenta());
                    ps.setLong(5, d.getProducto().getIdProducto());
                    ps.executeUpdate();
                }
            }
        }
        guardarAsientos(c, venta);
    }

    private void guardarAsientos(Connection c, Venta venta) throws SQLException {
        if (venta.getAsientos() == null || venta.getAsientos().isEmpty()) {
            return;
        }
        long idAsiento;
        try (PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(idAsiento), 0) + 1 FROM AsientoVenta");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            idAsiento = rs.getLong(1);
        }
        String sql = "INSERT INTO AsientoVenta (idAsiento, idVenta, idFuncion, asiento) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = c.prepareStatement(sql)) {
            for (String asiento : venta.getAsientos()) {
                ps.setLong(1, idAsiento++);
                ps.setLong(2, venta.getIdVenta());
                ps.setLong(3, venta.getFuncion().getIdFuncion());
                ps.setString(4, asiento);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            String mensaje = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
            if (mensaje.contains("unique") || mensaje.contains("duplicate")) {
                throw new IllegalStateException("Uno de los asientos ya fue ocupado por otra venta. Elige otros asientos.");
            }
            throw e;
        }
    }

    public List<String> asientosOcupados(Long idFuncion) {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement("SELECT asiento FROM AsientoVenta WHERE idFuncion = ? ORDER BY idAsiento")) {
            ps.setLong(1, idFuncion);
            List<String> ocupados = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ocupados.add(rs.getString(1));
                }
            }
            return ocupados;
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    @Override protected Long getId(Venta entity) { return entity.getIdVenta(); }
    @Override protected void setId(Venta entity, Long id) { entity.setIdVenta(id); }
    @Override protected Long generateId() { return siguienteId(); }

    public synchronized long siguienteIdDetalle() {
        if (sequenceDetalle < 0) {
            try (Connection c = Database.conectar();
                 PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(idDetalle), 0) + 1 FROM DetalleVenta");
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                sequenceDetalle = rs.getLong(1);
            } catch (SQLException e) {
                throw traducir(e);
            }
        }
        return sequenceDetalle++;
    }
}
