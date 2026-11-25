package pe.edu.upeu.sysventas.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.dto.SessionManager;
import pe.edu.upeu.sysventas.model.Venta;
import pe.edu.upeu.sysventas.service.IClienteService;
import pe.edu.upeu.sysventas.service.IPeliculaService;
import pe.edu.upeu.sysventas.service.IVentaService;

import java.util.List;

@RequiredArgsConstructor
public class InicioController {
    private final IVentaService ventaService;
    private final IClienteService clienteService;
    private final IPeliculaService peliculaService;

    @FXML
    private Label lblBienvenida;
    @FXML
    private Label lblFecha;
    @FXML
    private Label lblVentas;
    @FXML
    private Label lblIngresos;
    @FXML
    private Label lblClientes;
    @FXML
    private Label lblPeliculas;
    @FXML
    private PieChart graficoPagos;
    @FXML
    private VBox listaUltimas;

    @FXML
    public void initialize() {
        graficoPagos.setAnimated(false);
        graficoPagos.setLabelsVisible(false);
        actualizar();
    }

    @FXML
    public void actualizar() {
        String nombre = SessionManager.getInstance().getUserName();
        if (nombre == null) {
            nombre = "";
        }
        lblBienvenida.setText("Hola " + nombre);
        lblFecha.setText(java.time.LocalDate.now().toString());

        List<Venta> ventas;
        try {
            ventas = ventaService.findAll();
        } catch (RuntimeException e) {
            listaUltimas.getChildren().clear();
            listaUltimas.getChildren().add(new Label("No se pudo cargar los datos"));
            return;
        }

        double ingresos = 0;
        int efectivo = 0;
        int yape = 0;
        int tarjeta = 0;
        for (Venta v : ventas) {
            if (v.getTotal() != null) {
                ingresos = ingresos + v.getTotal();
            }
            String pago = v.getTipoPagoTexto();
            if (pago.equals("Efectivo")) {
                efectivo++;
            } else if (pago.equals("Yape")) {
                yape++;
            } else if (pago.equals("Tarjeta")) {
                tarjeta++;
            }
        }

        lblVentas.setText(String.valueOf(ventas.size()));
        lblIngresos.setText("S/ " + String.format("%.2f", ingresos).replace(',', '.'));
        lblClientes.setText(String.valueOf(clienteService.findAll().size()));
        lblPeliculas.setText(String.valueOf(peliculaService.findAll().size()));

        graficoPagos.setData(FXCollections.observableArrayList(
                new PieChart.Data("Efectivo (" + efectivo + ")", efectivo),
                new PieChart.Data("Yape (" + yape + ")", yape),
                new PieChart.Data("Tarjeta (" + tarjeta + ")", tarjeta)));

        listaUltimas.getChildren().clear();
        int mostradas = 0;
        for (int i = ventas.size() - 1; i >= 0 && mostradas < 6; i--) {
            listaUltimas.getChildren().add(crearFila(ventas.get(i)));
            mostradas++;
        }
        if (mostradas == 0) {
            listaUltimas.getChildren().add(new Label("Todavia no hay ventas"));
        }
    }

    private Label crearFila(Venta v) {
        String texto = "N° " + v.getIdVenta() + " - " + v.getClienteNombre() + " - " + v.getTipoPagoTexto()
                + " - S/ " + String.format("%.2f", v.getTotal()).replace(',', '.');
        return new Label(texto);
    }
}
