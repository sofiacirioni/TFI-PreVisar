package ar.edu.utn.frc.previsar.mapper;

import ar.edu.utn.frc.previsar.dtos.response.ObraResponseDto;
import ar.edu.utn.frc.previsar.entities.Obra;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre la entidad Obra y su DTO de respuesta.
 *
 * Campos especiales:
 *   - comitenteId, comitenteNombre, comitenteDniCuit: vienen de la
 *     entidad Comitente asociada (navegación).
 *   - nomenclaturaCatastral: campo derivado, calculado por el método
 *     getNomenclaturaCatastral() de la entidad. MapStruct lo detecta
 *     automáticamente.
 */

@Mapper(componentModel = "spring")
public interface ObraMapper {
    @Mapping(source = "comitente.id", target = "comitenteId")
    @Mapping(source = "comitente.nombreRazonSocial", target = "comitenteNombre")
    @Mapping(source = "comitente.dniCuit", target = "comitenteDniCuit")
    @Mapping(source = "provincia.id", target = "provinciaId")
    @Mapping(source = "provincia.nombre", target = "provinciaNombre")
    ObraResponseDto toResponse(Obra obra);
}
