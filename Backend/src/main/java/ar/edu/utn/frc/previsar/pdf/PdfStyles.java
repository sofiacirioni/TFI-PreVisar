package ar.edu.utn.frc.previsar.pdf;

import org.openpdf.text.Font;
import org.openpdf.text.FontFactory;

import java.awt.Color;

public final class PdfStyles {
    private PdfStyles() {}
    public static final float MARGEN = 50f; // pt (~1,76 cm)

    // Serif (Times) — documentos formales en prosa: contrato.
    public static Font tituloSerif()   { return FontFactory.getFont(FontFactory.TIMES_ROMAN, 16, Font.BOLD); }
    public static Font etiquetaSerif() { return FontFactory.getFont(FontFactory.TIMES_ROMAN, 11, Font.BOLD); }
    public static Font cuerpoSerif()   { return FontFactory.getFont(FontFactory.TIMES_ROMAN, 11); }

    // Sans (Helvetica) — formularios tabulares: carátula.
    public static Font cuerpoSans()      { return FontFactory.getFont(FontFactory.HELVETICA, 11); }
    public static Font etiquetaSans()    { return FontFactory.getFont(FontFactory.HELVETICA, 9, Font.BOLD); }
    public static Font valorSans()       { return FontFactory.getFont(FontFactory.HELVETICA, 10); }
    public static Font seccionSans()     { return FontFactory.getFont(FontFactory.HELVETICA, 10, Font.BOLD); }

    // Fondo gris de los encabezados de sección (carátula).
    public static final Color GRIS_SECCION = new Color(230, 230, 230);
}
