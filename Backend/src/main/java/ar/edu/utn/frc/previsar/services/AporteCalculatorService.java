package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.AportesCalculados;
import ar.edu.utn.frc.previsar.entities.TipoTarea;

import java.math.BigDecimal;

public interface AporteCalculatorService {
    AportesCalculados calcular(TipoTarea tipoTarea, BigDecimal honorariosReferenciales);
}
