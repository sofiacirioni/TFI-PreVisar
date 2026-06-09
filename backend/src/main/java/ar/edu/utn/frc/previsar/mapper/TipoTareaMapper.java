package ar.edu.utn.frc.previsar.mapper;

import ar.edu.utn.frc.previsar.dtos.response.TipoTareaResponseDto;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre la entidad TipoTarea y su DTO de respuesta.
 *
 * Los campos planos (id, codigo, nombre) se detectan por nombre. Los campos
 * derivados de la relación con Especialidad se declaran explícitamente.
 */
@Mapper(componentModel = "spring")
public interface TipoTareaMapper {
    @Mapping(source = "especialidad.id", target = "especialidadId")
    @Mapping(source = "especialidad.nombre", target = "especialidadNombre")
    TipoTareaResponseDto toResponse(TipoTarea tipoTarea);
}
