package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.config.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractJdbcRepository<T, ID> implements ICrudGenericoRepository<T, ID> {
    protected abstract String tabla();
    protected abstract String columnaId();

    protected abstract String[] columnas();

    protected abstract String seleccion();

    protected abstract String alias();
    protected abstract T mapear(ResultSet rs) throws SQLException;

    protected abstract void asignar(PreparedStatement ps, T entity, int desde) throws SQLException;
    protected abstract ID getId(T entity);
    protected abstract void setId(T entity, ID id);
    protected abstract ID generateId();

    protected void alCargar(Connection c, T entity) throws SQLException {
    }

    protected void alGuardar(Connection c, T entity) throws SQLException {
    }

    protected Long siguienteId() {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(" + columnaId() + "), 0) + 1 FROM " + tabla());
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    protected List<T> consultar(String where, Object... parametros) {
        String sql = seleccion() + " " + where + " ORDER BY " + alias() + "." + columnaId();
        try (Connection c = Database.conectar(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                ps.setObject(i + 1, parametros[i]);
            }
            List<T> resultado = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapear(rs));
                }
            }
            for (T entity : resultado) {
                alCargar(c, entity);
            }
            return resultado;
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    @Override
    public T save(T entity) {
        if (getId(entity) == null) {
            ID id = generateId();
            if (id == null) {
                throw new IllegalArgumentException("El ID es obligatorio");
            }
            setId(entity, id);
        }
        String[] cols = columnas();
        StringBuilder sql = new StringBuilder("INSERT INTO ").append(tabla()).append(" (").append(columnaId());
        StringBuilder marcas = new StringBuilder("?");
        for (String col : cols) {
            sql.append(", ").append(col);
            marcas.append(", ?");
        }
        sql.append(") VALUES (").append(marcas).append(")");
        try (Connection c = Database.conectar()) {
            c.setAutoCommit(false);
            try (PreparedStatement ps = c.prepareStatement(sql.toString())) {
                if (existe(c, getId(entity))) {
                    throw new IllegalArgumentException("Ya existe un registro con el ID: " + getId(entity));
                }
                ps.setObject(1, getId(entity));
                asignar(ps, entity, 2);
                ps.executeUpdate();
                alGuardar(c, entity);
                c.commit();
                return entity;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    @Override
    public T update(ID id, T entity) {
        if (id == null) {
            throw new IllegalArgumentException("El ID es obligatorio para actualizar");
        }
        setId(entity, id);
        StringBuilder sql = new StringBuilder("UPDATE ").append(tabla()).append(" SET ");
        String[] cols = columnas();
        for (int i = 0; i < cols.length; i++) {
            sql.append(i == 0 ? "" : ", ").append(cols[i]).append(" = ?");
        }
        sql.append(" WHERE ").append(columnaId()).append(" = ?");
        try (Connection c = Database.conectar(); PreparedStatement ps = c.prepareStatement(sql.toString())) {
            asignar(ps, entity, 1);
            ps.setObject(cols.length + 1, id);
            if (ps.executeUpdate() == 0) {
                throw new RuntimeException("No se encontro la entidad con ID: " + id);
            }
            return entity;
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    @Override
    public Optional<T> findById(ID id) {
        List<T> lista = consultar("WHERE " + alias() + "." + columnaId() + " = ?", id);
        if (lista.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(lista.get(0));
    }

    @Override
    public List<T> findAll() {
        return consultar("");
    }

    @Override
    public void deleteById(ID id) {
        try (Connection c = Database.conectar();
             PreparedStatement ps = c.prepareStatement("DELETE FROM " + tabla() + " WHERE " + columnaId() + " = ?")) {
            ps.setObject(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    @Override
    public boolean existsById(ID id) {
        try (Connection c = Database.conectar()) {
            return existe(c, id);
        } catch (SQLException e) {
            throw traducir(e);
        }
    }

    private boolean existe(Connection c, ID id) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM " + tabla() + " WHERE " + columnaId() + " = ?")) {
            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    protected RuntimeException traducir(SQLException e) {
        String mensaje = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        if (mensaje.contains("foreign key")) {
            return new RuntimeException("No se puede completar la operación: el registro está relacionado con otros datos.", e);
        }
        if (mensaje.contains("unique")) {
            return new RuntimeException("Ya existe un registro con ese valor (dato duplicado).", e);
        }
        return new RuntimeException("Error de base de datos: " + e.getMessage(), e);
    }
}
