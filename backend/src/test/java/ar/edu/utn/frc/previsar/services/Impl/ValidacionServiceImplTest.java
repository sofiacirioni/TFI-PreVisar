package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.DocumentoValidadoDto;
import ar.edu.utn.frc.previsar.dtos.ObservacionDto;
import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;
import ar.edu.utn.frc.previsar.enums.NivelObservacion;
import ar.edu.utn.frc.previsar.enums.OrigenObservacion;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.services.ValidadorExpediente;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests de la orquestación de la pre-validación.
 *
 * ValidacionServiceImpl recibe todos los ValidadorExpediente por inyección de lista
 * y fusiona sus resultados. Lo que se prueba acá es esa fusión: que las
 * observaciones de distintos niveles sobre el MISMO documento terminen juntas y no
 * se pisen entre sí.
 *
 * Se usan validadores de mentira en vez de mocks: son dos métodos, y así el test
 * dice exactamente qué devuelve cada nivel.
 */
class ValidacionServiceImplTest {

    /** Validador de prueba: devuelve un resultado fijo en el nivel indicado. */
    private record ValidadorFalso(int nivel, ValidacionResultadoDto resultado)
            implements ValidadorExpediente {
        @Override
        public ValidacionResultadoDto validar(Long expedienteId) {
            return resultado;
        }
    }

    private ObservacionDto obs(String codigo, OrigenObservacion origen) {
        return new ObservacionDto(codigo, NivelObservacion.ADVERTENCIA, origen, "detalle de " + codigo);
    }

    private DocumentoValidadoDto doc(Long id, ObservacionDto... observaciones) {
        return new DocumentoValidadoDto(id, 70L, "contrato.pdf", List.of(observaciones));
    }

    @Test
    @DisplayName("validarTodo fusiona en un solo documento las observaciones de los tres niveles")
    void fusionaObservacionesDelMismoDocumento() {
        var nivel1 = new ValidadorFalso(1, new ValidacionResultadoDto(
                List.of(doc(900L, obs("NO_A4", OrigenObservacion.DETERMINISTICO))), List.of()));
        var nivel2 = new ValidadorFalso(2, new ValidacionResultadoDto(
                List.of(doc(900L, obs("CUIT_NO_COINCIDE", OrigenObservacion.COHERENCIA))), List.of()));
        var nivel3 = new ValidadorFalso(3, new ValidacionResultadoDto(
                List.of(doc(900L, obs("IA_FIRMA", OrigenObservacion.IA_VISUAL))), List.of()));

        var servicio = new ValidacionServiceImpl(List.of(nivel1, nivel2, nivel3));

        ValidacionResultadoDto resultado = servicio.validarTodo(500L);

        assertThat(resultado.documentos()).hasSize(1);
        assertThat(resultado.documentos().get(0).observaciones())
                .extracting(ObservacionDto::codigo)
                .containsExactly("NO_A4", "CUIT_NO_COINCIDE", "IA_FIRMA");
    }

    @Test
    @DisplayName("Los documentos distintos se mantienen separados")
    void mantieneSeparadosLosDocumentosDistintos() {
        var nivel1 = new ValidadorFalso(1, new ValidacionResultadoDto(
                List.of(doc(900L, obs("NO_A4", OrigenObservacion.DETERMINISTICO))), List.of()));
        var nivel2 = new ValidadorFalso(2, new ValidacionResultadoDto(
                List.of(doc(901L, obs("SIN_MONTOS", OrigenObservacion.COHERENCIA))), List.of()));

        var servicio = new ValidacionServiceImpl(List.of(nivel1, nivel2));

        ValidacionResultadoDto resultado = servicio.validarTodo(500L);

        assertThat(resultado.documentos())
                .extracting(DocumentoValidadoDto::documentoCargadoId)
                .containsExactly(900L, 901L);
    }

    @Test
    @DisplayName("Las observaciones generales de todos los niveles se acumulan")
    void acumulaLasObservacionesGenerales() {
        var nivel1 = new ValidacionResultadoDto(List.of(), List.of(
                obs("EXCEDE_32MB", OrigenObservacion.DETERMINISTICO),
                obs("DUPLICADO", OrigenObservacion.DETERMINISTICO)));
        var servicio = new ValidacionServiceImpl(List.of(
                new ValidadorFalso(1, nivel1),
                new ValidadorFalso(2, new ValidacionResultadoDto(List.of(), List.of()))));

        ValidacionResultadoDto resultado = servicio.validarTodo(500L);

        assertThat(resultado.generales()).hasSize(2);
        assertThat(resultado.tieneObservaciones()).isTrue();
    }

    @Test
    @DisplayName("Un expediente sin problemas no reporta observaciones")
    void sinProblemasNoHayObservaciones() {
        var servicio = new ValidacionServiceImpl(List.of(
                new ValidadorFalso(1, new ValidacionResultadoDto(List.of(), List.of()))));

        assertThat(servicio.validarTodo(500L).tieneObservaciones()).isFalse();
    }

    @Test
    @DisplayName("validarNivel corre solo el nivel pedido")
    void validarNivelCorreSoloEseNivel() {
        var nivel1 = new ValidadorFalso(1, new ValidacionResultadoDto(
                List.of(doc(900L, obs("NO_A4", OrigenObservacion.DETERMINISTICO))), List.of()));
        var nivel2 = new ValidadorFalso(2, new ValidacionResultadoDto(
                List.of(doc(901L, obs("SIN_MONTOS", OrigenObservacion.COHERENCIA))), List.of()));

        var servicio = new ValidacionServiceImpl(List.of(nivel1, nivel2));

        ValidacionResultadoDto resultado = servicio.validarNivel(500L, 2);

        assertThat(resultado.documentos())
                .extracting(DocumentoValidadoDto::documentoCargadoId)
                .containsExactly(901L);
    }

    @Test
    @DisplayName("Pedir un nivel que no existe es un error de negocio")
    void nivelInexistenteFalla() {
        var servicio = new ValidacionServiceImpl(List.of(
                new ValidadorFalso(1, new ValidacionResultadoDto(List.of(), List.of()))));

        assertThatThrownBy(() -> servicio.validarNivel(500L, 9))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Nivel de validación inexistente");
    }
}
