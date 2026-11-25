package pe.edu.upeu.sysventas.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.components.ColumnInfo;
import pe.edu.upeu.sysventas.components.TableViewHelper;
import pe.edu.upeu.sysventas.dto.PersonaDto;
import pe.edu.upeu.sysventas.enums.TipoDocumento;
import pe.edu.upeu.sysventas.model.Cliente;
import pe.edu.upeu.sysventas.service.IClienteService;
import pe.edu.upeu.sysventas.util.ConsultaDNI;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.function.Consumer;

@RequiredArgsConstructor
public class ClienteController {
    private final IClienteService service;
    @FXML private TableView<Cliente> tableView;
    @FXML private TextField txtBuscar;
    private ObservableList<Cliente> items;
    private FilteredList<Cliente> filtered;
    private Validator validator;

    @FXML
    public void initialize() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
        LinkedHashMap<String, ColumnInfo> columns = new LinkedHashMap<>();
        columns.put("Documento", new ColumnInfo("dniruc", 130.0));
        columns.put("Nombres", new ColumnInfo("nombres", 220.0));
        columns.put("Rep. Legal", new ColumnInfo("repLegal", 180.0));
        columns.put("Tipo", new ColumnInfo("tipoDocumento", 130.0));
        new TableViewHelper<Cliente>().addColumnsInOrderWithSize(tableView, columns, this::abrirFormulario, this::eliminar);
        pintarTipoDocumento();
        tableView.setTableMenuButtonVisible(true);
        listar();
        txtBuscar.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(newValue));
    }

    @SuppressWarnings("unchecked")
    private void pintarTipoDocumento() {
        for (TableColumn<Cliente, ?> columna : tableView.getColumns()) {
            if (!"Tipo".equals(columna.getText())) {
                continue;
            }
            TableColumn<Cliente, Object> colTipo = (TableColumn<Cliente, Object>) columna;
            colTipo.setCellFactory(col -> new TableCell<Cliente, Object>() {
                @Override
                protected void updateItem(Object item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                        return;
                    }
                    setText(item.toString());
                    if (item == TipoDocumento.DNI) {
                        setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-font-weight: bold;");
                    } else {
                        setStyle("");
                    }
                }
            });
        }
    }

    private void aplicarFiltro(String texto) {
        if (filtered == null) return;
        String filtro = texto == null ? "" : texto.trim().toLowerCase();
        filtered.setPredicate(c -> filtro.isEmpty()
                || safe(c.getDniruc()).toLowerCase().contains(filtro)
                || safe(c.getNombres()).toLowerCase().contains(filtro)
                || safe(c.getRepLegal()).toLowerCase().contains(filtro)
                || (c.getTipoDocumento() != null && c.getTipoDocumento().name().toLowerCase().contains(filtro)));
    }

    @FXML public void nuevoCliente() { abrirFormulario(null); }

    @FXML
    public void editarCliente() {
        Cliente seleccionado = tableView.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            alerta("Selecciona un cliente", "Debes seleccionar un cliente para editarlo.");
            return;
        }
        abrirFormulario(seleccionado);
    }

    @FXML
    public void eliminarCliente() {
        Cliente seleccionado = tableView.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            alerta("Selecciona un cliente", "Debes seleccionar un cliente para eliminarlo.");
            return;
        }
        eliminar(seleccionado);
    }

    private void eliminar(Cliente cliente) {
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION, "¿Deseas eliminar al cliente " + cliente.getNombres() + "?", ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setTitle("Eliminar cliente");
        confirmacion.setHeaderText(null);
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.delete(cliente.getDniruc());
                listar();
                toast("Cliente eliminado correctamente");
            } catch (RuntimeException e) {
                alerta("No se pudo eliminar", mensajeError(e));
            }
        }
    }

    private void abrirFormulario(Cliente existente) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(existente == null ? "Nuevo cliente" : "Editar cliente");
        ButtonType accion = new ButtonType(existente == null ? "Guardar" : "Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(accion, ButtonType.CANCEL);

        TextField documento = new TextField(existente == null ? "" : safe(existente.getDniruc()));
        TextField nombres = new TextField(existente == null ? "" : safe(existente.getNombres()));
        TextField repLegal = new TextField(existente == null ? "" : safe(existente.getRepLegal()));
        ComboBox<TipoDocumento> tipo = new ComboBox<>(FXCollections.observableArrayList(TipoDocumento.values()));
        tipo.setValue(existente == null ? TipoDocumento.DNI : existente.getTipoDocumento());
        documento.setDisable(existente != null);

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new javafx.geometry.Insets(20));
        Button buscarDni = new Button("Buscar DNI en la web");
        buscarDni.setDisable(existente != null);
        buscarDni.setOnAction(e -> buscarEnWeb(documento, nombres, tipo, buscarDni));
        HBox filaDocumento = new HBox(8, documento, buscarDni);
        grid.addRow(0, new Label("Documento:"), filaDocumento);
        grid.addRow(1, new Label("Nombres:"), nombres);
        grid.addRow(2, new Label("Rep. Legal:"), repLegal);
        grid.addRow(3, new Label("Tipo:"), tipo);
        dialog.getDialogPane().setContent(grid);

        Button botonAccion = (Button) dialog.getDialogPane().lookupButton(accion);
        botonAccion.addEventFilter(ActionEvent.ACTION, event -> {
            Cliente cliente = new Cliente(documento.getText().trim(), nombres.getText().trim(), repLegal.getText().trim(), tipo.getValue());
            Set<ConstraintViolation<Cliente>> errores = validator.validate(cliente);
            if (!errores.isEmpty()) {
                event.consume();
                String texto = "";
                for (ConstraintViolation<?> v : errores) {
                    texto = texto + v.getMessage() + "\n";
                }
                alerta("Datos incorrectos", texto);
                return;
            }
            try {
                if (existente == null) service.save(cliente);
                else service.update(existente.getDniruc(), cliente);
                listar();
                toast(existente == null ? "Se guardó correctamente" : "Se actualizó correctamente");
            } catch (RuntimeException e) {
                event.consume();
                alerta("No se pudo guardar", mensajeError(e));
            }
        });
        dialog.showAndWait();
    }

    private void buscarEnWeb(TextField documento, TextField nombres, ComboBox<TipoDocumento> tipo, Button boton) {
        String dni = documento.getText().trim();
        if (!dni.matches("\\d{8}")) {
            alerta("DNI incorrecto", "El DNI debe tener exactamente 8 números.");
            return;
        }
        boton.setDisable(true);
        boton.setText("Buscando...");
        Task<PersonaDto> tarea = new Task<>() {
            @Override
            protected PersonaDto call() {
                return new ConsultaDNI().consultar(dni);
            }
        };
        tarea.setOnSucceeded(evento -> {
            boton.setDisable(false);
            boton.setText("Buscar DNI en la web");
            PersonaDto persona = tarea.getValue();
            if (persona == null) {
                alerta("No encontrado", "No se encontró ese DNI en la web. Puedes escribir el nombre manualmente.");
                return;
            }
            tipo.setValue(TipoDocumento.DNI);
            nombres.setText(persona.getNombreCompleto());
        });
        tarea.setOnFailed(evento -> {
            boton.setDisable(false);
            boton.setText("Buscar DNI en la web");
            Throwable error = tarea.getException();
            alerta("No se pudo consultar", error == null || error.getMessage() == null ? "No hay conexión con la web." : error.getMessage());
        });
        Thread hilo = new Thread(tarea);
        hilo.setDaemon(true);
        hilo.start();
    }

    private void listar() {
        items = FXCollections.observableArrayList(service.findAll());
        filtered = new FilteredList<>(items, c -> true);
        tableView.setItems(filtered);
        aplicarFiltro(txtBuscar == null ? "" : txtBuscar.getText());
    }

    private void alerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private String safe(String value) { return value == null ? "" : value; }
    private String mensajeError(RuntimeException e) { return e.getMessage() == null || e.getMessage().isBlank() ? "Ocurrió un error al procesar la operación." : e.getMessage(); }

    private void toast(String texto) {
        javafx.stage.Stage stage = (javafx.stage.Stage) tableView.getScene().getWindow();
        pe.edu.upeu.sysventas.components.Toast.showToast(stage, texto, 2000, stage.getWidth() / 1.5, stage.getHeight() / 2);
    }
}
