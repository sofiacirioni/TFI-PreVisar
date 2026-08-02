package ar.edu.utn.frc.previsar.services.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests del seguimiento del análisis con IA.
 *
 * Lo importante es el gate: mientras una ranura se está analizando, un segundo
 * disparo tiene que rebotar (el controller devuelve 409). Sin eso, apretar dos
 * veces "Analizar" lanzaba dos llamadas a Gemini por el mismo documento.
 */
class AnalisisEstadoTrackerTest {

    private static final Long EXPEDIENTE = 500L;
    private static final Long RANURA = 70L;

    private AnalisisEstadoTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new AnalisisEstadoTracker();
    }

    @Test
    @DisplayName("Una ranura sin análisis previo no tiene resultado")
    void sinAnalisisNoHayResultado() {
        assertThat(tracker.resultado(EXPEDIENTE, RANURA)).isNull();
    }

    @Test
    @DisplayName("El primer disparo toma la ranura y la deja EN_PROGRESO")
    void primerDisparoTomaLaRanura() {
        assertThat(tracker.iniciarSiLibre(EXPEDIENTE, RANURA)).isTrue();
        assertThat(tracker.resultado(EXPEDIENTE, RANURA).estado())
                .isEqualTo(AnalisisEstadoTracker.Estado.EN_PROGRESO);
    }

    @Test
    @DisplayName("Un segundo disparo sobre la misma ranura rebota")
    void segundoDisparoRebota() {
        tracker.iniciarSiLibre(EXPEDIENTE, RANURA);

        assertThat(tracker.iniciarSiLibre(EXPEDIENTE, RANURA)).isFalse();
    }

    @Test
    @DisplayName("Cada ranura se sigue por separado")
    void ranurasDistintasSonIndependientes() {
        tracker.iniciarSiLibre(EXPEDIENTE, RANURA);

        assertThat(tracker.iniciarSiLibre(EXPEDIENTE, 71L)).isTrue();
    }

    @Test
    @DisplayName("El mismo número de ranura en otro expediente también es independiente")
    void expedientesDistintosSonIndependientes() {
        tracker.iniciarSiLibre(EXPEDIENTE, RANURA);

        assertThat(tracker.iniciarSiLibre(501L, RANURA)).isTrue();
    }

    @Test
    @DisplayName("Al completar se libera la ranura y queda el detalle de lo encontrado")
    void completarLiberaYGuardaElDetalle() {
        tracker.iniciarSiLibre(EXPEDIENTE, RANURA);

        tracker.completar(EXPEDIENTE, RANURA, "La IA detectó 2 observaciones.");

        var resultado = tracker.resultado(EXPEDIENTE, RANURA);
        assertThat(resultado.estado()).isEqualTo(AnalisisEstadoTracker.Estado.COMPLETADO);
        assertThat(resultado.detalle()).isEqualTo("La IA detectó 2 observaciones.");
        // Ya no está en progreso: se puede volver a analizar.
        assertThat(tracker.iniciarSiLibre(EXPEDIENTE, RANURA)).isTrue();
    }

    @Test
    @DisplayName("Un fallo deja el motivo visible y libera la ranura")
    void errorLiberaYGuardaElMotivo() {
        tracker.iniciarSiLibre(EXPEDIENTE, RANURA);

        tracker.error(EXPEDIENTE, RANURA, "El análisis de IA no se pudo completar");

        var resultado = tracker.resultado(EXPEDIENTE, RANURA);
        assertThat(resultado.estado()).isEqualTo(AnalisisEstadoTracker.Estado.ERROR);
        assertThat(resultado.detalle()).isEqualTo("El análisis de IA no se pudo completar");
        assertThat(tracker.iniciarSiLibre(EXPEDIENTE, RANURA)).isTrue();
    }

    @Test
    @DisplayName("Completar con errores parciales conserva el detalle de qué falló")
    void completarConErroresConservaElDetalle() {
        tracker.iniciarSiLibre(EXPEDIENTE, RANURA);

        tracker.completarConErrores(EXPEDIENTE, RANURA, "No se pudo analizar 1 documento(s): plano.pdf");

        var resultado = tracker.resultado(EXPEDIENTE, RANURA);
        assertThat(resultado.estado()).isEqualTo(AnalisisEstadoTracker.Estado.COMPLETADO_CON_ERRORES);
        assertThat(resultado.detalle()).contains("plano.pdf");
    }
}
