package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.DocumentoRequeridoDto;
import ar.edu.utn.frc.previsar.dtos.EstructuraExpedienteDto;
import ar.edu.utn.frc.previsar.dtos.SeccionDto;
import ar.edu.utn.frc.previsar.dtos.request.DocumentoRequeridoRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.SeccionRequestDto;

/**
 * Administración de la estructura documental (secciones y documentos requeridos)
 * de un (provincia, tipo_tarea).
 *
 * La lectura es pública para cualquier usuario autenticado; las operaciones de
 * escritura solo las puede hacer un revisor, y únicamente sobre la estructura de
 * la provincia que ocupa (ver RolRevisor).
 */
public interface EstructuraService {

    /** Estructura vigente de un tipo de tarea en una provincia. */
    EstructuraExpedienteDto obtenerEstructura(Long tipoTareaId, Long provinciaId);

    SeccionDto crearSeccion(SeccionRequestDto request);

    SeccionDto actualizarSeccion(Long seccionId, SeccionRequestDto request);

    /** Baja lógica de la sección y de sus documentos. */
    void eliminarSeccion(Long seccionId);

    DocumentoRequeridoDto crearDocumento(Long seccionId, DocumentoRequeridoRequestDto request);

    DocumentoRequeridoDto actualizarDocumento(Long documentoId, DocumentoRequeridoRequestDto request);

    void eliminarDocumento(Long documentoId);

    /**
     * Copia la estructura de {@code provinciaOrigenId} hacia la provincia del
     * revisor autenticado. Devuelve la cantidad de secciones clonadas.
     */
    int clonarEstructura(Long tipoTareaId, Long provinciaOrigenId);
}
