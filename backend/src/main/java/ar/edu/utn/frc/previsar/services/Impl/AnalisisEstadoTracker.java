package ar.edu.utn.frc.previsar.services.Impl;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnalisisEstadoTracker {
    public enum Estado { EN_PROGRESO, COMPLETADO, COMPLETADO_CON_ERRORES, ERROR }

    /** Estado + detalle opcional (motivo cuando hubo errores). */
    public record Resultado(Estado estado, String detalle) {}

    /** El análisis es por (expediente, sección): una sección corre y se sigue de forma independiente. */
    private record Clave(Long expedienteId, Long seccionId) {}

    private final Map<Clave, Resultado> estados = new ConcurrentHashMap<>();

    /**
     * Marca la sección como EN_PROGRESO solo si no lo estaba ya (atómico).
     * @return true si tomó el análisis; false si esa sección ya está corriendo → evita doble disparo.
     */
    public boolean iniciarSiLibre(Long expedienteId, Long seccionId) {
        boolean[] tomado = {false};
        estados.compute(new Clave(expedienteId, seccionId), (k, actual) -> {
            if (actual != null && actual.estado() == Estado.EN_PROGRESO) return actual;   // ocupada
            tomado[0] = true;
            return new Resultado(Estado.EN_PROGRESO, null);
        });
        return tomado[0];
    }

    public void completar(Long expedienteId, Long seccionId) {
        estados.put(new Clave(expedienteId, seccionId), new Resultado(Estado.COMPLETADO, null));
    }

    public void completarConErrores(Long expedienteId, Long seccionId, String detalle) {
        estados.put(new Clave(expedienteId, seccionId), new Resultado(Estado.COMPLETADO_CON_ERRORES, detalle));
    }

    public void error(Long expedienteId, Long seccionId, String detalle) {
        estados.put(new Clave(expedienteId, seccionId), new Resultado(Estado.ERROR, detalle));
    }

    public Resultado resultado(Long expedienteId, Long seccionId) {
        return estados.get(new Clave(expedienteId, seccionId));
    }
}
