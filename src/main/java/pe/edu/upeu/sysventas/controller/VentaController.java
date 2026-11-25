package pe.edu.upeu.sysventas.controller;

import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.components.Toast;
import pe.edu.upeu.sysventas.dto.PersonaDto;
import pe.edu.upeu.sysventas.dto.SessionManager;
import pe.edu.upeu.sysventas.enums.TipoDocumento;
import pe.edu.upeu.sysventas.enums.TipoPago;
import pe.edu.upeu.sysventas.model.CarritoVenta;
import pe.edu.upeu.sysventas.model.Cliente;
import pe.edu.upeu.sysventas.model.Funcion;
import pe.edu.upeu.sysventas.model.ItemCarrito;
import pe.edu.upeu.sysventas.model.Producto;
import pe.edu.upeu.sysventas.model.Usuario;
import pe.edu.upeu.sysventas.model.Venta;
import pe.edu.upeu.sysventas.model.VentaDetalle;
import pe.edu.upeu.sysventas.service.IClienteService;
import pe.edu.upeu.sysventas.service.IFuncionService;
import pe.edu.upeu.sysventas.service.IProductoService;
import pe.edu.upeu.sysventas.service.IUsuarioService;
import pe.edu.upeu.sysventas.service.IVentaService;
import pe.edu.upeu.sysventas.util.Asientos;
import pe.edu.upeu.sysventas.util.ConsultaDNI;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@RequiredArgsConstructor
public class VentaController {

    private final IClienteService clienteService;
    private final IFuncionService funcionService;
    private final IProductoService productoService;
    private final IVentaService ventaService;
    private final IUsuarioService usuarioService;

    @FXML
    private AnchorPane miContenedor;
    @FXML
    private ComboBox<Funcion> cbFuncion;
    @FXML
    private TextField txtEntradas;
    @FXML
    private ComboBox<TipoPago> cbTipoPago;
    @FXML
    private ComboBox<Producto> cbProducto;
    @FXML
    private TextField txtCantidad;
    @FXML
    private TableView<ItemCarrito> tablaCarrito;
    @FXML
    private TableView<Venta> tablaVentas;
    @FXML
    private Label lblSubEntradas;
    @FXML
    private Label lblSubProductos;
    @FXML
    private Label lblTotal;
    @FXML
    private Label lblBase;
    @FXML
    private Label lblIgv;
    @FXML
    private TextField txtCodigo;
    @FXML
    private TextField txtPrecioUnit;
    @FXML
    private TextField txtPrecioTotal;
    @FXML
    private Label lblMsg;
    @FXML
    private Label lblAsientos;
    @FXML
    private TextField txtDocBuscar;
    @FXML
    private Button btnBuscarDoc;
    @FXML
    private TextField txtNombreCliente;
    @FXML
    private Button btnRegCliente;

    private Cliente clienteActual;
    private final CarritoVenta carrito = new CarritoVenta();
    private final List<String> asientosElegidos = new ArrayList<>();

    @FXML
    public void initialize() {
        cbFuncion.setConverter(new StringConverter<Funcion>() {
            @Override
            public String toString(Funcion f) {
                return f == null ? "" : f.getEtiqueta();
            }

            @Override
            public Funcion fromString(String texto) {
                return null;
            }
        });
        cbProducto.setConverter(new StringConverter<Producto>() {
            @Override
            public String toString(Producto p) {
                return p == null ? "" : p.getNombre() + " - S/ " + formato(p.getPrecio());
            }

            @Override
            public Producto fromString(String texto) {
                return null;
            }
        });

        refrescarCombo(cbFuncion, funcionService.findAll());
        refrescarCombo(cbProducto, productoService.findAll());

        btnRegCliente.setDisable(true);
        cbTipoPago.setItems(FXCollections.observableArrayList(TipoPago.values()));

        txtEntradas.setText("1");
        txtCantidad.setText("1");
        txtEntradas.textProperty().addListener((obs, anterior, nuevo) -> {
            limpiarAsientos();
            actualizarTotales();
        });
        cbFuncion.valueProperty().addListener((obs, anterior, nuevo) -> {
            limpiarAsientos();
            actualizarTotales();
        });

        cbProducto.valueProperty().addListener((obs, anterior, nuevo) -> mostrarProducto());
        txtCantidad.textProperty().addListener((obs, anterior, nuevo) -> mostrarProducto());

        configurarTablaCarrito();
        configurarTablaVentas();
        refrescarCarrito();
        refrescarVentas();
    }

