package ar.edu.utn.frc.previsar.dtos.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Valor vigente del arancel administrativo.
 * Lo consume la pantalla de gestión de parámetros (solo revisor).
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ArancelResponseDto {
    private BigDecimal valor;

    private LocalDate vigenciaDesde;
}
