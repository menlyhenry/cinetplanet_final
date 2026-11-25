package pe.edu.upeu.sysventas.util;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import pe.edu.upeu.sysventas.dto.PersonaDto;

import java.io.IOException;

public class ConsultaDNI {
    private static final String URL = "https://eldni.com/pe/buscar-datos-por-dni";
    private static final String AGENTE = "Mozilla/5.0";
    private static final int TIEMPO_MAXIMO = 10000;

    public PersonaDto consultar(String dni) {
        try {
            Connection.Response get = Jsoup.connect(URL)
                    .userAgent(AGENTE)
                    .timeout(TIEMPO_MAXIMO)
                    .method(Connection.Method.GET)
                    .execute();
            Document pagina = get.parse();
            String token = pagina.select("input[name=_token]").attr("value");

            Connection.Response post = Jsoup.connect(URL)
                    .userAgent(AGENTE)
                    .timeout(TIEMPO_MAXIMO)
                    .cookies(get.cookies())
                    .data("_token", token)
                    .data("dni", dni)
                    .method(Connection.Method.POST)
                    .ignoreContentType(true)
                    .execute();

            Document resultado = Jsoup.parse(post.body());
            Element fila = resultado.select("table tbody tr").first();
            if (fila == null) {
                return null;
            }
            Elements celdas = fila.select("td");
            if (celdas.size() < 4) {
                return null;
            }
            PersonaDto persona = new PersonaDto();
            persona.setDni(celdas.get(0).text());
            persona.setNombre(celdas.get(1).text());
            persona.setApellidoPaterno(celdas.get(2).text());
            persona.setApellidoMaterno(celdas.get(3).text());
            return persona;
        } catch (IOException e) {
            throw new RuntimeException("No se pudo consultar el DNI en la web. Revisa tu conexión a internet.", e);
        }
    }
}
