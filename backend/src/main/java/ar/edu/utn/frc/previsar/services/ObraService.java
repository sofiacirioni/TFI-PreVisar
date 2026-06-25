package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.request.ObraRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ObraResponseDto;

import java.util.List;

/**
 * Operaciones sobre las obras del profesional autenticado.
 *
 * Las obras se acceden a través de los comitentes del profesional.
 * Cualquier operación valida que la obra pertenezca a un comitente
 * del profesional actual.
 */

public interface ObraService {
    /**
     * Lista todas las obras activas del profesional (de cualquier comitente).
     */
    List<ObraResponseDto> listarTodasMisObras();

    /**
     * Lista las obras activas de un comitente específico del profesional.
     */
    List<ObraResponseDto> listarPorComitente(Long comitenteId);

    /**
     * Obtiene una obra por id. Valida propiedad y que no esté eliminada.
     */
    ObraResponseDto obtenerPorId(Long id);

    /**
     * Crea una nueva obra para un comitente específico del profesional.
     */
    ObraResponseDto crear(Long comitenteId, ObraRequestDto request);

    /**
     * Modifica una obra existente del profesional.
     */
    ObraResponseDto actualizar(Long id, ObraRequestDto request);

    /**
     * Soft delete: marca la obra como eliminada.
     */
    void eliminar(Long id);
}
