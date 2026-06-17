package ar.edu.utn.frc.previsar.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Datos para alta o modificación de una Sección de la estructura documental.
 *
 * La provincia NO se pasa en el body: se infiere del RolRevisor del usuario
 * autenticado (cada revisor administra solo la estructura de su provincia).
 *
 * En modificación se ignora {@code tipoTareaId}: una sección no cambia de
 * tipo de tarea. El {@code codigo} es clave estable; al modificar se mantiene.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeccionRequestDto {

    @NotNull(message = "El tipo de tarea es obligatorio")
    private Long tipoTareaId;

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 40)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 120)
    private String nombre;

    /** Opcional: si no se envía, la sección se agrega al final. */
    @Positive(message = "El orden debe ser un entero positivo")
    private Integer orden;
}
