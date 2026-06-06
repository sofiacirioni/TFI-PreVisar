package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.response.*;

import java.util.List;

public interface CatalogoService {
    List<ProvinciaResponseDto> listarProvincias();
    List<RegionalResponseDto> listarRegionales();
    List<CondicionIvaResponseDto> listarCondicionesIva();
    List<TituloResponseDto> listarTitulos();
    List<TipoTareaResponseDto> listarTiposTarea();
}
