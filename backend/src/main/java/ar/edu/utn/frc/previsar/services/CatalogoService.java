package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.response.*;

import java.util.List;

public interface CatalogoService {
    List<ProvinciaResponseDto> listarProvincias();
    List<RegionalResponseDto> listarRegionales();
    List<CondicionIvaResponseDto> listarCondicionesIva();
    List<TituloResponseDto> listarTitulos();
    List<EspecialidadResponseDto> listarEspecialidades();
    /** Si especialidadId es null, devuelve todos los tipos activos. */
    List<TipoTareaResponseDto> listarTiposTarea(Long especialidadId);
}
