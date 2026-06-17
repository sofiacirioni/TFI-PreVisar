package ar.edu.utn.frc.previsar.mapper;

import ar.edu.utn.frc.previsar.dtos.DocumentoRequeridoDto;
import ar.edu.utn.frc.previsar.dtos.SeccionDto;
import ar.edu.utn.frc.previsar.entities.DocumentoRequerido;
import ar.edu.utn.frc.previsar.entities.Seccion;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EstructuraMapper {
    DocumentoRequeridoDto toDto(DocumentoRequerido documento);

    SeccionDto toDto(Seccion seccion);          // mapea documentos con el método de arriba

    List<SeccionDto> toDtoList(List<Seccion> secciones);
}
