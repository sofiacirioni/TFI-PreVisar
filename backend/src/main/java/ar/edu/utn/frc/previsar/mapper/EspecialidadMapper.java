package ar.edu.utn.frc.previsar.mapper;

import ar.edu.utn.frc.previsar.dtos.response.EspecialidadResponseDto;
import ar.edu.utn.frc.previsar.entities.Especialidad;
import org.mapstruct.Mapper;

/**
 * Mapper entre la entidad Especialidad y su DTO de respuesta.
 * Mapeo trivial campo-a-campo (id, codigo, nombre).
 */
@Mapper(componentModel = "spring")
public interface EspecialidadMapper {
    EspecialidadResponseDto toResponse(Especialidad especialidad);
}
