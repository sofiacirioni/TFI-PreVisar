package ar.edu.utn.frc.previsar.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Datos para alta o modificación de una Obra.
 *
 * El comitente al que pertenece se infiere del path en alta
 * (POST /api/comitentes/{id}/obras), no se pasa en el body.
 *
 * Los datos catastrales (circunscripción, sección, manzana, parcela)
 * son opcionales: no todas las obras tienen catastro asignado.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ObraRequestDto {
    @NotBlank(message = "La designación es obligatoria")
    @Size(max = 255)
    private String designacion;

    @NotBlank(message = "La calle es obligatoria")
    @Size(max = 150)
    private String calle;

    @NotBlank(message = "El número es obligatorio")
    @Size(max = 20)
    private String numero;

    @Size(max = 100)
    private String barrio;

    @NotBlank(message = "La localidad es obligatoria")
    @Size(max = 100)
    private String localidad;

    @NotNull(message = "Debe seleccionar la provincia de la obra")
    private Long provinciaId;

    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[A-Z0-9]{4,10}$",
            message = "El código postal debe tener entre 4 y 10 caracteres alfanuméricos")
    private String codigoPostal;

    // Datos catastrales - todos opcionales pero si se cargan, deben ser numéricos
    @Pattern(regexp = "^\\d{1,10}$", message = "La circunscripción debe ser numérica")
    private String circunscripcion;

    @Pattern(regexp = "^\\d{1,10}$", message = "La sección debe ser numérica")
    private String seccion;

    @Pattern(regexp = "^\\d{1,10}$", message = "La manzana debe ser numérica")
    private String manzana;

    @Pattern(regexp = "^\\d{1,10}$", message = "La parcela debe ser numérica")
    private String parcela;
}