    public void actualizarListas() {
        refrescarCombo(cbFuncion, funcionService.findAll());
        refrescarCombo(cbProducto, productoService.findAll());
    }

    private <T> void refrescarCombo(ComboBox<T> combo, List<T> datos) {
        T actual = combo.getValue();
        combo.setItems(FXCollections.observableArrayList(datos));
        if (actual != null && datos.contains(actual)) {
            combo.setValue(actual);
        }
    }

    private void configurarTablaCarrito() {
        tablaCarrito.getColumns().add(columna("Producto", "nombre", 260.0));
        tablaCarrito.getColumns().add(columna("Cantidad", "cantidad", 90.0));
        tablaCarrito.getColumns().add(columna("P. Unitario", "precioUnitario", 110.0));
        tablaCarrito.getColumns().add(columna("Subtotal", "subtotal", 110.0));

        TableColumn<ItemCarrito, Void> acciones = new TableColumn<>("Acciones");
        acciones.setPrefWidth(110.0);
        acciones.setCellFactory(col -> new TableCell<ItemCarrito, Void>() {
            private final Button btnQuitar = new Button("Quitar");

            {
                btnQuitar.getStyleClass().addAll("btn", "btn-danger");
                btnQuitar.setOnAction(e -> {
                    if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                        return;
                    }
                    quitar(getTableView().getItems().get(getIndex()));
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btnQuitar);
            }
        });
        tablaCarrito.getColumns().add(acciones);
        tablaCarrito.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private void configurarTablaVentas() {
        tablaVentas.getColumns().add(columnaVenta("N°", "idVenta", 60.0));
        tablaVentas.getColumns().add(columnaVenta("Fecha", "fechaTexto", 140.0));
        tablaVentas.getColumns().add(columnaVenta("Cliente", "clienteNombre", 240.0));
        tablaVentas.getColumns().add(columnaVenta("Entradas", "cantidadEntradas", 80.0));
        tablaVentas.getColumns().add(columnaVenta("Asientos", "asientosTexto", 120.0));
        tablaVentas.getColumns().add(columnaVenta("Total (S/)", "total", 100.0));
        tablaVentas.getColumns().add(columnaVenta("Pago", "tipoPagoTexto", 90.0));
        tablaVentas.getColumns().add(columnaVenta("Usuario", "usuarioNombre", 100.0));
        tablaVentas.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
    }

    private TableColumn<ItemCarrito, Object> columna(String titulo, String campo, double ancho) {
        TableColumn<ItemCarrito, Object> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(new PropertyValueFactory<>(campo));
        columna.setPrefWidth(ancho);
        return columna;
    }

    private TableColumn<Venta, Object> columnaVenta(String titulo, String campo, double ancho) {
        TableColumn<Venta, Object> columna = new TableColumn<>(titulo);
        columna.setCellValueFactory(new PropertyValueFactory<>(campo));
        columna.setPrefWidth(ancho);
        return columna;
    }

    @FXML
    public void agregarProducto() {
        Producto producto = cbProducto.getValue();
        if (producto == null) {
            mensaje("Selecciona un producto", true);
            return;
        }
        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
        } catch (NumberFormatException e) {
            mensaje("La cantidad debe ser un número entero", true);
            return;
        }
        if (cantidad <= 0) {
            mensaje("La cantidad debe ser mayor que cero", true);
            return;
        }
        boolean sumado = carrito.agregar(producto.getIdProducto(), producto.getNombre(), producto.getPrecio(), cantidad);
        refrescarCarrito();
        txtCantidad.setText("1");
        mensaje("", false);
        if (sumado) {
            toast("Se sumó " + cantidad + " a " + producto.getNombre() + ": ahora son " + carrito.cantidadDe(producto.getIdProducto()));
        } else {
            toast(producto.getNombre() + " agregado al carrito");
        }
    }

