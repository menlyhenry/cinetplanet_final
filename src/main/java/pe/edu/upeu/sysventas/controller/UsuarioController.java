package pe.edu.upeu.sysventas.controller;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import lombok.RequiredArgsConstructor;
import pe.edu.upeu.sysventas.components.ColumnInfo;
import pe.edu.upeu.sysventas.components.TableViewHelper;
import pe.edu.upeu.sysventas.components.Toast;
import pe.edu.upeu.sysventas.dto.SessionManager;
import pe.edu.upeu.sysventas.model.Perfil;
import pe.edu.upeu.sysventas.model.Usuario;
import pe.edu.upeu.sysventas.service.IUsuarioService;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Set;

@RequiredArgsConstructor
public class UsuarioController {
    private final IUsuarioService service;
    @FXML private TableView<Usuario> tableView;
    @FXML private TextField txtBuscar;
    private ObservableList<Usuario> items;
    private FilteredList<Usuario> filtered;
    private Validator validator;

    @FXML
    public void initialize() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        LinkedHashMap<String, ColumnInfo> columns = new LinkedHashMap<>();
        columns.put("ID", new ColumnInfo("idUsuario", 60.0));
        columns.put("Usuario", new ColumnInfo("usuario", 130.0));
        columns.put("Clave (encriptada)", new ColumnInfo("clave", 420.0));
        columns.put("Perfil", new ColumnInfo("perfilNombre", 120.0));
        columns.put("Estado", new ColumnInfo("estado", 90.0));
        new TableViewHelper<Usuario>().addColumnsInOrderWithSize(tableView, columns, this::abrirFormulario, this::eliminar);
        tableView.setTableMenuButtonVisible(true);
        listar();
        txtBuscar.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro(newValue));
    }

    private void aplicarFiltro(String texto) {
        if (filtered == null) return;
        String filtro = texto == null ? "" : texto.trim().toLowerCase();
        filtered.setPredicate(u -> filtro.isEmpty()
                || safe(u.getUsuario()).toLowerCase().contains(filtro)
                || safe(u.getEstado()).toLowerCase().contains(filtro));
    }

    @FXML public void nuevoUsuario() { abrirFormulario(null); }

    @FXML
    public void editarUsuario() {
        Usuario seleccionado = tableView.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            alerta("Selecciona un usuario", "Debes seleccionar un usuario para editarlo.");
            return;
        }
        abrirFormulario(seleccionado);
    }

    @FXML
    public void eliminarUsuario() {
        Usuario seleccionado = tableView.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            alerta("Selecciona un usuario", "Debes seleccionar un usuario para eliminarlo.");
            return;
        }
        eliminar(seleccionado);
    }

    private void eliminar(Usuario usuario) {
        Long idSesion = SessionManager.getInstance().getUserId();
        if (usuario.getIdUsuario().equals(idSesion)) {
            alerta("No se puede eliminar", "No puedes eliminar el usuario con el que iniciaste sesión.");
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION, "¿Deseas eliminar al usuario " + usuario.getUsuario() + "?", ButtonType.OK, ButtonType.CANCEL);
        confirmacion.setTitle("Eliminar usuario");
        confirmacion.setHeaderText(null);
        if (confirmacion.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                service.delete(usuario.getIdUsuario());
                listar();
                toast("Usuario eliminado correctamente");
            } catch (RuntimeException e) {
                alerta("No se pudo eliminar", mensajeError(e));
            }
        }
    }

    private void abrirFormulario(Usuario existente) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle(existente == null ? "Nuevo usuario" : "Editar usuario");
        ButtonType accion = new ButtonType(existente == null ? "Guardar" : "Actualizar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(accion, ButtonType.CANCEL);

        TextField usuario = new TextField(existente == null ? "" : safe(existente.getUsuario()));
        PasswordField clave = new PasswordField();
        clave.setPromptText(existente == null ? "Mínimo 6 caracteres" : "Vacío = mantener la actual");
        ComboBox<String> estado = new ComboBox<>(FXCollections.observableArrayList("ACTIVO", "INACTIVO"));
        estado.setValue(existente == null ? "ACTIVO" : existente.getEstado());

        GridPane grid = new GridPane();
        grid.setHgap(12);
        grid.setVgap(12);
        grid.setPadding(new javafx.geometry.Insets(20));
        grid.addRow(0, new Label("Usuario:"), usuario);
        grid.addRow(1, new Label("Clave:"), clave);
        grid.addRow(2, new Label("Estado:"), estado);
        dialog.getDialogPane().setContent(grid);

        Button botonAccion = (Button) dialog.getDialogPane().lookupButton(accion);
        botonAccion.addEventFilter(ActionEvent.ACTION, event -> {
            Usuario nuevo = Usuario.builder()
                    .usuario(usuario.getText().trim())
                    .clave(clave.getText())
                    .estado(estado.getValue())
                    .idPerfil(existente == null ? perfilVendedor() : existente.getIdPerfil())
                    .build();
            Set<ConstraintViolation<Usuario>> errores = validator.validate(nuevo);
            StringBuilder mensajes = new StringBuilder();
            for (ConstraintViolation<Usuario> v : errores) {
                mensajes.append(v.getMessage()).append("\n");
            }
            boolean claveVacia = clave.getText() == null || clave.getText().isEmpty();
            if (existente == null && claveVacia) {
                mensajes.append("La clave es obligatoria\n");
            } else if (!claveVacia && clave.getText().length() < 6) {
                mensajes.append("La clave debe tener al menos 6 caracteres\n");
            }
            if (mensajes.length() > 0) {
                event.consume();
                alerta("Datos incorrectos", mensajes.toString().trim());
                return;
            }
            try {
                if (existente == null) service.save(nuevo);
                else service.update(existente.getIdUsuario(), nuevo);
                listar();
                toast(existente == null ? "Se guardó correctamente. La clave quedó encriptada" : "Se actualizó correctamente");
            } catch (RuntimeException e) {
                event.consume();
                alerta("No se pudo guardar", mensajeError(e));
            }
        });
        dialog.showAndWait();
    }

    private Perfil perfilVendedor() {
        Perfil perfil = new Perfil();
        perfil.setIdPerfil(2L);
        perfil.setNombre("Vendedor");
        perfil.setCodigo("VENDEDOR");
        return perfil;
    }

    private void listar() {
        items = FXCollections.observableArrayList(service.findAll());
        filtered = new FilteredList<>(items, u -> true);
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
        Toast.showToast(stage, texto, 2000, stage.getWidth() / 1.5, stage.getHeight() / 2);
    }
}
