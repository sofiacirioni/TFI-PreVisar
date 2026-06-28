package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.DocumentoRequeridoDto;
import ar.edu.utn.frc.previsar.dtos.EstructuraExpedienteDto;
import ar.edu.utn.frc.previsar.dtos.SeccionDto;
import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;
import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import ar.edu.utn.frc.previsar.exception.PdfGenerationException;
import ar.edu.utn.frc.previsar.repositories.DocumentoCargadoRepository;
import ar.edu.utn.frc.previsar.services.CompilacionService;
import ar.edu.utn.frc.previsar.services.EstructuraService;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CompilacionServiceImpl implements CompilacionService {
    private static final float MARGEN = 28f; // ~1 cm

    private final ExpedienteService expedienteService;          // obtener: 404 ante ajenos + tipoTarea/provincia derivados
    private final EstructuraService estructuraService;          // estructura ordenada por tarea+provincia
    private final DocumentoCargadoRepository documentoCargadoRepository;
    private final FileStorageService storage;

    @Override
    @Transactional(readOnly = true)
    public byte[] compilar(Long expedienteId) {
        ExpedienteResponseDto exp = expedienteService.obtener(expedienteId);   // 404 ante ajenos

        // Archivos activos agrupados por slot (un slot puede tener varios)
        Map<Long, List<DocumentoCargado>> porSlot = documentoCargadoRepository
                .findByExpedienteIdAndActivoTrue(expedienteId).stream()
                .collect(Collectors.groupingBy(d -> d.getDocumentoRequerido().getId()));

        EstructuraExpedienteDto estructura =
                estructuraService.obtenerEstructura(exp.getTipoTareaId(), exp.getProvinciaId());

        try (PDDocument salida = new PDDocument()) {
            // Recorre en el orden de la estructura: secciones, y dentro, documentos requeridos
            for (SeccionDto seccion : estructura.secciones()) {
                for (DocumentoRequeridoDto docReq : seccion.documentos()) {
                    for (DocumentoCargado archivo : porSlot.getOrDefault(docReq.id(), List.of())) {
                        anexar(salida, archivo);
                    }
                }
            }
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            salida.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new PdfGenerationException("No se pudo compilar el expediente " + expedienteId, e);
        }
    }

    private void anexar(PDDocument salida, DocumentoCargado archivo) throws IOException {
        byte[] bytes = storage.leerBytes(archivo.getRutaRelativa());
        if ("application/pdf".equals(archivo.getTipoMime())) {
            anexarPdf(salida, bytes);
        } else {
            anexarImagen(salida, bytes, archivo.getNombreOriginal());
        }
    }

    /** PDFs: se importan tal cual, sin recomprimir. */
    private void anexarPdf(PDDocument salida, byte[] bytes) throws IOException {
        try (PDDocument origen = Loader.loadPDF(bytes)) {
            PDFMergerUtility merger = new PDFMergerUtility();
            merger.appendDocument(salida, origen);
        }
    }

    /** Imágenes: se ajustan a una página A4, escaladas para entrar en los márgenes
     *  sin deformar (preserva el aspecto) y centradas. */
    private void anexarImagen(PDDocument salida, byte[] bytes, String nombre) throws IOException {
        PDImageXObject img = PDImageXObject.createFromByteArray(salida, bytes, nombre);

        PDPage pagina = new PDPage(PDRectangle.A4);
        salida.addPage(pagina);

        PDRectangle caja = pagina.getMediaBox();
        float maxW = caja.getWidth()  - 2 * MARGEN;
        float maxH = caja.getHeight() - 2 * MARGEN;

        float escala = Math.min(maxW / img.getWidth(), maxH / img.getHeight());
        float w = img.getWidth()  * escala;
        float h = img.getHeight() * escala;
        float x = (caja.getWidth()  - w) / 2;
        float y = (caja.getHeight() - h) / 2;

        try (PDPageContentStream cs = new PDPageContentStream(salida, pagina)) {
            cs.drawImage(img, x, y, w, h);
        }
    }
}
