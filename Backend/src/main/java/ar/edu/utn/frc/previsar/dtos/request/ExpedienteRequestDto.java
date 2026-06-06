package ar.edu.utn.frc.previsar.dtos.request;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteRequestDto {
    private Long obraId;
    private Long tipoTareaId;
    private BigDecimal honorariosReferenciales;
}
