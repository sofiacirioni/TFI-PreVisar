package ar.edu.utn.frc.previsar.dtos.response;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/**
 * Detalle de una revisión. El front lo consulta hasta que `estado` deje de ser
 * EN_PROGRESO. `resultado` es el resumen crudo de la IA (null mientras corre o si
 * hubo error); `detalle` trae el motivo cuando `estado` = ERROR.
 */
public record RevisionExternaDetalleDto(
        Long id,
        String nombreArchivo,
        String estado,
        String detalle,
        JsonNode resultado,
        Instant createdAt
) {}
