package ar.edu.utn.frc.previsar.services.Impl;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnalisisEstadoTracker {
    public enum Estado { EN_PROGRESO, COMPLETADO, COMPLETADO_CON_ERRORES, ERROR }

    /** Estado + detalle opcional (motivo cuando hubo errores). */
    public record Resultado(Estado estado, String detalle) {}

    /** El análisis es por (expediente, ranura): cada ranura corre y se sigue de forma independiente. */
    private record Clave(Long expedienteId, Long documentoRequeridoId) {}

    private final Map<Clave, Resultado> estados = new ConcurrentHashMap<>();

    /**
     * Marca la ranura como EN_PROGRESO solo si no lo estaba ya (atómico).
     * @return true si tomó el análisis; false si esa ranura ya está corriendo → evita doble disparo.
     */
    public boolean iniciarSiLibre(Long expedienteId, Long documentoRequeridoId) {
        boolean[] tomado = {false};
        estados.compute(new Clave(expedienteId, documentoRequeridoId), (k, actual) -> {
            if (actual != null && actual.estado() == Estado.EN_PROGRESO) return actual;   // ocupada
            tomado[0] = true;
            return new Resultado(Estado.EN_PROGRESO, null);
        });
        return tomado[0];
    }

    /** El detalle cuenta QUÉ encontró (o que no había nada que analizar): sin eso, un
     *  análisis exitoso sin observaciones es indistinguible de uno que falló en silencio. */
    public void completar(Long expedienteId, Long documentoRequeridoId, String detalle) {
        estados.put(new Clave(expedienteId, documentoRequeridoId), new Resultado(Estado.COMPLETADO, detalle));
    }

    public void completarConErrores(Long expedienteId, Long documentoRequeridoId, String detalle) {
        estados.put(new Clave(expedienteId, documentoRequeridoId), new Resultado(Estado.COMPLETADO_CON_ERRORES, detalle));
    }

    public void error(Long expedienteId, Long documentoRequeridoId, String detalle) {
        estados.put(new Clave(expedienteId, documentoRequeridoId), new Resultado(Estado.ERROR, detalle));
    }

    public Resultado resultado(Long expedienteId, Long documentoRequeridoId) {
        return estados.get(new Clave(expedienteId, documentoRequeridoId));
    }
}
