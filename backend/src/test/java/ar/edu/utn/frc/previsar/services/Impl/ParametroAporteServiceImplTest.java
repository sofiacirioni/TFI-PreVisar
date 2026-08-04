package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.request.ActualizarArancelRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ArancelResponseDto;
import ar.edu.utn.frc.previsar.entities.ConceptoAporte;
import ar.edu.utn.frc.previsar.entities.ParametroAporte;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.enums.TipoValor;
import ar.edu.utn.frc.previsar.exception.ForbiddenException;
import ar.edu.utn.frc.previsar.repositories.ConceptoAporteRepository;
import ar.edu.utn.frc.previsar.repositories.ParametroAporteRepository;
import ar.edu.utn.frc.previsar.repositories.RolRevisorRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la actualización del arancel administrativo.
 *
 * No es un UPDATE: se cierra el valor vigente y se abre uno nuevo, para conservar
 * el histórico. El caso delicado es corregir el arancel DOS VECES el mismo día:
 * cerrar el vigente con vigencia_hasta = ayer dejaría hasta < desde y rompería el
 * CHECK chk_parametro_vigencia.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ParametroAporteServiceImplTest {

    @Mock private ParametroAporteRepository parametroAporteRepository;
    @Mock private ConceptoAporteRepository conceptoAporteRepository;
    @Mock private RolRevisorRepository rolRevisorRepository;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private ParametroAporteServiceImpl parametroService;

    private ConceptoAporte arancel;

    @BeforeEach
    void setUp() {
        Profesional revisor = Profesional.builder().id(1L).build();
        when(securityUtils.getProfesionalActual()).thenReturn(revisor);
        when(rolRevisorRepository.existsByProfesionalId(1L)).thenReturn(true);

        arancel = new ConceptoAporte();
        arancel.setId(2L);
        arancel.setCodigo("ARANCEL_ADMIN");
        when(conceptoAporteRepository.findByCodigo("ARANCEL_ADMIN")).thenReturn(Optional.of(arancel));
    }

    private ActualizarArancelRequestDto pedido(String valor) {
        ActualizarArancelRequestDto dto = new ActualizarArancelRequestDto();
        dto.setValor(new BigDecimal(valor));
        return dto;
    }

    private ParametroAporte vigenteDesde(LocalDate desde, String valor) {
        ParametroAporte p = new ParametroAporte();
        p.setConcepto(arancel);
        p.setTipoValor(TipoValor.FIJO);
        p.setValor(new BigDecimal(valor));
        p.setVigenciaDesde(desde);
        p.setVigenciaHasta(null);
        p.setActivo(true);
        return p;
    }

    // -------------------- Lectura --------------------

    @Test
    @DisplayName("Devuelve el valor vigente con su fecha de inicio")
    void devuelveElArancelVigente() {
        LocalDate desde = LocalDate.now().minusMonths(2);
        when(parametroAporteRepository.findVigente(any(), any()))
                .thenReturn(Optional.of(vigenteDesde(desde, "19000")));

        ArancelResponseDto respuesta = parametroService.obtenerArancelVigente();

        assertThat(respuesta.getValor()).isEqualByComparingTo("19000");
        assertThat(respuesta.getVigenciaDesde()).isEqualTo(desde);
    }

    @Test
    @DisplayName("Sin arancel cargado devuelve un DTO vacío en vez de fallar")
    void sinArancelDevuelveVacio() {
        when(parametroAporteRepository.findVigente(any(), any())).thenReturn(Optional.empty());

        ArancelResponseDto respuesta = parametroService.obtenerArancelVigente();

        assertThat(respuesta.getValor()).isNull();
        assertThat(respuesta.getVigenciaDesde()).isNull();
    }

    // -------------------- Autorización --------------------

    @Test
    @DisplayName("Un profesional sin rol revisor no puede tocar el arancel")
    void soloRevisorPuedeActualizar() {
        when(rolRevisorRepository.existsByProfesionalId(1L)).thenReturn(false);

        assertThatThrownBy(() -> parametroService.actualizarArancel(pedido("25000")))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("Solo un revisor");

        verify(parametroAporteRepository, never()).save(any(ParametroAporte.class));
    }

    // -------------------- Vigencias --------------------

    @Test
    @DisplayName("Con un vigente anterior: se cierra ayer y se abre uno nuevo desde hoy")
    void cierraElAnteriorYAbreUnoNuevo() {
        LocalDate hoy = LocalDate.now();
        ParametroAporte anterior = vigenteDesde(hoy.minusMonths(2), "19000");
        when(parametroAporteRepository.findVigente(any(), any())).thenReturn(Optional.of(anterior));

        parametroService.actualizarArancel(pedido("25000"));

        assertThat(anterior.getVigenciaHasta()).isEqualTo(hoy.minusDays(1));

        ArgumentCaptor<ParametroAporte> captor = ArgumentCaptor.forClass(ParametroAporte.class);
        verify(parametroAporteRepository).save(captor.capture());

        ParametroAporte nuevo = captor.getValue();
        assertThat(nuevo.getValor()).isEqualByComparingTo("25000");
        assertThat(nuevo.getVigenciaDesde()).isEqualTo(hoy);
        assertThat(nuevo.getVigenciaHasta()).isNull();       // null = vigente
        assertThat(nuevo.getTipoValor()).isEqualTo(TipoValor.FIJO);
        assertThat(nuevo.getBaseCalculo()).isNull();          // un FIJO no tiene base
        assertThat(nuevo.isActivo()).isTrue();
    }

    @Test
    @DisplayName("El cierre del anterior se descarga a la base ANTES de insertar el nuevo")
    void cierraElAnteriorConFlushAntesDeInsertar() {
        LocalDate hoy = LocalDate.now();
        ParametroAporte anterior = vigenteDesde(hoy.minusMonths(2), "19000");
        when(parametroAporteRepository.findVigente(any(), any())).thenReturn(Optional.of(anterior));

        parametroService.actualizarArancel(pedido("25000"));

        // El índice uq_parametro_vigente (V031) admite una sola fila vigente por
        // concepto, y el id es IDENTITY: el persist del nuevo dispara su INSERT al
        // instante. Sin el flush explícito, el UPDATE que cierra el anterior queda
        // encolado hasta el commit, el INSERT llega primero y la base rechaza la
        // operación con un 409. El orden es parte del contrato, no un detalle.
        InOrder orden = inOrder(parametroAporteRepository);
        orden.verify(parametroAporteRepository).saveAndFlush(anterior);
        orden.verify(parametroAporteRepository).save(any(ParametroAporte.class));
    }

    @Test
    @DisplayName("Corregir el arancel dos veces el mismo día edita la fila de hoy, no crea otra")
    void correccionElMismoDiaNoGeneraHistoriaIntradia() {
        LocalDate hoy = LocalDate.now();
        ParametroAporte fijadoHoy = vigenteDesde(hoy, "25000");
        when(parametroAporteRepository.findVigente(any(), any())).thenReturn(Optional.of(fijadoHoy));

        parametroService.actualizarArancel(pedido("26000"));

        assertThat(fijadoHoy.getValor()).isEqualByComparingTo("26000");
        // Clave: no se le pone vigencia_hasta = ayer (quedaría hasta < desde y
        // violaría chk_parametro_vigencia), y no se inserta una segunda fila.
        assertThat(fijadoHoy.getVigenciaHasta()).isNull();
        verify(parametroAporteRepository, times(1)).save(any(ParametroAporte.class));
    }

    @Test
    @DisplayName("Sin arancel previo simplemente se inserta el primero")
    void sinVigentePreviosSoloInserta() {
        when(parametroAporteRepository.findVigente(any(), any())).thenReturn(Optional.empty());

        parametroService.actualizarArancel(pedido("19000"));

        ArgumentCaptor<ParametroAporte> captor = ArgumentCaptor.forClass(ParametroAporte.class);
        verify(parametroAporteRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getValor()).isEqualByComparingTo("19000");
        assertThat(captor.getValue().getVigenciaDesde()).isEqualTo(LocalDate.now());
    }
}
