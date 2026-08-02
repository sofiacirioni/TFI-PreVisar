package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.request.ComitenteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ComitenteResponseDto;

import java.util.List;
import java.util.Optional;

/**
 * Operaciones sobre la cartera de Comitentes del profesional autenticado.
 *
 * Todas las operaciones se aplican implícitamente al profesional que hace
 * la request; nunca se pasa profesionalId en los métodos. Esto evita que
 * un profesional pueda operar sobre la cartera de otro.
 */

public interface ComitenteService {
    /**
     * Lista todos los comitentes activos (no eliminados) del profesional actual.
     */
    List<ComitenteResponseDto> listar();

    /**
     * Devuelve un comitente específico por id. Falla si no pertenece al
     * profesional actual o si está eliminado.
     */
    ComitenteResponseDto obtenerPorId(Long id);

    /**
     * Busca un comitente activo por DNI/CUIT en la cartera del profesional
     * actual. Devuelve vacío si no existe (lo cual indica al frontend que
     * puede crear uno nuevo).
     */
    Optional<ComitenteResponseDto> buscarPorDniCuit(String dniCuit);

    /**
     * Búsqueda incremental por DNI/CUIT dentro de la cartera propia.
     *
     * Normaliza el fragmento (ignora guiones, puntos y espacios) y lo compara
     * contra el DNI/CUIT también normalizado, porque los DNI se guardan sin
     * separadores y los CUIT con ellos. Devuelve lista vacía con menos de 3
     * dígitos, para no traer media cartera en cada tecla.
     */
    List<ComitenteResponseDto> buscarPorFragmentoDniCuit(String fragmento);

    /**
     * Crea un nuevo comitente en la cartera del profesional actual.
     */
    ComitenteResponseDto crear(ComitenteRequestDto request);

    /**
     * Modifica un comitente existente del profesional actual.
     */
    ComitenteResponseDto actualizar(Long id, ComitenteRequestDto request);

    /**
     * Soft delete: marca el comitente como eliminado sin borrarlo físicamente.
     * Preserva la integridad de expedientes históricos.
     */
    void eliminar(Long id);
}
