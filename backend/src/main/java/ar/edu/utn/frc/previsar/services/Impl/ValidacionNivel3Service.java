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
     *  seguridad (el id ya viene validado desde el controller).
     *  <p>
     *  El alcance es UNA ranura (el slot que el profesional está mirando, ej. "Contrato de
     *  locación") y no la sección entera: analizar de a una da respuesta en segundos en vez de
     *  minutos, y se analiza solo aquello sobre lo que hay dudas. Una ranura puede tener varios
     *  archivos cargados, por eso sigue siendo un bucle. */
    @Async
    public void analizarAsync(Long expedienteId, Long documentoRequeridoId) {
        // El estado ya quedó EN_PROGRESO en el gate del controller (iniciarSiLibre).
        List<String> fallidos = new ArrayList<>();
        int analizados = 0;
        int problemas = 0;
        try {
            for (DocumentoCargado d : documentoCargadoRepository
                    .findByExpedienteIdAndDocumentoRequeridoIdAndActivoTrue(expedienteId, documentoRequeridoId)) {
                try {
                    problemas += analizarDocumento(d);
                    analizados++;
                } catch (Exception e) {   // un documento que falla (Gemini, timeout, storage, rasterizado) no aborta el resto
                    // Antes esto se perdía en un warn y el análisis "completaba" vacío sin explicación.
                    log.warn("Nivel 3: falló el análisis del documento {}, se omite", d.getId(), e);
                    fallidos.add(d.getNombreOriginal() + " (" + motivo(e) + ")");
                }
            }
            if (fallidos.isEmpty()) {
                tracker.completar(expedienteId, documentoRequeridoId, resumen(analizados, problemas));
            } else {
                tracker.completarConErrores(expedienteId, documentoRequeridoId,
                        "No se pudo analizar " + fallidos.size() + " documento(s): " + String.join(", ", fallidos));
            }
        } catch (Throwable e) {   // incluso Error (ej. OOM al rasterizar): nunca dejar el estado colgado en EN_PROGRESO
            tracker.error(expedienteId, documentoRequeridoId, "El análisis de IA no se pudo completar");
            log.error("Nivel 3: error general en expediente {} ranura {}", expedienteId, documentoRequeridoId, e);
        }
    }

    /**
     * Analiza SIEMPRE, aunque el contenido no haya cambiado: el análisis lo dispara el
     * profesional a mano, y si aprieta "Analizar" espera un resultado nuevo. Antes se
     * saltaba la llamada cuando el hash coincidía, y el botón parecía no hacer nada.
     * Además el modelo no es determinista: volver a correrlo puede detectar algo que la
     * pasada anterior se salteó, que es justamente por qué alguien re-dispara el análisis.
     * <p>
     * El resultado se guarda pisando el análisis previo de ese mismo contenido (hay un
     * índice único por documento+hash), así que el panel siempre muestra el último.
     */
    /**
     * Mensaje para el profesional. Un análisis exitoso que no encuentra nada tiene que
     * decirlo: si no, es indistinguible de uno que falló en silencio (que es justo lo
     * que pasaba).
     */
    private String resumen(int analizados, int problemas) {
        if (analizados == 0) {
            return "No hay documentos cargados en esta ranura para analizar.";
        }
        if (problemas == 0) {
            return "Parece que está todo en orden: la IA no detectó problemas de presentación.";
        }
        return problemas == 1
                ? "La IA detectó 1 observación."
                : "La IA detectó " + problemas + " observaciones.";
    }

    /** @return cuántos problemas detectó, para poder informarle el resultado al usuario. */
    private int analizarDocumento(DocumentoCargado d) {
        byte[] bytes = storage.leerBytes(d.getRutaRelativa());
        String hash = HashUtil.sha256(bytes);   // deja registro de QUE contenido se analizó

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

        // Hay un índice único (documento_cargado_id, hash_documento): al re-analizar el
        // mismo contenido no se puede insertar otra fila, se pisa el resultado anterior.
        ValidacionVisual vv = validacionVisualRepository
                .findByDocumentoCargadoIdAndHashDocumento(d.getId(), hash)
                .orElseGet(() -> ValidacionVisual.builder()
                        .documentoCargado(d).hashDocumento(hash).build());
        vv.setResultado(resultado.toString());
        validacionVisualRepository.save(vv);
        return problemas.size();
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
