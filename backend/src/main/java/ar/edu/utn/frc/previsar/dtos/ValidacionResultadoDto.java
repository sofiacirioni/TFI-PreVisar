package ar.edu.utn.frc.previsar.dtos;

import java.util.List;

public record ValidacionResultadoDto(
        List<DocumentoValidadoDto> documentos,
        List<ObservacionDto> generales) {
    public boolean tieneObservaciones() {
        return !generales.isEmpty() || documentos.stream().anyMatch(d -> !d.observaciones().isEmpty());
    }
}
