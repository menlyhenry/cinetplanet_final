package pe.edu.upeu.sysventas.service;

import pe.edu.upeu.sysventas.model.Venta;

import java.util.List;

public interface IVentaService extends ICrudGenericoService<Venta, Long> {

    List<String> asientosOcupados(Long idFuncion);
}
