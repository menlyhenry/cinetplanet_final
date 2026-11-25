package pe.edu.upeu.sysventas.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.stage.Stage;
import pe.edu.upeu.sysventas.SysVentas;
import pe.edu.upeu.sysventas.config.AppContext;
import pe.edu.upeu.sysventas.dto.SessionManager;

import java.io.IOException;

public class MainController {

    @FXML
    private Label lblUsuario;
    @FXML
    private TabPane tabs;
    @FXML
    private Tab tabInicio;

    @FXML
    public void initialize() {
        String nombre = SessionManager.getInstance().getUserName();
        lblUsuario.setText("Usuario: " + (nombre == null ? "" : nombre));
        tabs.getSelectionModel().selectedItemProperty().addListener((obs, anterior, nueva) -> {
            if (nueva != null && nueva.getText().equals("Ventas")) {
                AppContext.getInstance().getBean(VentaController.class).actualizarListas();
            }
            if (nueva == tabInicio) {
                AppContext.getInstance().getBean(InicioController.class).actualizar();
            }
        });
    }

    @FXML
    public void cerrarSesion() {
        SessionManager.getInstance().limpiar();
        Stage stage = (Stage) lblUsuario.getScene().getWindow();
        try {
            SysVentas.mostrarLogin(stage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
