package ar.edu.utn.frc.previsar.services.Impl;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnalisisEstadoTracker {
    public enum Estado { EN_PROGRESO, COMPLETADO, COMPLETADO_CON_ERRORES, ERROR }

    /** Estado + detalle opcional (motivo cuando hubo errores). */
    public record Resultado(Estado estado, String detalle) {}

    private final Map<Long, Resultado> estados = new ConcurrentHashMap<>();

    /**
     * Marca el expediente como EN_PROGRESO solo si no lo estaba ya (atómico).
     * @return true si tomó el análisis; false si otro ya está corriendo → evita doble disparo.
     */
    public boolean iniciarSiLibre(Long id) {
        // compute es atómico por clave: se permite re-analizar tras un estado terminal, no mientras corre.
        boolean[] tomado = {false};
        estados.compute(id, (k, actual) -> {
            if (actual != null && actual.estado() == Estado.EN_PROGRESO) return actual;   // ocupado
            tomado[0] = true;
            return new Resultado(Estado.EN_PROGRESO, null);
        });
        return tomado[0];
    }

    public void completar(Long id) {
        estados.put(id, new Resultado(Estado.COMPLETADO, null));
    }

    public void completarConErrores(Long id, String detalle) {
        estados.put(id, new Resultado(Estado.COMPLETADO_CON_ERRORES, detalle));
    }

    public void error(Long id, String detalle) {
        estados.put(id, new Resultado(Estado.ERROR, detalle));
    }

    public Resultado resultado(Long id) {
        return estados.get(id);
    }
}
