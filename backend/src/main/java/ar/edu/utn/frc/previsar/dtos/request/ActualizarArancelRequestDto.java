package ar.edu.utn.frc.previsar.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ActualizarArancelRequestDto {
    @NotNull(message = "El valor del arancel es obligatorio")
    @Positive(message = "El valor del arancel debe ser un monto positivo")
    private BigDecimal valor;   // el nuevo monto del arancel
}
