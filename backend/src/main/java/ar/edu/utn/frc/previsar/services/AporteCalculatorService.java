package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.AportesCalculadosDto;
import ar.edu.utn.frc.previsar.entities.TipoTarea;

import java.math.BigDecimal;

public interface AporteCalculatorService {
    AportesCalculadosDto calcular(TipoTarea tipoTarea, BigDecimal honorariosReferenciales);
}
