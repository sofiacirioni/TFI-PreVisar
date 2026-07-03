package ar.edu.utn.frc.previsar.services.Impl;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AnalisisEstadoTracker {
    public enum Estado { EN_PROGRESO, COMPLETADO, ERROR }
    private final Map<Long, Estado> estados = new ConcurrentHashMap<>();

    /**
     * Marca el expediente como EN_PROGRESO solo si no lo estaba ya (atómico).
     * @return true si tomó el análisis; false si otro ya está corriendo → evita doble disparo.
     */
    public boolean iniciarSiLibre(Long id) {
        // compute es atómico por clave: se permite re-analizar tras COMPLETADO/ERROR, no mientras corre.
        boolean[] tomado = {false};
        estados.compute(id, (k, actual) -> {
            if (actual == Estado.EN_PROGRESO) return actual;   // ocupado
            tomado[0] = true;
            return Estado.EN_PROGRESO;
        });
        return tomado[0];
    }

    public void completar(Long id) { estados.put(id, Estado.COMPLETADO); }
    public void error(Long id)     { estados.put(id, Estado.ERROR); }
    public Estado estado(Long id)  { return estados.get(id); }
}
