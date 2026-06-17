package ar.edu.utn.frc.previsar.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Datos para alta o modificación de un Documento Requerido dentro de una sección.
 *
 * La sección a la que pertenece se infiere del path en alta
 * (POST /api/estructura/secciones/{seccionId}/documentos), no se pasa en el body.
 * El {@code codigo} es clave estable; al modificar se mantiene.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoRequeridoRequestDto {

    @NotBlank(message = "El código es obligatorio")
    @Size(max = 40)
    private String codigo;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 160)
    private String nombre;

    /** Si no se envía, se asume obligatorio. */
    @Builder.Default
    private boolean obligatorio = true;

    /** Opcional: si no se envía, el documento se agrega al final de la sección. */
    @Positive(message = "El orden debe ser un entero positivo")
    private Integer orden;
}
