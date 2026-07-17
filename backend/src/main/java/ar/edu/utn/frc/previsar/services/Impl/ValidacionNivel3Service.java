package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.DocumentoValidadoDto;
import ar.edu.utn.frc.previsar.dtos.ObservacionDto;
import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;
import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import ar.edu.utn.frc.previsar.entities.ValidacionVisual;
import ar.edu.utn.frc.previsar.enums.NivelObservacion;
import ar.edu.utn.frc.previsar.enums.OrigenObservacion;
import ar.edu.utn.frc.previsar.repositories.DocumentoCargadoRepository;
import ar.edu.utn.frc.previsar.repositories.ValidacionVisualRepository;
import ar.edu.utn.frc.previsar.services.ValidadorExpediente;
import ar.edu.utn.frc.previsar.utils.HashUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class ValidacionNivel3Service implements ValidadorExpediente {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final DocumentoCargadoRepository documentoCargadoRepository;
    private final ValidacionVisualRepository validacionVisualRepository;
    private final FileStorageService storage;
    private final PdfRasterizerService rasterizer;
    private final GeminiVisionClient gemini;
    private final AnalisisEstadoTracker tracker;

    @Override
    public int nivel() {
        return 3;
    }

    /** BARATO: solo lee lo persistido. Seguro para validarTodo. No llama a Gemini. */
    @Override
    @Transactional(readOnly = true)
    public ValidacionResultadoDto validar(Long expedienteId) {
        List<DocumentoValidadoDto> docs = new ArrayList<>();
        for (DocumentoCargado d : documentoCargadoRepository.findByExpedienteIdAndActivoTrue(expedienteId)) {
            validacionVisualRepository.findFirstByDocumentoCargadoIdOrderByCreatedAtDesc(d.getId())
                    .ifPresent(vv -> {
                        List<ObservacionDto> obs = aObservaciones(vv.getResultado());
                        if (!obs.isEmpty())
                            docs.add(new DocumentoValidadoDto(d.getId(), d.getDocumentoRequerido().getId(),
                                    d.getNombreOriginal(), obs));
                    });
        }
        return new ValidacionResultadoDto(docs, List.of());
    }

    /** CARO: rasteriza + Gemini + persiste. Disparado por el botón, asíncrono. Sin chequeo de
     *  seguridad (el id ya viene validado desde el controller). */
    @Async
    public void analizarAsync(Long expedienteId, Long seccionId) {
        // El estado ya quedó EN_PROGRESO en el gate del controller (iniciarSiLibre).
        List<String> fallidos = new ArrayList<>();
        try {
            for (DocumentoCargado d : documentoCargadoRepository
                    .findByExpedienteIdAndDocumentoRequeridoSeccionIdAndActivoTrue(expedienteId, seccionId)) {
                try {
                    analizarDocumento(d);
                } catch (Exception e) {   // un documento que falla (Gemini, timeout, storage, rasterizado) no aborta el resto
                    // Antes esto se perdía en un warn y el análisis "completaba" vacío sin explicación.
                    log.warn("Nivel 3: falló el análisis del documento {}, se omite", d.getId(), e);
                    fallidos.add(d.getNombreOriginal() + " (" + motivo(e) + ")");
                }
            }
            if (fallidos.isEmpty()) {
                tracker.completar(expedienteId, seccionId);
            } else {
                tracker.completarConErrores(expedienteId, seccionId,
                        "No se pudo analizar " + fallidos.size() + " documento(s): " + String.join(", ", fallidos));
            }
        } catch (Throwable e) {   // incluso Error (ej. OOM al rasterizar): nunca dejar el estado colgado en EN_PROGRESO
            tracker.error(expedienteId, seccionId, "El análisis de IA no se pudo completar");
            log.error("Nivel 3: error general en expediente {} sección {}", expedienteId, seccionId, e);
        }
    }

    private void analizarDocumento(DocumentoCargado d) {
        byte[] bytes = storage.leerBytes(d.getRutaRelativa());
        String hash = HashUtil.sha256(bytes);
        if (validacionVisualRepository.findByDocumentoCargadoIdAndHashDocumento(d.getId(), hash).isPresent())
            return;   // ya analizado con este contenido → no re-llama a Gemini

        List<byte[]> paginas = "application/pdf".equals(d.getTipoMime())
                ? rasterizer.rasterizar(bytes)
                : List.of(bytes);   // imágenes van directo

        ArrayNode problemas = MAPPER.createArrayNode();
        for (byte[] img : paginas) {
            JsonNode res = gemini.analizar(img, PromptVisual.para(d.getNombreOriginal()));
            res.path("problemas").forEach(problemas::add);
        }
        ObjectNode resultado = MAPPER.createObjectNode();
        resultado.set("problemas", problemas);

        validacionVisualRepository.save(ValidacionVisual.builder()
                .documentoCargado(d).hashDocumento(hash).resultado(resultado.toString()).build());
    }

    private List<ObservacionDto> aObservaciones(String json) {
        try {
            List<ObservacionDto> obs = new ArrayList<>();
            for (JsonNode p : MAPPER.readTree(json).path("problemas")) {
                NivelObservacion nivel = "ADVERTENCIA".equals(p.path("severidad").asText())
                        ? NivelObservacion.ADVERTENCIA : NivelObservacion.INFO;
                obs.add(new ObservacionDto("IA_" + p.path("criterio").asText("VISUAL"),
                        nivel, OrigenObservacion.IA_VISUAL, p.path("detalle").asText("")));
            }
            return obs;
        } catch (JsonProcessingException e) {
            return List.of();   // resultado corrupto: no rompe el informe
        }
    }

    /** Motivo corto y legible del fallo, para el detalle que ve el usuario (ej. "Gemini respondió 503 (UNAVAILABLE)"). */
    private String motivo(Throwable e) {
        String m = e.getMessage();
        if (m == null || m.isBlank()) m = e.getClass().getSimpleName();
        return m.length() > 120 ? m.substring(0, 117) + "…" : m;
    }

}
