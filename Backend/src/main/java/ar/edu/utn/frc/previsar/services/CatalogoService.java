package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.response.CondicionIvaResponseDto;
import ar.edu.utn.frc.previsar.dtos.response.ProvinciaResponseDto;
import ar.edu.utn.frc.previsar.dtos.response.RegionalResponseDto;

import java.util.List;

public interface CatalogoService {
    List<ProvinciaResponseDto> listarProvincias();
    List<RegionalResponseDto> listarRegionales();
    List<CondicionIvaResponseDto> listarCondicionesIva();
}
