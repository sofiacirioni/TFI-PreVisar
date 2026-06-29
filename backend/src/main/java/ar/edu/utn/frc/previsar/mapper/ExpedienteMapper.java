package ar.edu.utn.frc.previsar.mapper;

import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;
import ar.edu.utn.frc.previsar.entities.Expediente;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper entre la entidad Expediente y su DTO de respuesta.
 *
 *
 */

@Mapper(componentModel = "spring")
public interface ExpedienteMapper {
    @Mapping(target = "obraId", source = "obra.id")
    @Mapping(target = "obraDesignacion", source = "obra.designacion")
    @Mapping(target = "provinciaId", source = "obra.provincia.id")
    @Mapping(target = "provinciaNombre", source = "obra.provincia.nombre")
    @Mapping(target = "comitenteId", source = "obra.comitente.id")
    @Mapping(target = "comitenteNombre", source = "obra.comitente.nombreRazonSocial")
    @Mapping(target = "comitenteDniCuit", source = "obra.comitente.dniCuit")
    @Mapping(target = "tipoTareaId", source = "tipoTarea.id")
    @Mapping(target = "tipoTareaCodigo", source = "tipoTarea.codigo")
    @Mapping(target = "tipoTareaNombre", source = "tipoTarea.nombre")
    @Mapping(target = "especialidadId", source = "tipoTarea.especialidad.id")
    @Mapping(target = "especialidadNombre", source = "tipoTarea.especialidad.nombre")
    ExpedienteResponseDto toResponse(Expediente expediente);
}
