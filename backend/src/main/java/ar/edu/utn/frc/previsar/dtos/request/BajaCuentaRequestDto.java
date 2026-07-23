package ar.edu.utn.frc.previsar.dtos.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Datos para dar de baja la cuenta del profesional autenticado.
 *
 * Se pide la contraseña actual como confirmación: la baja es una acción
 * destructiva (deshabilita el acceso a la cuenta) y no es auto-reversible.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BajaCuentaRequestDto {

    @NotBlank(message = "Debe ingresar su contraseña para confirmar la baja")
    private String password;
}
