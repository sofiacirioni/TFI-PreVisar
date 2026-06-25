package ar.edu.utn.frc.previsar.pdf;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

public class PdfResponseFactory {
    private PdfResponseFactory() {}

    public static ResponseEntity<ByteArrayResource> attachment(byte[] pdf, String nombreArchivo) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdf.length)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nombreArchivo + ".pdf").build().toString())
                .body(new ByteArrayResource(pdf));
    }
}
