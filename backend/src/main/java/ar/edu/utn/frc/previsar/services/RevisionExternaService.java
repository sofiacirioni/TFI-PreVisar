package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.response.RevisionExternaDetalleDto;
import ar.edu.utn.frc.previsar.dtos.response.RevisionExternaResumenDto;
import ar.edu.utn.frc.previsar.dtos.response.RevisionMetricasDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Análisis de expedientes completos que sube un revisor.
 *
 * Todas las operaciones exigen que el usuario autenticado tenga rol de revisor
 * y operan solo sobre SUS propias revisiones (el historial es por usuario).
 */
public interface RevisionExternaService {

    /**
     * Valida el rol de revisor y el PDF (tipo y tamaño), lo almacena y crea la
     * revisión en estado EN_PROGRESO. Devuelve el id para que el controller
     * dispare el análisis asíncrono. NO llama a Gemini (eso es async).
     */
    Long crear(MultipartFile archivo);

    /**
     * Analiza el PDF con IA en segundo plano y persiste el resultado (COMPLETADO
     * con resumen, o ERROR con motivo visible). Se invoca cross-bean desde el
     * controller para que aplique el proxy @Async.
     */
    void analizarAsync(Long revisionId);

    /** Historial del revisor: sus revisiones, más nueva primero. */
    List<RevisionExternaResumenDto> listar();

    /**
     * Métricas agregadas del historial propio. Existe como endpoint aparte
     * porque los problemas detectados viven dentro del `resultado` de cada
     * revisión, que el listado no devuelve: sin esto el front tendría que pedir
     * el detalle de cada una para poder contarlos.
     */
    RevisionMetricasDto metricas();

    /** Detalle de una revisión propia (con el resumen). 404 ante revisiones ajenas. */
    RevisionExternaDetalleDto obtener(Long id);

    /** Elimina una revisión propia y su PDF. 404 ante revisiones ajenas. */
    void eliminar(Long id);
}
