package ar.edu.utn.frc.previsar.dtos.response;

import java.time.Instant;

/**
 * Fila del historial de revisiones del revisor. Liviano: no incluye el resumen
 * completo, solo lo necesario para listar y decidir si abrir.
 */
public record RevisionExternaResumenDto(
        Long id,
        String nombreArchivo,
        String estado,
        Instant createdAt
) {}
