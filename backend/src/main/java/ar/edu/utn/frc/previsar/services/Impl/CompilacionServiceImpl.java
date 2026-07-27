package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.DocumentoRequeridoDto;
import ar.edu.utn.frc.previsar.dtos.EstructuraExpedienteDto;
import ar.edu.utn.frc.previsar.dtos.SeccionDto;
import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import ar.edu.utn.frc.previsar.exception.PdfGenerationException;
import ar.edu.utn.frc.previsar.repositories.DocumentoCargadoRepository;
import ar.edu.utn.frc.previsar.services.CompilacionService;
import ar.edu.utn.frc.previsar.services.EstructuraService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@RequiredArgsConstructor
public class CompilacionServiceImpl implements CompilacionService {
    private static final float MARGEN = 28f; // ~1 cm

    private final EstructuraService estructuraService;          // estructura ordenada por tarea+provincia
    private final DocumentoCargadoRepository documentoCargadoRepository;
    private final FileStorageService storage;

    @Override
    @Transactional(readOnly = true)
    public byte[] compilar(Long expedienteId) {
        // Estructura scopeada al expediente: incluye las ranuras retiradas que aún tienen
        // archivo cargado, para que esos PDFs también entren en el compilado. Valida además
        // que el expediente sea propio (404 ante ajenos).
        EstructuraExpedienteDto estructura =
                estructuraService.obtenerEstructuraParaExpediente(expedienteId);

        // Archivos activos agrupados por slot (un slot puede tener varios)
        Map<Long, List<DocumentoCargado>> porSlot = documentoCargadoRepository
                .findByExpedienteIdAndActivoTrue(expedienteId).stream()
                .collect(Collectors.groupingBy(d -> d.getDocumentoRequerido().getId()));

        try (PDDocument salida = new PDDocument()) {
            int totalArchivos = 0;
            int fallidos = 0;
            // Recorre en el orden de la estructura: secciones, y dentro, documentos requeridos
            for (SeccionDto seccion : estructura.secciones()) {
                for (DocumentoRequeridoDto docReq : seccion.documentos()) {
                    for (DocumentoCargado archivo : porSlot.getOrDefault(docReq.id(), List.of())) {
                        totalArchivos++;
                        try {
                            anexar(salida, archivo);
                        } catch (Exception e) {
                            // Un archivo ilegible (falta en disco, PDF dañado) no puede tirar abajo
                            // toda la compilación: se saltea y se sigue con el resto.
                            fallidos++;
                            log.warn("Compilación exp {}: se omite el documento {} ({}): {}",
                                    expedienteId, archivo.getId(), archivo.getNombreOriginal(), e.getMessage());
                        }
                    }
                }
            }

            // Sin páginas no se puede guardar (PDFBox falla) y además no tendría sentido
            // devolver un PDF vacío: es señal de que faltan los archivos físicos.
            if (salida.getNumberOfPages() == 0) {
                throw new PdfGenerationException("No se pudo compilar el expediente " + expedienteId
                        + ": no hay documentos legibles (se encontraron " + totalArchivos
                        + " archivo(s), " + fallidos + " ilegible(s)).");
            }
            if (fallidos > 0) {
                log.warn("Compilación exp {}: {} de {} documento(s) quedaron fuera por ser ilegibles",
                        expedienteId, fallidos, totalArchivos);
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
