package ar.edu.utn.frc.previsar.dtos.response;

import java.time.LocalDateTime;

public record DocumentoCargadoResponseDto(
        Long id, Long documentoRequeridoId, String nombreOriginal,
        String tipoMime, long tamanoBytes, LocalDateTime createdAt
) {}
