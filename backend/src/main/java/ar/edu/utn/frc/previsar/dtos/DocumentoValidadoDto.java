package ar.edu.utn.frc.previsar.dtos;

import java.util.List;

public record DocumentoValidadoDto(
        Long documentoCargadoId,
        Long documentoRequeridoId,
        String nombreOriginal,
        List<ObservacionDto> observaciones) {
}