    private void quitar(ItemCarrito item) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Deseas quitar " + item.getNombre() + " del carrito?", ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setTitle("Quitar producto");
        confirmacion.setHeaderText(null);
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        carrito.quitar(item.getIdProducto());
        refrescarCarrito();
        toast("Se eliminó " + item.getNombre() + " del carrito");
    }

    @FXML
    public void vaciarCarrito() {
        if (carrito.estaVacio()) {
            mensaje("El carrito ya está vacío", true);
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION,
                "¿Deseas vaciar todo el carrito?", ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setTitle("Vaciar carrito");
        confirmacion.setHeaderText(null);
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }
        carrito.vaciar();
        refrescarCarrito();
        toast("Se vació el carrito");
    }

    @FXML
    public void registrarVenta() {
        Cliente cliente = clienteActual;
        if (cliente == null) {
            mensaje("Busque un cliente por DNI o RUC", true);
            return;
        }
        Funcion funcion = cbFuncion.getValue();
        if (funcion == null) {
            mensaje("Selecciona una función", true);
            return;
        }
        int entradas = leerEntradas();
        if (entradas < 0) {
            mensaje("Las entradas deben ser un número entero (0 o más)", true);
            return;
        }
        if (entradas == 0 && carrito.estaVacio()) {
            mensaje("Agrega entradas o productos a la venta", true);
            return;
        }
        if (entradas > 0 && asientosElegidos.size() != entradas) {
            mensaje("Elige " + entradas + " asiento(s) con el botón \"Elegir asientos\"", true);
            return;
        }
        TipoPago tipoPago = cbTipoPago.getValue();
        if (tipoPago == null) {
            mensaje("Selecciona el tipo de pago (Efectivo, Yape o Tarjeta)", true);
            return;
        }
        Long idUsuario = SessionManager.getInstance().getUserId();
        if (idUsuario == null) {
            mensaje("No hay un usuario con sesión iniciada", true);
            return;
        }
        try {
            Usuario usuario = usuarioService.findById(idUsuario);
            List<VentaDetalle> detalles = new ArrayList<>();
            for (ItemCarrito item : carrito.items()) {
                detalles.add(VentaDetalle.builder()
                        .cantidad(item.getCantidad())
                        .precioUnitario(item.getPrecioUnitario())
                        .producto(productoService.findById(item.getIdProducto()))
                        .build());
            }
            Venta venta = Venta.builder()
                    .fechaVenta(LocalDateTime.now())
                    .cantidadEntradas(entradas)
                    .total(calcularTotal())
                    .tipoPago(tipoPago)
                    .asientos(new ArrayList<>(asientosElegidos))
                    .cliente(cliente)
                    .funcion(funcion)
                    .usuario(usuario)
                    .detalles(detalles)
                    .build();
            Venta guardada = ventaService.save(venta);
            carrito.vaciar();
            cbTipoPago.setValue(null);
            limpiarCliente();
            limpiarAsientos();
            txtEntradas.setText("1");
            txtCantidad.setText("1");
            refrescarCarrito();
            refrescarVentas();
            mensaje("", false);
            toast("Venta N° " + guardada.getIdVenta() + " registrada por S/ " + formato(guardada.getTotal()));
            mostrarTicket(guardada);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("asiento")) {
                limpiarAsientos();
            }
            mensaje(e.getMessage() == null || e.getMessage().isBlank() ? "No se pudo registrar la venta" : e.getMessage(), true);
        }
    }

    private void limpiarCliente() {
        clienteActual = null;
        txtDocBuscar.clear();
        txtNombreCliente.clear();
        btnRegCliente.setDisable(true);
    }

    @FXML
    public void buscarCliente() {
        String documento = txtDocBuscar.getText().trim();
        txtNombreCliente.clear();
        btnRegCliente.setDisable(true);
        clienteActual = null;
        if (documento.length() != 8 && documento.length() != 11) {
            toast("El valor buscado debe tener 8 u 11 dígitos");
            return;
        }
        for (Cliente c : clienteService.findAll()) {
            if (documento.equals(c.getDniruc())) {
                clienteActual = c;
            }
        }
        if (clienteActual != null) {
            txtNombreCliente.setText(clienteActual.getNombres());
            toast("El cliente sí existe");
            return;
        }
        if (documento.length() == 11) {
            toast("El RUC no está registrado, regístrelo en la pestaña Clientes");
            return;
        }
        btnBuscarDoc.setDisable(true);
        toast("Buscando en RENIEC...");
        Task<PersonaDto> tarea = new Task<>() {
            @Override
            protected PersonaDto call() {
                return new ConsultaDNI().consultar(documento);
            }
        };
        tarea.setOnSucceeded(evento -> {
            btnBuscarDoc.setDisable(false);
            PersonaDto persona = tarea.getValue();
            if (persona == null) {
                toast("El cliente no se encuentra en RENIEC, regístrelo en la pestaña Clientes");
                return;
            }
            txtNombreCliente.setText(persona.getNombreCompleto());
            btnRegCliente.setDisable(false);
            toast("El cliente se encontró en RENIEC, para registrarlo haga clic en Añadir");
        });
        tarea.setOnFailed(evento -> {
            btnBuscarDoc.setDisable(false);
            toast("No hay conexión a internet");
        });
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    @FXML
    public void registrarCliente() {
        String documento = txtDocBuscar.getText().trim();
        try {
            clienteActual = clienteService.save(new Cliente(documento, txtNombreCliente.getText().trim(), "", TipoDocumento.DNI));
            btnRegCliente.setDisable(true);
            toast("El cliente se guardó correctamente");
        } catch (RuntimeException e) {
            toast("Error al guardar el cliente");
        }
    }

    private void limpiarAsientos() {
        asientosElegidos.clear();
        if (lblAsientos != null) {
            lblAsientos.setText("Ninguno");
        }
    }

    @FXML
    public void elegirAsientos() {
        Funcion funcion = cbFuncion.getValue();
        if (funcion == null) {
            mensaje("Primero selecciona una función", true);
            return;
        }
        int entradas = leerEntradas();
        if (entradas <= 0) {
            mensaje("Indica cuántas entradas (1 o más) para poder elegir asientos", true);
            return;
        }
        if (funcion.getSala() == null || funcion.getSala().getCapacidad() == null) {
            mensaje("La función no tiene una sala con capacidad definida", true);
            return;
        }
        List<String> ocupados;
        try {
            ocupados = ventaService.asientosOcupados(funcion.getIdFuncion());
        } catch (RuntimeException e) {
            mensaje(e.getMessage() == null ? "No se pudieron cargar los asientos" : e.getMessage(), true);
            return;
        }
        List<String> todos = Asientos.codigos(funcion.getSala().getCapacidad());
        int libres = todos.size() - ocupados.size();
        if (libres < entradas) {
            mensaje("Solo quedan " + libres + " asiento(s) libres en esta función", true);
            return;
        }
        mensaje("", false);

        List<ToggleButton> botones = new ArrayList<>();
        Label contador = new Label();
        GridPane grilla = new GridPane();
        grilla.setHgap(6);
        grilla.setVgap(6);
        for (int i = 0; i < todos.size(); i++) {
            String codigo = todos.get(i);
            ToggleButton boton = new ToggleButton(codigo);
            boton.setPrefWidth(54);
            if (ocupados.contains(codigo)) {
                boton.setDisable(true);
                boton.setStyle(estiloAsiento("ocupado"));
            } else {
                boton.setSelected(asientosElegidos.contains(codigo));
                boton.setStyle(estiloAsiento(boton.isSelected() ? "elegido" : "libre"));
                boton.selectedProperty().addListener((obs, antes, ahora) -> {
                    if (ahora && contarElegidos(botones) > entradas) {
                        boton.setSelected(false);
                        return;
                    }
                    boton.setStyle(estiloAsiento(ahora ? "elegido" : "libre"));
                    contador.setText("Elegidos: " + contarElegidos(botones) + " de " + entradas);
                });
            }
            botones.add(boton);
            grilla.add(boton, i % Asientos.COLUMNAS, i / Asientos.COLUMNAS);
        }
        contador.setText("Elegidos: " + contarElegidos(botones) + " de " + entradas);
        contador.setStyle("-fx-font-weight: bold;");

        Label pantalla = new Label("PANTALLA");
        pantalla.setMaxWidth(Double.MAX_VALUE);
        pantalla.setAlignment(Pos.CENTER);
        pantalla.setStyle("-fx-background-color: #444; -fx-text-fill: white; -fx-padding: 4;");
        Label leyenda = new Label("Gris = libre    Verde = elegido    Rojo = ocupado");
        ScrollPane scroll = new ScrollPane(grilla);
        scroll.setPrefViewportHeight(Math.min(380, 40 * ((todos.size() + Asientos.COLUMNAS - 1) / Asientos.COLUMNAS) + 10));
        scroll.setPrefViewportWidth(Asientos.COLUMNAS * 60 + 20);
        VBox contenido = new VBox(10, pantalla, scroll, leyenda, contador);
        contenido.setPadding(new Insets(10));

        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Elegir asientos");
        dialogo.setHeaderText(funcion.getEtiqueta());
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button aceptar = (Button) dialogo.getDialogPane().lookupButton(ButtonType.OK);
        aceptar.addEventFilter(ActionEvent.ACTION, evento -> {
            if (contarElegidos(botones) != entradas) {
                evento.consume();
                contador.setText("Debes elegir exactamente " + entradas + " asiento(s)");
            }
        });
        if (dialogo.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            asientosElegidos.clear();
            for (ToggleButton boton : botones) {
                if (!boton.isDisable() && boton.isSelected()) {
                    asientosElegidos.add(boton.getText());
                }
            }
            lblAsientos.setText(String.join(", ", asientosElegidos));
        }
    }

    private int contarElegidos(List<ToggleButton> botones) {
        int total = 0;
        for (ToggleButton boton : botones) {
            if (!boton.isDisable() && boton.isSelected()) {
                total++;
            }
        }
        return total;
    }

    private String estiloAsiento(String estado) {
        switch (estado) {
            case "ocupado":
                return "-fx-background-color: #d9534f; -fx-text-fill: white; -fx-opacity: 1;";
            case "elegido":
                return "-fx-background-color: #28a745; -fx-text-fill: white;";
            default:
                return "-fx-background-color: #e0e0e0; -fx-text-fill: black;";
        }
    }

    @FXML
    public void verTicket() {
        Venta venta = tablaVentas.getSelectionModel().getSelectedItem();
        if (venta == null) {
            mensaje("Selecciona una venta de la tabla para ver su ticket", true);
            return;
        }
        mensaje("", false);
        mostrarTicket(venta);
    }

    private void mostrarTicket(Venta v) {
        TextArea area = new TextArea(construirTicket(v));
        area.setEditable(false);
        area.setFont(Font.font("Monospaced", 13));
        area.setPrefColumnCount(44);
        area.setPrefRowCount(Math.min(30, 18 + (v.getDetalles() == null ? 0 : v.getDetalles().size())));
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle("Ticket de venta");
        alerta.setHeaderText("Ticket N° " + v.getIdVenta());
        alerta.getDialogPane().setContent(area);
        alerta.showAndWait();
    }

    private String construirTicket(Venta v) {
        String linea = "-".repeat(42);
        StringBuilder sb = new StringBuilder();
        sb.append("           CINEPLANET\n").append(linea).append("\n");
        sb.append("Ticket N°: ").append(v.getIdVenta()).append("\n");
        sb.append("Fecha    : ").append(v.getFechaTexto()).append("\n");
        sb.append("Cliente  : ").append(v.getClienteNombre()).append("\n");
        sb.append("Atendió  : ").append(v.getUsuarioNombre()).append("\n").append(linea).append("\n");
        Funcion f = v.getFuncion();
        if (f != null) {
            sb.append("Película : ").append(f.getPelicula() == null ? "" : f.getPelicula().getTitulo()).append("\n");
            if (f.getSala() != null) {
                sb.append("Sala     : ").append(f.getSala().getNumero()).append(" (").append(f.getSala().getTipo()).append(")\n");
            }
            sb.append("Función  : ").append(f.getFecha()).append(" ").append(f.getHoraInicio()).append("\n");
            sb.append(String.format(Locale.US, "Entradas : %d x S/ %.2f%n", v.getCantidadEntradas(),
                    f.getPrecioEntrada() == null ? 0.0 : f.getPrecioEntrada()));
            if (v.getAsientos() != null && !v.getAsientos().isEmpty()) {
                sb.append("Asientos : ").append(v.getAsientosTexto()).append("\n");
            }
        }
        if (v.getDetalles() != null && !v.getDetalles().isEmpty()) {
            sb.append(linea).append("\n");
            for (VentaDetalle d : v.getDetalles()) {
                sb.append(String.format(Locale.US, "%d x %s  S/ %.2f%n", d.getCantidad(), d.getProducto().getNombre(),
                        d.getCantidad() * d.getPrecioUnitario()));
            }
        }
        sb.append(linea).append("\n");
        sb.append(String.format(Locale.US, "TOTAL    : S/ %.2f%n", v.getTotal()));
        sb.append("Tipo de pago: ").append(v.getTipoPagoTexto()).append("\n").append(linea).append("\n");
        sb.append("     ¡Gracias por su compra!\n");
        return sb.toString();
    }

    private int leerEntradas() {
        try {
            int valor = Integer.parseInt(txtEntradas.getText().trim());
            return valor < 0 ? -1 : valor;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private double subtotalEntradas() {
        Funcion funcion = cbFuncion.getValue();
        int entradas = leerEntradas();
        if (funcion == null || funcion.getPrecioEntrada() == null || entradas < 0) {
            return 0.0;
        }
        return redondear(entradas * funcion.getPrecioEntrada());
    }

    private double calcularTotal() {
        return redondear(subtotalEntradas() + carrito.total());
    }

    private void refrescarCarrito() {
        tablaCarrito.setItems(FXCollections.observableArrayList(carrito.items()));
        actualizarTotales();
    }

    private void refrescarVentas() {
        List<Venta> ventas = new ArrayList<>(ventaService.findAll());
        Collections.reverse(ventas);
        tablaVentas.setItems(FXCollections.observableArrayList(ventas));
    }

    private void mostrarProducto() {
        Producto p = cbProducto.getValue();
        if (p == null) {
            txtCodigo.clear();
            txtPrecioUnit.clear();
            txtPrecioTotal.clear();
            return;
        }
        txtCodigo.setText(String.valueOf(p.getIdProducto()));
        txtPrecioUnit.setText(formato(p.getPrecio()));
        try {
            int cantidad = Integer.parseInt(txtCantidad.getText().trim());
            txtPrecioTotal.setText(formato(p.getPrecio() * cantidad));
        } catch (NumberFormatException e) {
            txtPrecioTotal.clear();
        }
    }

    private void actualizarTotales() {
        double total = calcularTotal();
        lblBase.setText("S/ " + formato(total / 1.18));
        lblIgv.setText("S/ " + formato(total - total / 1.18));
        lblSubEntradas.setText("S/ " + formato(subtotalEntradas()));
        lblSubProductos.setText("S/ " + formato(carrito.total()));
        lblTotal.setText("S/ " + formato(calcularTotal()));
    }

    private void mensaje(String texto, boolean error) {
        lblMsg.setText(texto);
        lblMsg.setStyle(error ? "-fx-text-fill: red; -fx-font-size: 14px;" : "-fx-text-fill: green; -fx-font-size: 14px;");
    }

    private void toast(String texto) {
        Stage stage = (Stage) miContenedor.getScene().getWindow();
        Toast.showToast(stage, texto, 2000, stage.getWidth() / 1.5, stage.getHeight() / 2);
    }

    private double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }

    private String formato(Double valor) {
        return String.format(Locale.US, "%.2f", valor == null ? 0.0 : valor);
    }
}
