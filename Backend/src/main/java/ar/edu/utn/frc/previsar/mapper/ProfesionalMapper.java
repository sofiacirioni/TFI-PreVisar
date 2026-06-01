package ar.edu.utn.frc.previsar.mapper;

import ar.edu.utn.frc.previsar.dtos.response.ProfesionalResponseDto;
import ar.edu.utn.frc.previsar.entities.Profesional;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre la entidad Profesional y su DTO de respuesta.
 *
 * Mapea los campos del Usuario asociado (email, rol, activo), de la
 * Regional (id, nombre, provincia) y de la CondicionIVA (id, descripción).
 *
 * El campo esRevisor NO se mapea acá: requiere consultar RolRevisorRepository.
 * El service lo setea después de llamar a este mapper.
 */
@Mapper(componentModel = "spring")
public interface ProfesionalMapper {
    @Mapping(source = "usuario.email", target = "email")
    @Mapping(source = "usuario.rol", target = "rol")
    @Mapping(source = "usuario.activo", target = "activo")
    @Mapping(target = "tituloId", source = "titulo.id")
    @Mapping(target = "tituloNombre", source = "titulo.nombre")
    @Mapping(source = "regional.id", target = "regionalId")
    @Mapping(source = "regional.nombre", target = "regionalNombre")
    @Mapping(source = "regional.provincia.nombre", target = "provinciaNombre")
    @Mapping(source = "condicionIva.id", target = "condicionIvaId")
    @Mapping(source = "condicionIva.descripcion", target = "condicionIvaDescripcion")
    @Mapping(target = "esRevisor", ignore = true)
    ProfesionalResponseDto toResponse(Profesional profesional);
}
