package ar.edu.utn.frc.previsar.dtos;

public record DocumentoRequeridoDto(
        Long id,
        String codigo,
        String nombre,
        boolean obligatorio,
        int orden,
        boolean permiteMultiples
) {}
