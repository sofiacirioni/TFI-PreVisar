package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.DocumentoValidadoDto;
import ar.edu.utn.frc.previsar.dtos.ObservacionDto;
import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;
import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import ar.edu.utn.frc.previsar.enums.NivelObservacion;
import ar.edu.utn.frc.previsar.enums.OrigenObservacion;
import ar.edu.utn.frc.previsar.repositories.DocumentoCargadoRepository;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import ar.edu.utn.frc.previsar.services.ValidadorExpediente;
import ar.edu.utn.frc.previsar.utils.HashUtil;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ValidacionNivel1Service implements ValidadorExpediente {
    private static final long LIMITE_TOTAL = 32L * 1024 * 1024;                 // 32 MB del expediente
    private static final float A4_W = 595.276f, A4_H = 841.890f, TOL = 5f;      // puntos

    private final ExpedienteService expedienteService;   // para buscarPropio (aislamiento 404)
    private final DocumentoCargadoRepository documentoCargadoRepository;
    private final FileStorageService storage;

    @Override
    @Transactional(readOnly = true)
    public ValidacionResultadoDto validar(Long expedienteId) {
        expedienteService.verificarPropio(expedienteId);  // 404 ante ajenos
        List<DocumentoCargado> docs =
                documentoCargadoRepository.findByExpedienteIdAndActivoTrue(expedienteId);

        List<DocumentoValidadoDto> resultados = new ArrayList<>();
        Map<String, List<String>> porHash = new HashMap<>();
        long total = 0;

        for (DocumentoCargado doc : docs) {
            byte[] bytes = storage.leerBytes(doc.getRutaRelativa());
            total += bytes.length;
            List<ObservacionDto> obs = new ArrayList<>();

            if ("application/pdf".equals(doc.getTipoMime())) {
                validarPdf(bytes, doc, obs);
            } else {
                obs.add(new ObservacionDto("ES_IMAGEN", NivelObservacion.INFO,
                        OrigenObservacion.DETERMINISTICO,
                        "Es una imagen; se ajustará a A4 al compilar el expediente."));
            }
            porHash.computeIfAbsent(HashUtil.sha256(bytes), k -> new ArrayList<>()).add(doc.getNombreOriginal());
            resultados.add(new DocumentoValidadoDto(doc.getId(),
                    doc.getDocumentoRequerido().getId(), doc.getNombreOriginal(), obs));
        }

        List<ObservacionDto> generales = new ArrayList<>();
        if (total > LIMITE_TOTAL)
            generales.add(new ObservacionDto("EXCEDE_32MB", NivelObservacion.ADVERTENCIA,
                    OrigenObservacion.DETERMINISTICO,
                    "El expediente pesa " + (total / 1_048_576) + " MB y supera el límite de 32 MB de miCIEC."));
        porHash.values().stream().filter(l -> l.size() > 1).forEach(l ->
                generales.add(new ObservacionDto("DUPLICADO", NivelObservacion.ADVERTENCIA,
                        OrigenObservacion.DETERMINISTICO,
                        "Hay archivos repetidos: " + String.join(", ", l))));

        return new ValidacionResultadoDto(resultados, generales);
    }

    private void validarPdf(byte[] bytes, DocumentoCargado doc, List<ObservacionDto> obs) {
        // Los planos son de gran formato (A1/A3): el slot marca valida_a4 = false y se omite el chequeo.
        if (!validaA4(doc)) return;
        try (PDDocument pdf = Loader.loadPDF(bytes)) {                 // org.apache.pdfbox.Loader
            for (int i = 0; i < pdf.getNumberOfPages(); i++) {
                PDRectangle box = pdf.getPage(i).getMediaBox();
                if (!esA4(box.getWidth(), box.getHeight())) {
                    obs.add(new ObservacionDto("NO_A4", NivelObservacion.ADVERTENCIA,
                            OrigenObservacion.DETERMINISTICO,
                            "La página " + (i + 1) + " no es A4."));
                    break;
                }
            }
        } catch (IOException e) {
            obs.add(new ObservacionDto("NO_ES_PDF", NivelObservacion.ADVERTENCIA,
                    OrigenObservacion.DETERMINISTICO,
                    "El archivo no se pudo leer como un PDF válido."));
        }
    }

    private boolean validaA4(DocumentoCargado doc) {
        return doc.getDocumentoRequerido().isValidaA4();
    }

    // package-private para testear la geometría (tolerancia + orientación) sin cargar PDFs.
    boolean esA4(float w, float h) {
        return (cerca(w, A4_W) && cerca(h, A4_H)) || (cerca(w, A4_H) && cerca(h, A4_W));
    }

    private boolean cerca(float a, float b) { return Math.abs(a - b) <= TOL; }

    @Override
    public int nivel() {
        return 1;
    }
}
