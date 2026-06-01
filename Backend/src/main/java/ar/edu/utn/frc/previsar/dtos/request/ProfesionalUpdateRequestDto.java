package ar.edu.utn.frc.previsar.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Datos editables del perfil del profesional.
 *
 * NO incluye campos inmutables:
 *   - email (es el username, requiere flujo distinto si se cambia)
 *   - dni, cuit, matricula (deberían validarse contra entidades externas)
 *   - rol (no se autoasigna)
 *
 * Si el usuario quiere cambiar email o password, hay endpoints dedicados.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfesionalUpdateRequestDto {
    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100)
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100)
    private String apellido;

    @NotNull(message = "Debe seleccionar un título")
    private Long tituloId;

    @Size(max = 150, message = "La descripción del título no puede exceder 150 caracteres")
    private String tituloOtroDescripcion;

    @NotBlank(message = "El domicilio es obligatorio")
    @Size(max = 255)
    private String domicilio;

    @Size(max = 30)
    private String telefono;

    @NotNull(message = "Debe seleccionar una regional")
    private Long regionalId;

    @NotNull(message = "Debe seleccionar una condición frente al IVA")
    private Long condicionIvaId;

    @NotNull(message = "Debe declarar si está afiliado a la Caja Ley 8470")
    private Boolean afiliadoCaja8470;
}
