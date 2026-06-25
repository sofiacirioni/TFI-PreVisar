package ar.edu.utn.frc.previsar.pdf;

import org.openpdf.text.Document;
import org.openpdf.text.DocumentException;

public interface PdfTemplate {
    /** Nombre del archivo sin extensión, ej. "contrato-locacion-104146". */
    String nombreArchivo();

    /** Escribe el contenido. El open/close lo maneja el servicio. */
    void escribir(Document document) throws DocumentException;
}
