package ar.edu.utn.frc.previsar.dtos;

import ar.edu.utn.frc.previsar.enums.NivelObservacion;

public record ObservacionDto(
        String codigo, NivelObservacion nivel, String mensaje) {
}
