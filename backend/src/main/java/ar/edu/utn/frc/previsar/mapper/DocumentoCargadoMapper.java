package ar.edu.utn.frc.previsar.mapper;

import ar.edu.utn.frc.previsar.dtos.response.DocumentoCargadoResponseDto;
import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DocumentoCargadoMapper {

    // documentoRequeridoId se toma del id de la relación documentoRequerido.
    @Mapping(target = "documentoRequeridoId", source = "documentoRequerido.id")
    DocumentoCargadoResponseDto toResponse(DocumentoCargado doc);

    List<DocumentoCargadoResponseDto> toResponseList(List<DocumentoCargado> docs);
}
