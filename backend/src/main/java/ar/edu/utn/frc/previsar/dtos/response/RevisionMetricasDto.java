package ar.edu.utn.frc.previsar.dtos.response;

import java.util.List;

/**
 * Métricas agregadas de las revisiones del revisor autenticado.
 *
 * Los conteos por estado también se podrían calcular en el front sobre el
 * listado, pero `problemasPorTipo` no: los problemas viven dentro del
 * `resultado` (jsonb) de cada revisión y el listado no lo devuelve. Se agrega
 * todo acá para que el panel resuelva sus métricas con una sola llamada.
 */
public record RevisionMetricasDto(
        long totalAnalizados,
        long completados,
        long conError,
        long enProgreso,

        /** Cuántos problemas detectó la IA de cada tipo, de mayor a menor. */
        List<ConteoDto> problemasPorTipo,

        /**
         * Promedio de problemas por expediente analizado con éxito.
         * Null si todavía no hay ninguno completado: un promedio sobre cero
         * no es cero, es "no hay dato".
         */
        Double promedioProblemas
) {
    /** Un par etiqueta/cantidad, listo para dibujar. */
    public record ConteoDto(String etiqueta, long cantidad) {}
}
