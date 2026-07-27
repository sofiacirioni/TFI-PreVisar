package ar.edu.utn.frc.previsar.mapper;

import ar.edu.utn.frc.previsar.dtos.DocumentoRequeridoDto;
import ar.edu.utn.frc.previsar.dtos.SeccionDto;
import ar.edu.utn.frc.previsar.entities.DocumentoRequerido;
import ar.edu.utn.frc.previsar.entities.Seccion;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EstructuraMapper {
    // desactivado = el documento está inactivo. La query por expediente solo incluye
    // inactivos que tienen archivo cargado, así que ahí !activo ⟺ "retirado pero con archivo".
    // En la lectura por provincia solo vienen activos, por lo que queda en false.
    @Mapping(target = "desactivado", expression = "java(!documento.isActivo())")
    DocumentoRequeridoDto toDto(DocumentoRequerido documento);

    SeccionDto toDto(Seccion seccion);          // mapea documentos con el método de arriba

    List<SeccionDto> toDtoList(List<Seccion> secciones);
}
