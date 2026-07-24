package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.response.RevisionExternaDetalleDto;
import ar.edu.utn.frc.previsar.dtos.response.RevisionExternaResumenDto;
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
import ar.edu.utn.frc.previsar.services.RevisionExternaService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class RevisionExternaServiceImpl implements RevisionExternaService {

    /** Límite práctico del modo inline de Gemini: mejor avisar al subir que fallar después. */
    private static final long MAX_BYTES = 15L * 1024 * 1024;   // ~15 MB
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RevisionExternaRepository repo;
    private final RolRevisorRepository rolRevisorRepository;
    private final SecurityUtils securityUtils;
    private final FileStorageService storage;
    private final GeminiVisionClient gemini;

    @Override
    @Transactional
    public Long crear(MultipartFile archivo) {
        Usuario usuario = revisorActual();
        validar(archivo);

        String ruta = storage.guardar(archivo, "revisiones/" + usuario.getId());
        RevisionExterna revision = repo.save(RevisionExterna.builder()
                .usuario(usuario)
                .nombreArchivo(archivo.getOriginalFilename())
                .rutaRelativa(ruta)
                .estado(EstadoRevision.EN_PROGRESO)
                .build());
        return revision.getId();   // el controller dispara el async con este id
    }

    @Override
    @Async
    public void analizarAsync(Long revisionId) {
        RevisionExterna revision = repo.findById(revisionId).orElseThrow(
                () -> new ResourceNotFoundException("Revisión no encontrada: " + revisionId));
        try {
            byte[] pdf = storage.leerBytes(revision.getRutaRelativa());
            JsonNode resumen = gemini.analizarPdf(pdf, PromptResumenExterno.texto());
            if (resumen == null || resumen.isEmpty()) {
                // Escaneo puro sin texto legible es el escenario más probable de falla: que
                // el motivo se vea es lo que salva la demo.
                throw new BusinessException(
                        "La IA no pudo generar un resumen. El PDF puede ser un escaneo de baja calidad o sin texto legible.");
            }
            revision.setResultado(resumen.toString());
            revision.setEstado(EstadoRevision.COMPLETADO);
        } catch (Exception e) {                    // fallo visible, nunca mudo
            revision.setEstado(EstadoRevision.ERROR);
            revision.setDetalle(motivo(e));
            log.warn("Revisión externa {} falló", revisionId, e);
        }
        repo.save(revision);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RevisionExternaResumenDto> listar() {
        Usuario usuario = revisorActual();
        return repo.findByUsuarioIdOrderByCreatedAtDesc(usuario.getId()).stream()
                .map(r -> new RevisionExternaResumenDto(
                        r.getId(), r.getNombreArchivo(), r.getEstado().name(), r.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RevisionExternaDetalleDto obtener(Long id) {
        RevisionExterna r = propiaOr404(id);
        return new RevisionExternaDetalleDto(
                r.getId(), r.getNombreArchivo(), r.getEstado().name(),
                r.getDetalle(), parsear(r.getResultado()), r.getCreatedAt());
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        RevisionExterna r = propiaOr404(id);
        storage.eliminar(r.getRutaRelativa());
        repo.delete(r);
    }

    // -------------------------- Helpers --------------------------

    /** Guard de revisor: el usuario debe ser un profesional con RolRevisor. Si no, 403. */
    private Usuario revisorActual() {
        Profesional prof = securityUtils.getProfesionalActual();
        if (!rolRevisorRepository.existsByProfesionalId(prof.getId())) {
            throw new ForbiddenException("No tenés rol de revisor para usar esta función");
        }
        return prof.getUsuario();
    }

    /** Recupera una revisión asegurando que sea del usuario autenticado (revisor). */
    private RevisionExterna propiaOr404(Long id) {
        Usuario usuario = revisorActual();
        return repo.findByIdAndUsuarioId(id, usuario.getId()).orElseThrow(
                () -> new ResourceNotFoundException("Revisión no encontrada: " + id));
    }

    private void validar(MultipartFile archivo) {
        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("El archivo está vacío");
        }
        if (!"application/pdf".equals(archivo.getContentType())) {
            throw new BusinessException("El expediente debe ser un único archivo PDF");
        }
        if (archivo.getSize() > MAX_BYTES) {
            throw new BusinessException("El PDF supera el límite de 15 MB. Reducí su tamaño e intentá de nuevo.");
        }
    }

    /** JSON crudo persistido → JsonNode para que se serialice como objeto. Null si aún no hay resumen. */
    private JsonNode parsear(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            return null;   // resultado corrupto: no rompe el detalle
        }
    }

    /** Motivo corto y legible del fallo, para el detalle que ve el revisor. */
    private String motivo(Throwable e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) m = e.getClass().getSimpleName();
        return m.length() > 250 ? m.substring(0, 247) + "…" : m;
    }
}
