package ar.edu.utn.frc.previsar.mapper;

import ar.edu.utn.frc.previsar.dtos.response.TipoTareaResponseDto;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import org.mapstruct.Mapper;

/**
 * Mapper entre la entidad TipoTarea y su DTO de respuesta.
 *
 * Mapeo trivial campo-a-campo (id, codigo, nombre). MapStruct los detecta
 * automáticamente por nombre, no hace falta declarar @Mapping.
 */
@Mapper(componentModel = "spring")
public interface TipoTareaMapper {
    TipoTareaResponseDto toResponse(TipoTarea tipoTarea);
}
