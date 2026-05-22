package ar.edu.utn.frc.previsar.dtos.request;

import ar.edu.utn.frc.previsar.enums.TipoPersona;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Datos para alta o modificación de un Comitente.
 *
 * El profesional dueño se infiere del usuario autenticado (no se pasa
 * en el body para evitar que alguien intente cargar comitentes en la
 * cartera de otro profesional).
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComitenteRequestDto {
    @NotNull(message = "El tipo de persona es obligatorio")
    private TipoPersona tipoPersona;

    @NotBlank(message = "El nombre o razón social es obligatorio")
    @Size(max = 200)
    private String nombreRazonSocial;

    @NotBlank(message = "El DNI o CUIT es obligatorio")
    @Pattern(regexp = "^(\\d{7,8}|\\d{2}-\\d{8}-\\d{1})$",
            message = "Debe ser un DNI (7-8 dígitos) o un CUIT (XX-XXXXXXXX-X)")
    private String dniCuit;

    @NotBlank(message = "El domicilio es obligatorio")
    @Size(max = 255)
    private String domicilio;

    @Email(message = "El email no tiene un formato válido")
    @Size(max = 255)
    private String email;

    @Size(max = 30)
    private String telefono;
}
