package ar.edu.utn.frc.previsar.pdf;

import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;

public final class PdfStyles {
    private PdfStyles() {}
    public static final float MARGEN = 50f; // pt (~1,76 cm)

    // Serif (Times) — documentos formales: contrato y carátula.
    public static Font tituloSerif()   { return FontFactory.getFont(FontFactory.TIMES_ROMAN, 16, Font.BOLD); }
    public static Font etiquetaSerif() { return FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD); }
    public static Font cuerpoSerif()   { return FontFactory.getFont(FontFactory.TIMES_ROMAN, 11); }

    // Sans (Helvetica) — por si algún documento la necesita.
    public static Font cuerpoSans()    { return FontFactory.getFont(FontFactory.HELVETICA, 11); }
}
