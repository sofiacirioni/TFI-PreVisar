package ar.edu.utn.frc.previsar.dtos.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class CalcularAportesRequestDto {
    private Long tipoTareaId;
    private BigDecimal honorariosReferenciales;
}
