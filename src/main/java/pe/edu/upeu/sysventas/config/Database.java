package pe.edu.upeu.sysventas.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import java.lang.reflect.Proxy;

public final class Database {
    private static final String SCRIPT = "/db/cineplanet_script_unido.sql";
    private static final String CONFIG = "db.properties";
    private static Properties config;
    private static boolean iniciada;
    private static Connection compartida;
    private static long ultimoUso;

    private Database() {
    }

    public static synchronized boolean esSqlite() {
        return config != null && config.getProperty("db.url", "").startsWith("jdbc:sqlite:");
    }

    public static synchronized Connection conectar() throws SQLException {
        if (config == null) {
            config = cargarConfig();
        }
        if (!iniciada) {
            iniciar();
            iniciada = true;
        }
        long ahora = System.currentTimeMillis();
        if (compartida == null || compartida.isClosed() || (ahora - ultimoUso > 20000 && !compartida.isValid(2))) {
            compartida = abrir();
        }
        ultimoUso = ahora;
        final Connection real = compartida;
        return (Connection) Proxy.newProxyInstance(Database.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, metodo, args) -> {
                    if (metodo.getName().equals("close")) {
                        if (!real.getAutoCommit()) {
                            real.rollback();
                            real.setAutoCommit(true);
                        }
                        return null;
                    }
                    try {
                        return metodo.invoke(real, args);
                    } catch (java.lang.reflect.InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }

    private static Properties cargarConfig() throws SQLException {
        Properties p = new Properties();
        Path externo = Path.of(System.getProperty("user.dir"), CONFIG);
        try {
            if (Files.exists(externo)) {
                try (InputStream in = Files.newInputStream(externo)) {
                    p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            } else {
                try (InputStream in = Database.class.getResourceAsStream("/" + CONFIG)) {
                    if (in == null) {
                        throw new SQLException("No se encontró el archivo " + CONFIG);
                    }
                    p.load(new InputStreamReader(in, StandardCharsets.UTF_8));
                }
            }
        } catch (IOException e) {
            throw new SQLException("No se pudo leer " + CONFIG + ": " + e.getMessage(), e);
        }
        return p;
    }

    private static Connection abrir() throws SQLException {
        String url = config.getProperty("db.url");
        try {
            if (esSqlite()) {
                Connection conexion = DriverManager.getConnection(url);
                try (Statement st = conexion.createStatement()) {
                    st.execute("PRAGMA foreign_keys = ON");
                }
                return conexion;
            }
            return DriverManager.getConnection(url, config.getProperty("db.user"), config.getProperty("db.password"));
        } catch (SQLException e) {
            throw new SQLException("No se pudo conectar a la base de datos (" + url + "). Si usas PostgreSQL, revisa que el "
                    + "servidor esté encendido, que exista la base y que el usuario y la clave de db.properties sean correctos. "
                    + "Detalle: " + e.getMessage(), e);
        }
    }

    private static void iniciar() throws SQLException {
        String url = config.getProperty("db.url");
        if (esSqlite()) {
            String ruta = url.substring("jdbc:sqlite:".length());
            Path archivo = Path.of(ruta).toAbsolutePath();
            try {
                if (archivo.getParent() != null) {
                    Files.createDirectories(archivo.getParent());
                }
            } catch (IOException e) {
                throw new SQLException("No se pudo crear la carpeta de la base de datos: " + e.getMessage(), e);
            }
            System.out.println("Base de datos SQLite: " + archivo);
        } else {
            System.out.println("Base de datos: " + url);
        }
        String consulta = esSqlite()
                ? "SELECT 1 FROM sqlite_master WHERE type = 'table' AND lower(name) = 'usuario'"
                : "SELECT 1 FROM information_schema.tables WHERE table_schema = current_schema() AND table_name = 'usuario'";
        try (Connection conexion = abrir();
             Statement st = conexion.createStatement();
             ResultSet rs = st.executeQuery(consulta)) {
            if (!rs.next()) {
                ejecutarScript(conexion);
            }
        }
        try (Connection conexion = abrir()) {
            asegurarTablaAsientos(conexion);
        }
    }

    private static void asegurarTablaAsientos(Connection conexion) throws SQLException {
        try (Statement st = conexion.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS AsientoVenta ("
                    + " idAsiento BIGINT NOT NULL PRIMARY KEY,"
                    + " idVenta BIGINT NOT NULL,"
                    + " idFuncion BIGINT NOT NULL,"
                    + " asiento VARCHAR(10) NOT NULL,"
                    + " FOREIGN KEY (idVenta) REFERENCES Venta(idVenta),"
                    + " FOREIGN KEY (idFuncion) REFERENCES Funcion(idFuncion),"
                    + " UNIQUE (idFuncion, asiento))");
        }
    }

    private static void ejecutarScript(Connection conexion) throws SQLException {
        String texto;
        try (InputStream in = Database.class.getResourceAsStream(SCRIPT)) {
            if (in == null) {
                throw new SQLException("No se encontró el script " + SCRIPT);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                texto = reader.lines().filter(l -> !l.trim().startsWith("--")).collect(Collectors.joining("\n"));
            }
        } catch (IOException e) {
            throw new SQLException("No se pudo leer el script: " + e.getMessage(), e);
        }
        List<String> sentencias = new ArrayList<>();
        for (String parte : texto.split(";\\s*\\n")) {
            String sentencia = parte.trim();
            if (sentencia.endsWith(";")) {
                sentencia = sentencia.substring(0, sentencia.length() - 1).trim();
            }
            if (!sentencia.isEmpty()) {
                sentencias.add(sentencia);
            }
        }
        conexion.setAutoCommit(false);
        try (Statement st = conexion.createStatement()) {
            for (String sentencia : sentencias) {
                st.execute(sentencia);
            }
            conexion.commit();
        } catch (SQLException e) {
            conexion.rollback();
            throw e;
        } finally {
            conexion.setAutoCommit(true);
        }
    }
}
