package pe.edu.upeu.sysventas.repository;

import pe.edu.upeu.sysventas.enums.TipoDocumento;
import pe.edu.upeu.sysventas.model.Cliente;
import pe.edu.upeu.sysventas.model.Pelicula;
import pe.edu.upeu.sysventas.model.Perfil;
import pe.edu.upeu.sysventas.model.Sala;
import pe.edu.upeu.sysventas.model.Usuario;
import pe.edu.upeu.sysventas.model.Funcion;

import pe.edu.upeu.sysventas.config.Database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

final class JdbcMapeo {
    static final String SEL_SALA = "s.idSala, s.numero, s.capacidad, s.tipo";
    static final String SEL_PELICULA = "p.idPelicula, p.titulo, p.genero, p.duracion";
    static final String SEL_CLIENTE = "c.dniruc, c.nombres, c.repLegal, c.tipoDocumento";
    static final String SEL_PERFIL = "pf.id_perfil, pf.nombre, pf.codigo";
    static final String SEL_USUARIO = "u.id_usuario, u.usuario, u.clave, u.estado, " + SEL_PERFIL;
    static final String SEL_FUNCION = "f.idFuncion, f.fecha, f.horaInicio, f.precioEntrada, " + SEL_SALA + ", " + SEL_PELICULA;

    static final String FROM_FUNCION = "Funcion f JOIN Sala s ON s.idSala = f.idSala JOIN Pelicula p ON p.idPelicula = f.idPelicula";
    static final String FROM_USUARIO = "Usuario u JOIN Perfil pf ON pf.id_perfil = u.id_perfil";

    private JdbcMapeo() {
    }

    static Sala sala(ResultSet rs, int i) throws SQLException {
        Sala s = new Sala();
        s.setIdSala(rs.getLong(i));
        s.setNumero(rs.getInt(i + 1));
        s.setCapacidad(rs.getInt(i + 2));
        s.setTipo(rs.getString(i + 3));
        return s;
    }

    static Pelicula pelicula(ResultSet rs, int i) throws SQLException {
        Pelicula p = new Pelicula();
        p.setIdPelicula(rs.getLong(i));
        p.setTitulo(rs.getString(i + 1));
        p.setGenero(rs.getString(i + 2));
        p.setDuracion(rs.getInt(i + 3));
        return p;
    }

    static Cliente cliente(ResultSet rs, int i) throws SQLException {
        Cliente c = new Cliente();
        c.setDniruc(rs.getString(i));
        c.setNombres(rs.getString(i + 1));
        c.setRepLegal(rs.getString(i + 2));
        c.setTipoDocumento(TipoDocumento.valueOf(rs.getString(i + 3)));
        return c;
    }

    static Perfil perfil(ResultSet rs, int i) throws SQLException {
        Perfil p = new Perfil();
        p.setIdPerfil(rs.getLong(i));
        p.setNombre(rs.getString(i + 1));
        p.setCodigo(rs.getString(i + 2));
        return p;
    }

    static Usuario usuario(ResultSet rs, int i) throws SQLException {
        Usuario u = new Usuario();
        u.setIdUsuario(rs.getLong(i));
        u.setUsuario(rs.getString(i + 1));
        u.setClave(rs.getString(i + 2));
        u.setEstado(rs.getString(i + 3));
        u.setIdPerfil(perfil(rs, i + 4));
        return u;
    }

    static Funcion funcion(ResultSet rs, int i) throws SQLException {
        Funcion f = new Funcion();
        f.setIdFuncion(rs.getLong(i));
        f.setFecha(LocalDate.parse(rs.getString(i + 1)));
        f.setHoraInicio(LocalTime.parse(rs.getString(i + 2)));
        f.setPrecioEntrada(rs.getDouble(i + 3));
        f.setSala(sala(rs, i + 4));
        f.setPelicula(pelicula(rs, i + 8));
        return f;
    }

    static LocalDateTime fechaHora(String texto) {
        return LocalDateTime.parse(texto.trim().replace(' ', 'T'));
    }

    static void poner(PreparedStatement ps, int i, LocalDate fecha) throws SQLException {
        if (Database.esSqlite()) ps.setString(i, fecha.toString());
        else ps.setObject(i, fecha);
    }

    static void poner(PreparedStatement ps, int i, LocalTime hora) throws SQLException {
        if (Database.esSqlite()) ps.setString(i, hora.toString());
        else ps.setObject(i, hora);
    }

    static void poner(PreparedStatement ps, int i, LocalDateTime fechaHora) throws SQLException {
        LocalDateTime f = fechaHora.withNano(0);
        if (Database.esSqlite()) ps.setString(i, f.toString().replace('T', ' '));
        else ps.setObject(i, f);
    }
}
