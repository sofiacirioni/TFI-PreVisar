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
 *
 * <p>Los flags son {@code Boolean} (nullable) a propósito: si el cliente no los
 * envía, en el alta se aplica el default del negocio y en la modificación se
 * conserva el valor actual, en vez de pisarlo con {@code false}. Un primitivo con
 * {@code @Builder.Default} no alcanza: Jackson deserializa por el constructor sin
 * argumentos y Lombok mueve el inicializador al builder, así que un campo ausente
 * llegaría como {@code false} igual.
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

    /** Si no se envía: en alta se asume obligatorio; en modificación se conserva. */
    private Boolean obligatorio;

    /** Opcional: si no se envía, el documento se agrega al final de la sección. */
    @Positive(message = "El orden debe ser un entero positivo")
    private Integer orden;

    /**
     * La ranura admite varios archivos (ej. comprobantes, planos). Si no se envía:
     * en alta se asume un único archivo; en modificación se conserva.
     */
    private Boolean permiteMultiples;

    /**
     * El sistema puede producir el PDF de esta ranura (carátula, contrato). Si no
     * se envía: en alta se asume que no; en modificación se conserva.
     */
    private Boolean generable;

    /**
     * Se espera que el documento venga en A4. Va en false para los de gran formato
     * (planos A1/A3). Si no se envía: en alta se asume que sí; en modificación se
     * conserva.
     */
    private Boolean validaA4;
}
