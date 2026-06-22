package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.DescargaDocumentoDto;
import ar.edu.utn.frc.previsar.dtos.response.DocumentoCargadoResponseDto;
import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import ar.edu.utn.frc.previsar.entities.DocumentoRequerido;
import ar.edu.utn.frc.previsar.entities.Expediente;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.DocumentoCargadoMapper;
import ar.edu.utn.frc.previsar.repositories.DocumentoCargadoRepository;
import ar.edu.utn.frc.previsar.repositories.DocumentoRequeridoRepository;
import ar.edu.utn.frc.previsar.repositories.ExpedienteRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import ar.edu.utn.frc.previsar.services.DocumentoCargadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DocumentoCargadoServiceImpl implements DocumentoCargadoService {
    private static final Set<String> MIMES_OK = Set.of("application/pdf", "image/jpeg", "image/png");

    private final ExpedienteRepository expedienteRepository;
    private final DocumentoRequeridoRepository documentoRequeridoRepository;
    private final DocumentoCargadoRepository documentoCargadoRepository;
    private final FileStorageService storage;
    private final DocumentoCargadoMapper mapper;
    private final SecurityUtils securityUtils;

    @Override
    @Transactional
    public DocumentoCargadoResponseDto subir(Long expedienteId, Long documentoRequeridoId, MultipartFile archivo) {
        Expediente exp = buscarPropio(expedienteId);                       // aislamiento 404
        DocumentoRequerido slot = documentoRequeridoRepository.findById(documentoRequeridoId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento requerido no encontrado"));

        // El slot tiene que ser de la estructura de este expediente.
        if (!slot.getSeccion().getTipoTarea().getId().equals(exp.getTipoTarea().getId()))
            throw new BusinessException("El documento no corresponde a este expediente");

        validarFormato(archivo);

        // Cardinalidad: si el slot es de un solo archivo, el nuevo reemplaza al activo previo.
        if (!slot.isPermiteMultiples()) {
            documentoCargadoRepository
                    .findByExpedienteIdAndDocumentoRequeridoIdAndActivoTrue(expedienteId, documentoRequeridoId)
                    .forEach(this::darDeBaja);
        }

        String ruta = storage.guardar(archivo, expedienteId);
        DocumentoCargado doc = DocumentoCargado.builder()
                .expediente(exp).documentoRequerido(slot)
                .nombreOriginal(StringUtils.cleanPath(archivo.getOriginalFilename()))
                .rutaRelativa(ruta).tipoMime(archivo.getContentType()).tamanoBytes(archivo.getSize())
                .build();
        return mapper.toResponse(documentoCargadoRepository.save(doc));
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoCargadoResponseDto> listar(Long expedienteId) {
        buscarPropio(expedienteId);
        return mapper.toResponseList(documentoCargadoRepository.findByExpedienteIdAndActivoTrue(expedienteId));
    }

    @Override
    @Transactional(readOnly = true)
    public DescargaDocumentoDto descargar(Long expedienteId, Long documentoId) {
        buscarPropio(expedienteId);
        DocumentoCargado doc = buscarDocumento(expedienteId, documentoId);
        return new DescargaDocumentoDto(storage.cargar(doc.getRutaRelativa()), doc.getNombreOriginal(), doc.getTipoMime());
    }

    @Override
    @Transactional
    public void eliminar(Long expedienteId, Long documentoId) {
        buscarPropio(expedienteId);
        darDeBaja(buscarDocumento(expedienteId, documentoId));
    }

    // --- helpers ---

    private void darDeBaja(DocumentoCargado doc) {
        doc.setActivo(false);
        documentoCargadoRepository.save(doc);
        storage.eliminar(doc.getRutaRelativa());   // best-effort: archivo y fila no son transaccionales juntos
    }

    private void validarFormato(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) throw new BusinessException("El archivo está vacío");
        if (!MIMES_OK.contains(archivo.getContentType()))
            throw new BusinessException("Formato no permitido. Se aceptan PDF, JPG y PNG");
    }

    private DocumentoCargado buscarDocumento(Long expedienteId, Long documentoId) {
        return documentoCargadoRepository.findByIdAndExpedienteIdAndActivoTrue(documentoId, expedienteId)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado"));
    }

    /** Mismo patrón de aislamiento 404 que ExpedienteService: solo expedientes del profesional actual. */
    private Expediente buscarPropio(Long expedienteId) {
        Long profesionalId = securityUtils.getProfesionalActual().getId();
        return expedienteRepository
                .findByIdAndProfesionalIdAndActivoTrue(expedienteId, profesionalId)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado"));
    }
}
