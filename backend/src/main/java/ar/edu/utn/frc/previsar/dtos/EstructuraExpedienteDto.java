package ar.edu.utn.frc.previsar.dtos;

import java.util.List;

public record EstructuraExpedienteDto(
        Long tipoTareaId,
        String tipoTareaCodigo,
        List<SeccionDto> secciones
) {}
