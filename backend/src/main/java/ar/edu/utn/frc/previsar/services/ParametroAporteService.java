package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.request.ActualizarArancelRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ArancelResponseDto;

public interface ParametroAporteService {
    ArancelResponseDto obtenerArancelVigente();

    void actualizarArancel(ActualizarArancelRequestDto request);
}
