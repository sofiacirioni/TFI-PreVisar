package ar.edu.utn.frc.previsar.enums;

/**
 * Estado del análisis de una {@link ar.edu.utn.frc.previsar.entities.RevisionExterna}.
 *
 * El estado vive en la entidad (no en un tracker en memoria) para que el
 * historial sobreviva un reinicio: el front consulta el detalle hasta que
 * deje de estar EN_PROGRESO.
 */
public enum EstadoRevision {
    /** El PDF se subió y el análisis con IA está corriendo en segundo plano. */
    EN_PROGRESO,
    /** El análisis terminó y hay un resumen disponible. */
    COMPLETADO,
    /** El análisis falló (PDF ilegible, Gemini caído, timeout). El motivo va en `detalle`. */
    ERROR
}
