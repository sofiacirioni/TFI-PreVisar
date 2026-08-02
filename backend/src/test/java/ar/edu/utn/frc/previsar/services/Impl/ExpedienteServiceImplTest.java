package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.AportesCalculadosDto;
import ar.edu.utn.frc.previsar.dtos.DatosContratoDto;
import ar.edu.utn.frc.previsar.dtos.request.CalcularAportesRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.ExpedienteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.AportesResponseDto;
import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;
import ar.edu.utn.frc.previsar.entities.Comitente;
import ar.edu.utn.frc.previsar.entities.Especialidad;
import ar.edu.utn.frc.previsar.entities.Expediente;
import ar.edu.utn.frc.previsar.entities.Obra;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.enums.EstadoArancel;
import ar.edu.utn.frc.previsar.enums.EstadoExpediente;
import ar.edu.utn.frc.previsar.enums.EstadoPago;
import ar.edu.utn.frc.previsar.enums.GrupoAporte;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.ExpedienteMapper;
import ar.edu.utn.frc.previsar.repositories.ExpedienteRepository;
import ar.edu.utn.frc.previsar.repositories.ObraRepository;
import ar.edu.utn.frc.previsar.repositories.PagoRepository;
import ar.edu.utn.frc.previsar.repositories.TipoTareaRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import ar.edu.utn.frc.previsar.services.AporteCalculatorService;
import ar.edu.utn.frc.previsar.services.PdfGenerationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de ExpedienteServiceImpl, el núcleo del armado.
 *
 * Foco:
 *   - Aislamiento por profesional (404, no 403, ante expedientes y obras ajenas).
 *   - Guardado parcial: el PATCH solo toca los campos que vienen.
 *   - Transición BORRADOR -> EN_PROCESO y qué datos exige.
 *   - Estado del arancel derivado de los pagos (no persistido).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpedienteServiceImplTest {

    @Mock private ExpedienteRepository expedienteRepository;
    @Mock private TipoTareaRepository tipoTareaRepository;
    @Mock private ObraRepository obraRepository;
    @Mock private AporteCalculatorService aporteCalculator;
    @Mock private ExpedienteMapper mapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private PdfGenerationService pdfGenerationService;
    @Mock private PagoRepository pagoRepository;

    @InjectMocks private ExpedienteServiceImpl expedienteService;

    private Profesional profesionalActual;
    private Profesional otroProfesional;
    private TipoTarea prDtRt;
    private Obra obraPropia;

    @BeforeEach
    void setUp() {
        profesionalActual = Profesional.builder().id(1L).nombre("Sofía").apellido("Cirioni").build();
        otroProfesional = Profesional.builder().id(2L).nombre("Otro").apellido("Profesional").build();
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);

        Especialidad electrica = new Especialidad();
        electrica.setId(1L);
        electrica.setCodigo("ELEC");

        prDtRt = new TipoTarea();
        prDtRt.setId(10L);
        prDtRt.setCodigo("PR-DT-RT");
        prDtRt.setEspecialidad(electrica);
        prDtRt.setActivo(true);
        when(tipoTareaRepository.findById(10L)).thenReturn(Optional.of(prDtRt));

        Comitente comitentePropio = Comitente.builder().id(100L).profesional(profesionalActual).build();
        obraPropia = Obra.builder().id(200L).comitente(comitentePropio).build();
        when(obraRepository.findById(200L)).thenReturn(Optional.of(obraPropia));

        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mapper.toResponse(any(Expediente.class))).thenReturn(new ExpedienteResponseDto());
    }

    /** Expediente propio ya existente, listo para las operaciones sobre un id. */
    private Expediente expedientePropio() {
        Expediente e = new Expediente();
        e.setId(500L);
        e.setProfesional(profesionalActual);
        e.setEstado(EstadoExpediente.BORRADOR);
        when(expedienteRepository.findByIdAndProfesionalIdAndActivoTrue(500L, 1L))
                .thenReturn(Optional.of(e));
        return e;
    }

    // -------------------- Alta y guardado parcial --------------------

    @Test
    @DisplayName("Se crea en BORRADOR y a nombre del profesional autenticado")
    void creaEnBorrador() {
        expedienteService.crear(ExpedienteRequestDto.builder().nombre("Obra Mykonos").build());

        Expediente guardado = capturarGuardado();
        assertThat(guardado.getEstado()).isEqualTo(EstadoExpediente.BORRADOR);
        assertThat(guardado.getProfesional()).isEqualTo(profesionalActual);
        assertThat(guardado.getNombre()).isEqualTo("Obra Mykonos");
    }

    @Test
    @DisplayName("El PATCH solo aplica los campos presentes: lo ausente no se pisa")
    void actualizarParcialNoPisaLoAusente() {
        Expediente e = expedientePropio();
        e.setNombre("Nombre original");
        e.setHonorariosReferenciales(new BigDecimal("100000"));
        e.setTipoTarea(prDtRt);

        // Solo viene el nombre: honorarios y tipo de tarea tienen que sobrevivir.
        expedienteService.actualizarParcial(500L, ExpedienteRequestDto.builder()
                .nombre("Nombre nuevo")
                .build());

        assertThat(e.getNombre()).isEqualTo("Nombre nuevo");
        assertThat(e.getHonorariosReferenciales()).isEqualByComparingTo("100000");
        assertThat(e.getTipoTarea()).isEqualTo(prDtRt);
    }

    @Test
    @DisplayName("El PATCH acepta obra propia, tipo de tarea y honorarios")
    void actualizarParcialAplicaLoQueViene() {
        Expediente e = expedientePropio();

        expedienteService.actualizarParcial(500L, ExpedienteRequestDto.builder()
                .obraId(200L)
                .tipoTareaId(10L)
                .honorariosReferenciales(new BigDecimal("5623389.04"))
                .build());

        assertThat(e.getObra()).isEqualTo(obraPropia);
        assertThat(e.getTipoTarea()).isEqualTo(prDtRt);
        assertThat(e.getHonorariosReferenciales()).isEqualByComparingTo("5623389.04");
    }

    @Test
    @DisplayName("No se puede enganchar una obra de otro profesional: 404, no 403")
    void noSePuedeUsarObraAjena() {
        expedientePropio();
        Comitente comitenteAjeno = Comitente.builder().id(101L).profesional(otroProfesional).build();
        Obra obraAjena = Obra.builder().id(201L).comitente(comitenteAjeno).build();
        when(obraRepository.findById(201L)).thenReturn(Optional.of(obraAjena));

        assertThatThrownBy(() -> expedienteService.actualizarParcial(500L,
                ExpedienteRequestDto.builder().obraId(201L).build()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Obra no encontrada");
    }

    @Test
    @DisplayName("No se puede usar un tipo de tarea dado de baja")
    void noSePuedeUsarTipoTareaInactivo() {
        expedientePropio();
        TipoTarea discontinuado = new TipoTarea();
        discontinuado.setId(11L);
        discontinuado.setActivo(false);
        when(tipoTareaRepository.findById(11L)).thenReturn(Optional.of(discontinuado));

        assertThatThrownBy(() -> expedienteService.actualizarParcial(500L,
                ExpedienteRequestDto.builder().tipoTareaId(11L).build()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no esta disponible");
    }

    // -------------------- Generar (BORRADOR -> EN_PROCESO) --------------------

    @Test
    @DisplayName("Generar exige obra, tipo de tarea y honorarios, y los enumera en el error")
    void completarEnumeraLoQueFalta() {
        expedientePropio();   // borrador vacío

        assertThatThrownBy(() -> expedienteService.completar(500L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("obra")
                .hasMessageContaining("tipo de tarea")
                .hasMessageContaining("honorarios referenciales");
    }

    @Test
    @DisplayName("Generar avisa solo por lo que realmente falta")
    void completarAvisaSoloLoFaltante() {
        Expediente e = expedientePropio();
        e.setObra(obraPropia);
        e.setTipoTarea(prDtRt);
        // faltan los honorarios

        assertThatThrownBy(() -> expedienteService.completar(500L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("honorarios referenciales")
                .hasMessageNotContaining("tipo de tarea");
    }

    @Test
    @DisplayName("Con todos los datos, el expediente pasa a EN_PROCESO")
    void completarPasaAEnProceso() {
        Expediente e = expedientePropio();
        e.setObra(obraPropia);
        e.setTipoTarea(prDtRt);
        e.setHonorariosReferenciales(new BigDecimal("5623389.04"));

        expedienteService.completar(500L);

        assertThat(e.getEstado()).isEqualTo(EstadoExpediente.EN_PROCESO);
    }

    // -------------------- Aislamiento y baja --------------------

    @Test
    @DisplayName("Un expediente ajeno da 404 (no revela que existe)")
    void expedienteAjenoDa404() {
        when(expedienteRepository.findByIdAndProfesionalIdAndActivoTrue(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> expedienteService.obtener(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Expediente no encontrado");
    }

    @Test
    @DisplayName("El borrado es lógico: se marca activo = false")
    void eliminarEsSoftDelete() {
        Expediente e = expedientePropio();

        expedienteService.eliminar(500L);

        assertThat(e.isActivo()).isFalse();
        verify(expedienteRepository).save(e);
        verify(expedienteRepository, never()).delete(any(Expediente.class));
    }

    @Test
    @DisplayName("verificarPropio no lanza sobre un expediente propio")
    void verificarPropioAceptaElPropio() {
        expedientePropio();

        expedienteService.verificarPropio(500L);   // no debe lanzar
    }

    // -------------------- Listado y estado del arancel --------------------

    @Test
    @DisplayName("Sin expedientes devuelve lista vacía sin consultar los pagos")
    void listadoVacioNoConsultaPagos() {
        when(expedienteRepository.findByProfesionalIdAndActivoTrueOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of());

        assertThat(expedienteService.listarMisExpedientes()).isEmpty();
        verify(pagoRepository, never()).findExpedienteIdsConEstado(any(), any());
    }

    @Test
    @DisplayName("El listado deriva el estado del arancel de los pagos de todo el lote")
    void listadoDerivaEstadoArancel() {
        Expediente conPago = new Expediente();
        conPago.setId(500L);
        Expediente sinPago = new Expediente();
        sinPago.setId(501L);

        when(expedienteRepository.findByProfesionalIdAndActivoTrueOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(conPago, sinPago));
        when(pagoRepository.findExpedienteIdsConEstado(List.of(500L, 501L), EstadoPago.APROBADO))
                .thenReturn(Set.of(500L));
        when(pagoRepository.findExpedienteIdsConEstado(List.of(500L, 501L), EstadoPago.PENDIENTE))
                .thenReturn(Set.of());
        when(mapper.toResponse(any(Expediente.class)))
                .thenAnswer(inv -> new ExpedienteResponseDto());

        List<ExpedienteResponseDto> resultado = expedienteService.listarMisExpedientes();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getEstadoArancel()).isEqualTo(EstadoArancel.APROBADO);
        assertThat(resultado.get(1).getEstadoArancel()).isEqualTo(EstadoArancel.NINGUNO);
    }

    @Test
    @DisplayName("Un pago pendiente deja el arancel en PENDIENTE")
    void pagoPendienteDejaArancelPendiente() {
        expedientePropio();
        when(pagoRepository.existsByExpedienteIdAndEstado(500L, EstadoPago.APROBADO)).thenReturn(false);
        when(pagoRepository.existsByExpedienteIdAndEstado(500L, EstadoPago.PENDIENTE)).thenReturn(true);

        assertThat(expedienteService.obtener(500L).getEstadoArancel())
                .isEqualTo(EstadoArancel.PENDIENTE);
    }

    // -------------------- Cálculo de aportes --------------------

    @Test
    @DisplayName("Calcular aportes exige tipo de tarea y honorarios")
    void calcularAportesExigeLosDatos() {
        CalcularAportesRequestDto sinNada = new CalcularAportesRequestDto();

        assertThatThrownBy(() -> expedienteService.calcularAportes(sinNada))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("obligatorios");
    }

    @Test
    @DisplayName("Calcular aportes devuelve las líneas y los totales por grupo")
    void calcularAportesDevuelveTotales() {
        CalcularAportesRequestDto request = new CalcularAportesRequestDto();
        request.setTipoTareaId(10L);
        request.setHonorariosReferenciales(new BigDecimal("1000000"));

        when(aporteCalculator.calcular(any(TipoTarea.class), any(BigDecimal.class)))
                .thenReturn(new AportesCalculadosDto(List.of(
                        new AportesCalculadosDto.LineaAporte("REGISTRO_OBRA", "Registro de Obra",
                                GrupoAporte.CIEC, new BigDecimal("50000.00")),
                        new AportesCalculadosDto.LineaAporte("CAJA_PROFESIONAL", "Caja (profesional)",
                                GrupoAporte.CAJA, new BigDecimal("90000.00")))));

        AportesResponseDto respuesta = expedienteService.calcularAportes(request);

        assertThat(respuesta.lineas()).hasSize(2);
        assertThat(respuesta.totalCiec()).isEqualByComparingTo("50000.00");
        assertThat(respuesta.totalCaja()).isEqualByComparingTo("90000.00");
        assertThat(respuesta.total()).isEqualByComparingTo("140000.00");
    }

    @Test
    @DisplayName("Calcular aportes con un tipo de tarea inexistente da 404")
    void calcularAportesConTareaInexistente() {
        CalcularAportesRequestDto request = new CalcularAportesRequestDto();
        request.setTipoTareaId(99L);
        request.setHonorariosReferenciales(new BigDecimal("1000"));
        when(tipoTareaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> expedienteService.calcularAportes(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // -------------------- Datos del contrato --------------------

    @Test
    @DisplayName("Los textos en blanco del contrato se guardan como null (el PDF pone puntos)")
    void datosContratoGuardaBlancosComoNull() {
        Expediente e = expedientePropio();

        expedienteService.actualizarDatosContrato(500L, new DatosContratoDto(
                new BigDecimal("250000"), "  ", "", null, "  30 días  ", ""));

        assertThat(e.getDatosContrato().getHonorariosPactados()).isEqualByComparingTo("250000");
        assertThat(e.getDatosContrato().getDocumentacionConfeccion()).isNull();
        assertThat(e.getDatosContrato().getTareasEspeciales()).isNull();
        assertThat(e.getDatosContrato().getFormaPago()).isNull();
        assertThat(e.getDatosContrato().getPlazoEntrega()).isEqualTo("30 días");   // trim
        assertThat(e.getDatosContrato().getGastosEspeciales()).isNull();
    }

    // -------------------- PDFs generables --------------------

    @Test
    @DisplayName("generarDocumento devuelve null para un código sin generador asociado")
    void generarDocumentoDesconocidoDevuelveNull() {
        assertThat(expedienteService.generarDocumento(500L, "MEMORIA_TECNICA")).isNull();
        verify(pdfGenerationService, never()).generar(any());
    }

    // -------------------- Helpers --------------------

    private Expediente capturarGuardado() {
        ArgumentCaptor<Expediente> captor = ArgumentCaptor.forClass(Expediente.class);
        verify(expedienteRepository).save(captor.capture());
        return captor.getValue();
    }
}
