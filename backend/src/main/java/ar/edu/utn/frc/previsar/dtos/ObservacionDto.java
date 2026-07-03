package ar.edu.utn.frc.previsar.dtos;

import ar.edu.utn.frc.previsar.enums.NivelObservacion;
import ar.edu.utn.frc.previsar.enums.OrigenObservacion;

public record ObservacionDto(
        String codigo, NivelObservacion nivel, OrigenObservacion origen, String mensaje) {
}
