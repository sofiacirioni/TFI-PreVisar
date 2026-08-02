package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.response.RevisionExternaDetalleDto;
import ar.edu.utn.frc.previsar.dtos.response.RevisionMetricasDto;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.RevisionExterna;
import ar.edu.utn.frc.previsar.entities.Usuario;
import ar.edu.utn.frc.previsar.enums.EstadoRevision;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ForbiddenException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.repositories.RevisionExternaRepository;
import ar.edu.utn.frc.previsar.repositories.RolRevisorRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la revisión de expedientes externos (función del rol revisor).
 *
 * Foco:
 *   - Guard de revisor: sin RolRevisor no se puede usar nada.
 *   - Validación del PDF al subir (tipo y tamaño), para avisar antes de llamar a la IA.
 *   - Las métricas del panel, que agregan los problemas guardados como JSON.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RevisionExternaServiceImplTest {

    @Mock private RevisionExternaRepository repo;
    @Mock private RolRevisorRepository rolRevisorRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private FileStorageService storage;
    @Mock private GeminiVisionClient gemini;

    @InjectMocks private RevisionExternaServiceImpl revisionService;

    private Usuario usuarioRevisor;

    @BeforeEach
    void setUp() {
        usuarioRevisor = Usuario.builder().id(7L).email("revisor@example.com").build();
        Profesional revisor = Profesional.builder().id(1L).usuario(usuarioRevisor).build();
        when(securityUtils.getProfesionalActual()).thenReturn(revisor);
        when(rolRevisorRepository.existsByProfesionalId(1L)).thenReturn(true);
        when(storage.guardar(any(MultipartFile.class), anyString())).thenReturn("revisiones/7/uuid.pdf");
        when(repo.save(any(RevisionExterna.class))).thenAnswer(inv -> {
            RevisionExterna r = inv.getArgument(0);
            if (r.getId() == null) r.setId(300L);
            return r;
        });
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("archivo", "expediente.pdf", "application/pdf", "%PDF-1.4".getBytes());
    }

    private RevisionExterna revision(EstadoRevision estado, String resultadoJson) {
        return RevisionExterna.builder()
                .id(300L).usuario(usuarioRevisor)
                .nombreArchivo("expediente.pdf").rutaRelativa("revisiones/7/uuid.pdf")
                .estado(estado).resultado(resultadoJson)
                .build();
    }

    // -------------------- Guard de revisor --------------------

    @Test
    @DisplayName("Sin rol de revisor no se puede subir un expediente")
    void sinRolRevisorNoPuedeSubir() {
        when(rolRevisorRepository.existsByProfesionalId(1L)).thenReturn(false);

        assertThatThrownBy(() -> revisionService.crear(pdf()))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("rol de revisor");

        verify(storage, never()).guardar(any(MultipartFile.class), anyString());
    }

    @Test
    @DisplayName("Sin rol de revisor tampoco se puede listar")
    void sinRolRevisorNoPuedeListar() {
        when(rolRevisorRepository.existsByProfesionalId(1L)).thenReturn(false);

        assertThatThrownBy(() -> revisionService.listar()).isInstanceOf(ForbiddenException.class);
    }

    // -------------------- Alta --------------------

    @Test
    @DisplayName("El PDF se guarda bajo la carpeta del revisor y arranca EN_PROGRESO")
    void creaEnProgreso() {
        Long id = revisionService.crear(pdf());

        assertThat(id).isEqualTo(300L);
        verify(storage).guardar(any(MultipartFile.class), eq("revisiones/7"));

        ArgumentCaptor<RevisionExterna> captor = ArgumentCaptor.forClass(RevisionExterna.class);
        verify(repo).save(captor.capture());
        RevisionExterna guardada = captor.getValue();
        assertThat(guardada.getEstado()).isEqualTo(EstadoRevision.EN_PROGRESO);
        assertThat(guardada.getNombreArchivo()).isEqualTo("expediente.pdf");
        assertThat(guardada.getUsuario()).isEqualTo(usuarioRevisor);
        assertThat(guardada.getResultado()).isNull();   // todavía no hay resumen
    }

    @Test
    @DisplayName("Solo se acepta PDF: un JPG se rechaza antes de llamar a la IA")
    void rechazaLoQueNoEsPdf() {
        MockMultipartFile jpg = new MockMultipartFile(
                "archivo", "escaneo.jpg", "image/jpeg", "x".getBytes());

        assertThatThrownBy(() -> revisionService.crear(jpg))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("único archivo PDF");
    }

    @Test
    @DisplayName("Se rechaza un PDF que supera el límite de 15 MB")
    void rechazaPdfDemasiadoGrande() {
        MockMultipartFile enorme = new MockMultipartFile(
                "archivo", "enorme.pdf", "application/pdf", new byte[16 * 1024 * 1024]);

        assertThatThrownBy(() -> revisionService.crear(enorme))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("15 MB");
    }

    @Test
    @DisplayName("Se rechaza un archivo vacío")
    void rechazaArchivoVacio() {
        MockMultipartFile vacio =
                new MockMultipartFile("archivo", "vacio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> revisionService.crear(vacio))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vacío");
    }

    // -------------------- Aislamiento --------------------

    @Test
    @DisplayName("Una revisión de otro revisor da 404")
    void revisionAjenaDa404() {
        when(repo.findByIdAndUsuarioId(999L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> revisionService.obtener(999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("Eliminar borra el archivo físico y la fila")
    void eliminarBorraArchivoYFila() {
        RevisionExterna r = revision(EstadoRevision.COMPLETADO, null);
        when(repo.findByIdAndUsuarioId(300L, 7L)).thenReturn(Optional.of(r));

        revisionService.eliminar(300L);

        verify(storage).eliminar("revisiones/7/uuid.pdf");
        verify(repo).delete(r);
    }

    // -------------------- Detalle --------------------

    @Test
    @DisplayName("El detalle devuelve el resumen parseado como objeto")
    void detalleParseaElResumen() {
        when(repo.findByIdAndUsuarioId(300L, 7L)).thenReturn(Optional.of(
                revision(EstadoRevision.COMPLETADO, "{\"tipo\":\"PR-DT-RT\",\"problemas\":[]}")));

        RevisionExternaDetalleDto detalle = revisionService.obtener(300L);

        assertThat(detalle.resultado().path("tipo").asText()).isEqualTo("PR-DT-RT");
    }

    @Test
    @DisplayName("Un resultado corrupto no rompe el detalle: viaja como null")
    void resultadoCorruptoNoRompe() {
        when(repo.findByIdAndUsuarioId(300L, 7L)).thenReturn(Optional.of(
                revision(EstadoRevision.COMPLETADO, "{ esto no es json")));

        assertThat(revisionService.obtener(300L).resultado()).isNull();
    }

    // -------------------- Métricas --------------------

    @Test
    @DisplayName("Las métricas cuentan por estado y agregan los problemas por tipo")
    void metricasAgreganLosProblemas() {
        when(repo.findByUsuarioIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(
                revision(EstadoRevision.COMPLETADO,
                        "{\"problemas\":[{\"tipo\":\"FIRMA_FALTANTE\"},{\"tipo\":\"ILEGIBLE\"}]}"),
                revision(EstadoRevision.COMPLETADO,
                        "{\"problemas\":[{\"tipo\":\"FIRMA_FALTANTE\"}]}"),
                revision(EstadoRevision.ERROR, null),
                revision(EstadoRevision.EN_PROGRESO, null)));

        RevisionMetricasDto metricas = revisionService.metricas();

        assertThat(metricas.totalAnalizados()).isEqualTo(4);
        assertThat(metricas.completados()).isEqualTo(2);
        assertThat(metricas.conError()).isEqualTo(1);
        assertThat(metricas.enProgreso()).isEqualTo(1);

        // 3 problemas sobre 2 análisis completados = 1.5
        assertThat(metricas.promedioProblemas()).isEqualTo(1.5);

        // Ordenados de mayor a menor
        assertThat(metricas.problemasPorTipo())
                .extracting(RevisionMetricasDto.ConteoDto::etiqueta)
                .containsExactly("FIRMA_FALTANTE", "ILEGIBLE");
        assertThat(metricas.problemasPorTipo().get(0).cantidad()).isEqualTo(2);
    }

    @Test
    @DisplayName("Sin análisis completados el promedio es null, no cero")
    void promedioEsNullSinCompletados() {
        when(repo.findByUsuarioIdOrderByCreatedAtDesc(7L)).thenReturn(List.of(
                revision(EstadoRevision.ERROR, null),
                revision(EstadoRevision.EN_PROGRESO, null)));

        RevisionMetricasDto metricas = revisionService.metricas();

        assertThat(metricas.completados()).isZero();
        assertThat(metricas.promedioProblemas()).isNull();
        assertThat(metricas.problemasPorTipo()).isEmpty();
    }

    @Test
    @DisplayName("Un historial vacío devuelve métricas en cero sin fallar")
    void historialVacio() {
        when(repo.findByUsuarioIdOrderByCreatedAtDesc(7L)).thenReturn(List.of());

        RevisionMetricasDto metricas = revisionService.metricas();

        assertThat(metricas.totalAnalizados()).isZero();
        assertThat(metricas.promedioProblemas()).isNull();
    }
}
