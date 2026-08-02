package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.response.DocumentoCargadoResponseDto;
import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import ar.edu.utn.frc.previsar.entities.DocumentoRequerido;
import ar.edu.utn.frc.previsar.entities.Expediente;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Seccion;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.DocumentoCargadoMapper;
import ar.edu.utn.frc.previsar.repositories.DocumentoCargadoRepository;
import ar.edu.utn.frc.previsar.repositories.DocumentoRequeridoRepository;
import ar.edu.utn.frc.previsar.repositories.ExpedienteRepository;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de la subida de documentos al expediente.
 *
 * Foco:
 *   - Formatos aceptados (solo PDF, JPG y PNG).
 *   - Cardinalidad de la ranura: si es de un solo archivo, el nuevo reemplaza al anterior.
 *   - La ranura tiene que pertenecer a la estructura del expediente.
 *   - Aislamiento por profesional (404 ante expedientes ajenos).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DocumentoCargadoServiceImplTest {

    @Mock private ExpedienteRepository expedienteRepository;
    @Mock private DocumentoRequeridoRepository documentoRequeridoRepository;
    @Mock private DocumentoCargadoRepository documentoCargadoRepository;
    @Mock private FileStorageService storage;
    @Mock private DocumentoCargadoMapper mapper;
    @Mock private SecurityUtils securityUtils;

    @InjectMocks private DocumentoCargadoServiceImpl documentoService;

    private Expediente expediente;
    private DocumentoRequerido ranuraSimple;

    @BeforeEach
    void setUp() {
        Profesional profesional = Profesional.builder().id(1L).build();
        when(securityUtils.getProfesionalActual()).thenReturn(profesional);

        TipoTarea prDtRt = new TipoTarea();
        prDtRt.setId(10L);

        expediente = new Expediente();
        expediente.setId(500L);
        expediente.setProfesional(profesional);
        expediente.setTipoTarea(prDtRt);
        when(expedienteRepository.findByIdAndProfesionalIdAndActivoTrue(500L, 1L))
                .thenReturn(Optional.of(expediente));

        Seccion seccion = Seccion.builder().id(50L).tipoTarea(prDtRt).build();
        ranuraSimple = DocumentoRequerido.builder()
                .id(70L).seccion(seccion).codigo("CONTRATO_LOCACION")
                .permiteMultiples(false).activo(true)
                .build();
        when(documentoRequeridoRepository.findById(70L)).thenReturn(Optional.of(ranuraSimple));

        when(storage.guardar(any(MultipartFile.class), anyLong())).thenReturn("500/uuid.pdf");
        when(documentoCargadoRepository.save(any(DocumentoCargado.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        // El mapeo a DTO no es lo que se prueba acá: alcanza con que devuelva algo.
        when(mapper.toResponse(any(DocumentoCargado.class))).thenReturn(
                new DocumentoCargadoResponseDto(900L, 70L, "contrato.pdf", "application/pdf",
                        8L, LocalDateTime.now()));
    }

    private MockMultipartFile pdf() {
        return new MockMultipartFile("archivo", "contrato.pdf", "application/pdf", "%PDF-1.4".getBytes());
    }

    // -------------------- Formatos --------------------

    @Test
    @DisplayName("Un PDF se guarda con su nombre, tipo y tamaño")
    void subePdf() {
        documentoService.subir(500L, 70L, pdf());

        ArgumentCaptor<DocumentoCargado> captor = ArgumentCaptor.forClass(DocumentoCargado.class);
        verify(documentoCargadoRepository).save(captor.capture());
        DocumentoCargado guardado = captor.getValue();
        assertThat(guardado.getNombreOriginal()).isEqualTo("contrato.pdf");
        assertThat(guardado.getTipoMime()).isEqualTo("application/pdf");
        assertThat(guardado.getRutaRelativa()).isEqualTo("500/uuid.pdf");
        assertThat(guardado.getExpediente()).isEqualTo(expediente);
        assertThat(guardado.getDocumentoRequerido()).isEqualTo(ranuraSimple);
    }

    @Test
    @DisplayName("Se rechaza un formato que no sea PDF, JPG o PNG")
    void rechazaFormatoNoPermitido() {
        MockMultipartFile word = new MockMultipartFile(
                "archivo", "contrato.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "x".getBytes());

        assertThatThrownBy(() -> documentoService.subir(500L, 70L, word))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Formato no permitido");

        verify(storage, never()).guardar(any(MultipartFile.class), anyLong());
    }

    @Test
    @DisplayName("Se rechaza un archivo vacío")
    void rechazaArchivoVacio() {
        MockMultipartFile vacio =
                new MockMultipartFile("archivo", "vacio.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> documentoService.subir(500L, 70L, vacio))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vacío");
    }

    // -------------------- Cardinalidad de la ranura --------------------

    @Test
    @DisplayName("Ranura de un solo archivo: el nuevo da de baja al anterior")
    void ranuraSimpleReemplazaElAnterior() {
        DocumentoCargado previo = DocumentoCargado.builder()
                .id(900L).rutaRelativa("500/viejo.pdf").activo(true).build();
        when(documentoCargadoRepository.findByExpedienteIdAndDocumentoRequeridoIdAndActivoTrue(500L, 70L))
                .thenReturn(List.of(previo));

        documentoService.subir(500L, 70L, pdf());

        assertThat(previo.isActivo()).isFalse();
        verify(storage).eliminar("500/viejo.pdf");
    }

    @Test
    @DisplayName("Ranura que admite varios: los anteriores se conservan")
    void ranuraMultipleConservaLosAnteriores() {
        Seccion seccion = ranuraSimple.getSeccion();
        DocumentoRequerido ranuraMultiple = DocumentoRequerido.builder()
                .id(71L).seccion(seccion).codigo("PLANIMETRIA")
                .permiteMultiples(true).activo(true)
                .build();
        when(documentoRequeridoRepository.findById(71L)).thenReturn(Optional.of(ranuraMultiple));

        documentoService.subir(500L, 71L, pdf());

        verify(documentoCargadoRepository, never())
                .findByExpedienteIdAndDocumentoRequeridoIdAndActivoTrue(500L, 71L);
        verify(storage, never()).eliminar(anyString());
    }

    // -------------------- Coherencia y aislamiento --------------------

    @Test
    @DisplayName("No se puede subir a una ranura de otro tipo de tarea")
    void rechazaRanuraDeOtraEstructura() {
        TipoTarea otraTarea = new TipoTarea();
        otraTarea.setId(99L);
        Seccion seccionAjena = Seccion.builder().id(51L).tipoTarea(otraTarea).build();
        DocumentoRequerido ranuraAjena = DocumentoRequerido.builder()
                .id(72L).seccion(seccionAjena).codigo("OTRO").activo(true).build();
        when(documentoRequeridoRepository.findById(72L)).thenReturn(Optional.of(ranuraAjena));

        assertThatThrownBy(() -> documentoService.subir(500L, 72L, pdf()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no corresponde a este expediente");
    }

    @Test
    @DisplayName("Subir a un expediente ajeno da 404")
    void expedienteAjenoDa404() {
        when(expedienteRepository.findByIdAndProfesionalIdAndActivoTrue(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoService.subir(999L, 70L, pdf()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Expediente no encontrado");
    }

    @Test
    @DisplayName("Una ranura inexistente da 404")
    void ranuraInexistenteDa404() {
        when(documentoRequeridoRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoService.subir(500L, 404L, pdf()))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Documento requerido no encontrado");
    }

    // -------------------- Baja --------------------

    @Test
    @DisplayName("Eliminar es baja lógica y borra el archivo físico")
    void eliminarEsSoftDeleteYBorraElArchivo() {
        DocumentoCargado doc = DocumentoCargado.builder()
                .id(900L).rutaRelativa("500/uuid.pdf").activo(true).build();
        when(documentoCargadoRepository.findByIdAndExpedienteIdAndActivoTrue(900L, 500L))
                .thenReturn(Optional.of(doc));

        documentoService.eliminar(500L, 900L);

        assertThat(doc.isActivo()).isFalse();
        verify(documentoCargadoRepository).save(doc);
        verify(storage).eliminar("500/uuid.pdf");
        verify(documentoCargadoRepository, never()).delete(any(DocumentoCargado.class));
    }

    @Test
    @DisplayName("Eliminar un documento que no es del expediente da 404")
    void eliminarDocumentoAjenoDa404() {
        when(documentoCargadoRepository.findByIdAndExpedienteIdAndActivoTrue(901L, 500L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentoService.eliminar(500L, 901L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Documento no encontrado");
    }
}
