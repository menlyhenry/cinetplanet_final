package pe.edu.upeu.sysventas.util;

import java.util.ArrayList;
import java.util.List;

public final class Asientos {
    public static final int COLUMNAS = 10;

    private Asientos() {
    }

    public static List<String> codigos(int capacidad) {
        List<String> codigos = new ArrayList<>();
        for (int i = 0; i < capacidad; i++) {
            codigos.add(nombreFila(i / COLUMNAS) + (i % COLUMNAS + 1));
        }
        return codigos;
    }

    private static String nombreFila(int fila) {
        StringBuilder sb = new StringBuilder();
        int n = fila;
        do {
            sb.insert(0, (char) ('A' + n % 26));
            n = n / 26 - 1;
        } while (n >= 0);
        return sb.toString();
    }
}
