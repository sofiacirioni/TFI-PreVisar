package ar.edu.utn.frc.previsar.dtos.response;

import ar.edu.utn.frc.previsar.enums.TipoPersona;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Datos del Comitente devueltos al frontend.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComitenteResponseDto {
    private Long id;

    private TipoPersona tipoPersona;

    private String nombreRazonSocial;

    private String dniCuit;

    private String domicilio;

    private String email;

    private String telefono;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
