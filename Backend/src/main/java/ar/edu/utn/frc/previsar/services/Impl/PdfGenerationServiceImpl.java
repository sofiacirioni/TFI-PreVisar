package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.exception.PdfGenerationException;
import ar.edu.utn.frc.previsar.pdf.PdfStyles;
import ar.edu.utn.frc.previsar.pdf.PdfTemplate;
import ar.edu.utn.frc.previsar.services.PdfGenerationService;
import org.openpdf.text.Document;
import org.openpdf.text.PageSize;
import org.openpdf.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfGenerationServiceImpl implements PdfGenerationService {
    @Override
    public byte[] generar(PdfTemplate template) {
        // A4 con márgenes uniformes; se genera en memoria (byte[]), no a disco.
        Document document = new Document(PageSize.A4,
                PdfStyles.MARGEN, PdfStyles.MARGEN, PdfStyles.MARGEN, PdfStyles.MARGEN);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter.getInstance(document, baos);
            document.open();
            template.escribir(document);   // contenido específico de cada documento
            document.close();              // flush al stream
            return baos.toByteArray();
        } catch (Exception e) {
            throw new PdfGenerationException(
                    "No se pudo generar el PDF '" + template.nombreArchivo() + "'", e);
        }
    }
}
