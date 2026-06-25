package ar.edu.utn.frc.previsar.mapper;

import ar.edu.utn.frc.previsar.dtos.response.ComitenteResponseDto;
import ar.edu.utn.frc.previsar.entities.Comitente;
import org.mapstruct.Mapper;

/**
 * Mapper entre la entidad Comitente y su DTO de respuesta.
 *
 * MapStruct genera la implementación en tiempo de compilación.
 * Como los nombres de campos coinciden exactamente, no necesita
 * @Mapping para ningún campo.
 */

@Mapper(componentModel = "spring")
public interface ComitenteMapper {
    ComitenteResponseDto toResponse(Comitente comitente);
}
