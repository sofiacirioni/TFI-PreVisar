package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.request.DocumentoRequeridoRequestDto;
import ar.edu.utn.frc.previsar.entities.DocumentoRequerido;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Provincia;
import ar.edu.utn.frc.previsar.entities.RolRevisor;
import ar.edu.utn.frc.previsar.entities.Seccion;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.mapper.EstructuraMapper;
import ar.edu.utn.frc.previsar.repositories.DocumentoRequeridoRepository;
import ar.edu.utn.frc.previsar.repositories.RolRevisorRepository;
import ar.edu.utn.frc.previsar.repositories.SeccionRepository;
import ar.edu.utn.frc.previsar.repositories.TipoTareaRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
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

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la configuración de estructura documental que hace el revisor.
 *
 * Foco: los flags de comportamiento de una ranura (permiteMultiples, generable,
 * validaA4). El request no los traía y ni el alta ni el clonado los seteaban, así
 * que todo documento creado desde la pantalla del revisor quedaba con un solo
 * archivo, sin generación de PDF y exigiendo A4 (incluso en planos), y clonar una
 * provincia producía una estructura degradada.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EstructuraServiceImplTest {

    @Mock private SeccionRepository seccionRepository;
    @Mock private DocumentoRequeridoRepository documentoRepository;
    @Mock private TipoTareaRepository tipoTareaRepository;
    @Mock private RolRevisorRepository rolRevisorRepository;
    @Mock private EstructuraMapper estructuraMapper;
    @Mock private SecurityUtils securityUtils;
    @Mock private ExpedienteService expedienteService;

    @InjectMocks private EstructuraServiceImpl estructuraService;

    private Provincia cordoba;
    private TipoTarea prDtRt;
    private Seccion seccionTecnica;

    @BeforeEach
    void setUp() {
        cordoba = Provincia.builder().id(1L).nombre("Córdoba").codigo("CBA").build();

        prDtRt = new TipoTarea();
        prDtRt.setId(10L);
        prDtRt.setCodigo("PR-DT-RT");

        Profesional revisor = Profesional.builder().id(100L).build();
        RolRevisor rol = RolRevisor.builder().id(1L).profesional(revisor).provincia(cordoba).build();

        when(securityUtils.getProfesionalActual()).thenReturn(revisor);
        when(rolRevisorRepository.findByProfesionalId(100L)).thenReturn(Optional.of(rol));
        when(tipoTareaRepository.findById(10L)).thenReturn(Optional.of(prDtRt));

        seccionTecnica = Seccion.builder()
                .id(50L).provincia(cordoba).tipoTarea(prDtRt)
                .codigo("TECNICO").nombre("Contenido técnico").orden(2).activo(true)
                .build();
        when(seccionRepository.findById(50L)).thenReturn(Optional.of(seccionTecnica));

        // save() devuelve la misma entidad que recibe: alcanza para capturar lo persistido.
        when(documentoRepository.save(any(DocumentoRequerido.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("Alta: persiste los flags que mandó el revisor (planos: varios archivos, sin A4)")
    void altaPersisteLosFlags() {
        DocumentoRequeridoRequestDto request = DocumentoRequeridoRequestDto.builder()
                .codigo("PLANIMETRIA").nombre("Planimetría / planos")
                .obligatorio(true)
                .permiteMultiples(true)
                .generable(false)
                .validaA4(false)          // gran formato: A1/A3
                .build();

        estructuraService.crearDocumento(50L, request);

        DocumentoRequerido guardado = capturarGuardado();
        assertTrue(guardado.isPermiteMultiples(), "un plano admite varios archivos");
        assertFalse(guardado.isValidaA4(), "un plano NO se valida contra A4");
        assertFalse(guardado.isGenerable());
        assertTrue(guardado.isObligatorio());
    }

    @Test
    @DisplayName("Alta: una ranura generable queda marcada como tal (contrato, carátula)")
    void altaDeRanuraGenerable() {
        DocumentoRequeridoRequestDto request = DocumentoRequeridoRequestDto.builder()
                .codigo("CONTRATO_LOCACION").nombre("Contrato de locación")
                .generable(true)
                .build();

        estructuraService.crearDocumento(50L, request);

        assertTrue(capturarGuardado().isGenerable(), "sin esto el compilado sale sin el contrato");
    }

    @Test
    @DisplayName("Alta: los flags ausentes toman el default del negocio, no false")
    void altaAplicaDefaultsCuandoNoVienen() {
        DocumentoRequeridoRequestDto request = DocumentoRequeridoRequestDto.builder()
                .codigo("MEMORIA_TECNICA").nombre("Memoria técnica")
                .build();   // sin ningún flag

        estructuraService.crearDocumento(50L, request);

        DocumentoRequerido guardado = capturarGuardado();
        assertTrue(guardado.isObligatorio(), "por defecto un documento es obligatorio");
        assertTrue(guardado.isValidaA4(), "por defecto se espera A4");
        assertFalse(guardado.isPermiteMultiples());
        assertFalse(guardado.isGenerable());
    }

    @Test
    @DisplayName("Edición: lo que el cliente no manda se conserva (el reorden no apaga flags)")
    void edicionConservaLosFlagsAusentes() {
        DocumentoRequerido existente = DocumentoRequerido.builder()
                .id(70L).seccion(seccionTecnica)
                .codigo("PLANIMETRIA").nombre("Planimetría / planos")
                .obligatorio(true).orden(2)
                .permiteMultiples(true).generable(false).validaA4(false)
                .activo(true)
                .build();
        when(documentoRepository.findById(70L)).thenReturn(Optional.of(existente));

        // Esto es exactamente lo que manda la pantalla al reordenar con las flechas.
        DocumentoRequeridoRequestDto soloOrden = DocumentoRequeridoRequestDto.builder()
                .codigo("PLANIMETRIA").nombre("Planimetría / planos")
                .orden(1)
                .build();

        estructuraService.actualizarDocumento(70L, soloOrden);

        assertTrue(existente.isPermiteMultiples(), "reordenar no puede apagar permiteMultiples");
        assertFalse(existente.isValidaA4(), "reordenar no puede volver a exigir A4 en un plano");
        assertTrue(existente.isObligatorio());
        assertEquals(1, existente.getOrden());
    }

    @Test
    @DisplayName("Edición: los flags que sí vienen se aplican")
    void edicionAplicaLosFlagsPresentes() {
        DocumentoRequerido existente = DocumentoRequerido.builder()
                .id(71L).seccion(seccionTecnica)
                .codigo("ANEXOS").nombre("Anexos")
                .obligatorio(true).orden(3)
                .permiteMultiples(false).generable(false).validaA4(true)
                .activo(true)
                .build();
        when(documentoRepository.findById(71L)).thenReturn(Optional.of(existente));

        estructuraService.actualizarDocumento(71L, DocumentoRequeridoRequestDto.builder()
                .codigo("ANEXOS").nombre("Anexos")
                .obligatorio(false)
                .permiteMultiples(true)
                .validaA4(false)
                .build());

        assertFalse(existente.isObligatorio());
        assertTrue(existente.isPermiteMultiples());
        assertFalse(existente.isValidaA4());
    }

    @Test
    @DisplayName("Clonado: la copia conserva los flags del original")
    void clonadoCopiaLosFlags() {
        Provincia santaFe = Provincia.builder().id(2L).nombre("Santa Fe").codigo("SFE").build();

        DocumentoRequerido plano = DocumentoRequerido.builder()
                .id(80L).codigo("PLANIMETRIA").nombre("Planimetría / planos")
                .obligatorio(true).orden(2)
                .permiteMultiples(true).generable(false).validaA4(false)
                .activo(true)
                .build();
        DocumentoRequerido caratula = DocumentoRequerido.builder()
                .id(81L).codigo("CARATULA").nombre("Carátula del expediente")
                .obligatorio(true).orden(1)
                .permiteMultiples(false).generable(true).validaA4(true)
                .activo(true)
                .build();

        Seccion origen = Seccion.builder()
                .id(60L).provincia(santaFe).tipoTarea(prDtRt)
                .codigo("TECNICO").nombre("Contenido técnico").orden(1).activo(true)
                .documentos(List.of(caratula, plano))
                .build();

        when(seccionRepository.findEstructura(10L, 2L)).thenReturn(List.of(origen));
        when(seccionRepository.findEstructura(10L, 1L)).thenReturn(List.of());   // destino vacío
        when(seccionRepository.save(any(Seccion.class))).thenAnswer(inv -> inv.getArgument(0));

        int sinConesClonadas = estructuraService.clonarEstructura(10L, 2L);

        assertEquals(1, sinConesClonadas);

        ArgumentCaptor<DocumentoRequerido> captor = ArgumentCaptor.forClass(DocumentoRequerido.class);
        verify(documentoRepository, times(2)).save(captor.capture());

        DocumentoRequerido copiaCaratula = buscarPorCodigo(captor.getAllValues(), "CARATULA");
        assertTrue(copiaCaratula.isGenerable(), "la carátula clonada tiene que seguir siendo generable");
        assertTrue(copiaCaratula.isValidaA4());
        assertFalse(copiaCaratula.isPermiteMultiples());

        DocumentoRequerido copiaPlano = buscarPorCodigo(captor.getAllValues(), "PLANIMETRIA");
        assertTrue(copiaPlano.isPermiteMultiples());
        assertFalse(copiaPlano.isValidaA4(), "el plano clonado no puede pasar a exigir A4");
    }

    // -------------------------- Helpers --------------------------

    private DocumentoRequerido capturarGuardado() {
        ArgumentCaptor<DocumentoRequerido> captor = ArgumentCaptor.forClass(DocumentoRequerido.class);
        verify(documentoRepository).save(captor.capture());
        return captor.getValue();
    }

    private DocumentoRequerido buscarPorCodigo(List<DocumentoRequerido> docs, String codigo) {
        return docs.stream()
                .filter(d -> codigo.equals(d.getCodigo()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No se guardó ningún documento con código " + codigo));
    }
}
