package ar.edu.utn.frc.previsar.dtos;

import java.util.List;

public record SeccionDto(
        Long id,
        String codigo,
        String nombre,
        int orden,
        List<DocumentoRequeridoDto> documentos
) {}
