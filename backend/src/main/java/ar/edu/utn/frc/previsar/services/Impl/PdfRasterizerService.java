package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.config.GeminiProperties;
import ar.edu.utn.frc.previsar.exception.GeminiException;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PdfRasterizerService {
    private final GeminiProperties props;

    /** Una imagen PNG por página del PDF. */
    public List<byte[]> rasterizar(byte[] pdf) {
        List<byte[]> paginas = new ArrayList<>();
        try (PDDocument doc = Loader.loadPDF(pdf)) {
            PDFRenderer renderer = new PDFRenderer(doc);
            for (int i = 0; i < doc.getNumberOfPages(); i++) {
                BufferedImage img = renderer.renderImageWithDPI(i, props.dpi(), ImageType.RGB);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(img, "png", baos);
                paginas.add(baos.toByteArray());
            }
        } catch (IOException e) {
            throw new GeminiException("No se pudo rasterizar el PDF", e);
        }
        return paginas;
    }
}
