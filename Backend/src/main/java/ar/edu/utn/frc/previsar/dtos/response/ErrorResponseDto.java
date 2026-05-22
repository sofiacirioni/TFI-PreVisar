package ar.edu.utn.frc.previsar.dtos.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta estándar de error. Devuelta por el GlobalExceptionHandler.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)  // Omite campos null en el JSON
public class ErrorResponseDto {
    private LocalDateTime timestamp;

    private int status; // HTTP status code (400, 404, etc.)

    private String error; // Descripción del status (ej: "Bad Request")

    private String mensaje; // Mensaje específico de este error

    private String path; // El endpoint que tiró el error

    /**
     * Lista de errores de validación. Solo se llena cuando hay errores
     * de @Valid (uno por cada campo inválido).
     */
    private List<ErrorCampo> erroresValidacion;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ErrorCampo {
        private String campo;
        private String mensaje;
    }
}
