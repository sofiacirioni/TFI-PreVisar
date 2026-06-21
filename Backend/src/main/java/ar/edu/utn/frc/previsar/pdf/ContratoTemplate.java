package ar.edu.utn.frc.previsar.pdf;

import org.openpdf.text.*;

public class ContratoTemplate implements PdfTemplate {
    private final ContratoData d;
    public ContratoTemplate(ContratoData d) { this.d = d; }

    @Override
    public String nombreArchivo() {
        return "contrato-locacion";
    }

    @Override
    public void escribir(Document doc) throws DocumentException {
        Paragraph titulo = new Paragraph("CONTRATO DE LOCACIÓN DE SERVICIO PROFESIONAL", PdfStyles.tituloSerif());
        titulo.setAlignment(Element.ALIGN_CENTER);
        titulo.setSpacingAfter(16);
        doc.add(titulo);

        doc.add(p("Por el presente el / los Ingenieros (locador/es)"));
        doc.add(campo("Nombre", d.profesionalNombre()));
        doc.add(campo("Mat./Nº de Orden", d.profesionalMatriculaOrden(), "Especialidad", d.profesionalEspecialidad()));
        doc.add(campo("con domicilio/s", d.profesionalDomicilio()));

        doc.add(p("convienen con (el/los) Comitente/s (locatario/s)"));
        doc.add(campo("Nombre", d.comitenteNombre(), "CUIT/CUIL", d.comitenteCuit()));
        doc.add(campo("Domicilio/s", d.comitenteDomicilio()));

        doc.add(rotulo("DEL OBJETO: la ejecución de la/s siguiente/s tarea/s profesional/es:", d.tareaProfesional()));
        doc.add(campoVacio("que corresponde a la confección de la siguiente documentación")); // lo completa el profesional
        doc.add(campoVacio("y ejecución de la/s siguiente/s tarea/s especial/es"));

        doc.add(p("DE LA UBICACIÓN DE LA TAREA: la obra (instalación) a ejecutarse en:", PdfStyles.etiquetaSerif()));
        doc.add(campo("Domicilio - Paraje - Barrio", d.obraDomicilio()));
        doc.add(campo("Localidad (CP)", d.obraLocalidad(), "Provincia", d.obraProvincia()));

        doc.add(monto("DEL MONTO DE HONORARIOS: las partes convienen el siguiente monto (**):", d.honorariosPactados()));
        doc.add(p("(**) Además el Comitente se compromete a abonar el aporte jubilatorio Ley 8470 de la Caja de "
                + "Previsión, calculado según la Resolución Nº 12.14/12.", PdfStyles.cuerpoSerif())); // texto fijo: copiá el exacto del .doc
        doc.add(monto("DEL MONTO DE LOS HONORARIOS REFERENCIALES:", d.honorariosReferenciales()));

        doc.add(campoVacio("FORMA DE PAGO CONVENIDA"));
        doc.add(campoVacio("PLAZO DE ENTREGA CONVENIDO"));
        doc.add(campoVacio("GASTOS ESPECIALES CONVENIDOS"));

        Paragraph fecha = p("En la Ciudad de " + nz(d.ciudad()) + " a los ......... días del mes de ............... del año .........");
        fecha.setSpacingBefore(18);
        doc.add(fecha);

        Paragraph firmas = new Paragraph("\n\n_______________________            _______________________\n"
                + "Firma y Sello del/los Profesional/es        Firma del/los Comitente/s", PdfStyles.cuerpoSerif());
        firmas.setAlignment(Element.ALIGN_CENTER);
        firmas.setSpacingBefore(40);
        doc.add(firmas);
    }

    // helpers de armado (label en negrita + valor o línea de puntos) ...
    private Paragraph monto(String label, java.math.BigDecimal v) {
        return rotulo(label, "$ " + v.toPlainString() + "  (SON PESOS " + NumeroALetras.enLetras(v) + ")");
    }
    // p(), campo(), campoVacio(), rotulo(), nz() — utilitarios cortos de Paragraph/Chunk
    private static final float ESPACIO = 6f;

    private Paragraph p(String text) { return p(text, PdfStyles.cuerpoSerif()); }

    private Paragraph p(String text, Font font) {
        Paragraph par = new Paragraph(text, font);
        par.setSpacingAfter(ESPACIO);
        return par;
    }

    /** "Label: valor"  (si el valor viene vacío, dibuja puntos). */
    private Paragraph campo(String label, String valor) {
        Paragraph par = new Paragraph();
        par.add(new Chunk(label + ": ", PdfStyles.etiquetaSerif()));
        par.add(new Chunk(rellenar(valor), PdfStyles.cuerpoSerif()));
        par.setSpacingAfter(ESPACIO);
        return par;
    }

    /** Dos campos en un mismo renglón (ej. Mat./Nº Orden + Especialidad). */
    private Paragraph campo(String l1, String v1, String l2, String v2) {
        Paragraph par = new Paragraph();
        par.add(new Chunk(l1 + ": ", PdfStyles.etiquetaSerif()));
        par.add(new Chunk(rellenar(v1) + "   ", PdfStyles.cuerpoSerif()));
        par.add(new Chunk(l2 + ": ", PdfStyles.etiquetaSerif()));
        par.add(new Chunk(rellenar(v2), PdfStyles.cuerpoSerif()));
        par.setSpacingAfter(ESPACIO);
        return par;
    }

    /** Campo que siempre queda en blanco (lo completa el profesional a mano). */
    private Paragraph campoVacio(String label) {
        Paragraph par = new Paragraph();
        par.add(new Chunk(label + ": ", PdfStyles.etiquetaSerif()));
        par.add(new Chunk(puntos(60), PdfStyles.cuerpoSerif()));
        par.setSpacingAfter(ESPACIO);
        return par;
    }

    /** Rótulo de sección (frase en negrita) seguido del valor en la misma línea. */
    private Paragraph rotulo(String label, String valor) {
        Paragraph par = new Paragraph();
        par.add(new Chunk(label + " ", PdfStyles.etiquetaSerif()));
        par.add(new Chunk(rellenar(valor), PdfStyles.cuerpoSerif()));
        par.setSpacingBefore(ESPACIO);
        par.setSpacingAfter(ESPACIO);
        return par;
    }

    private static String rellenar(String v) { return (v == null || v.isBlank()) ? puntos(40) : v; }
    private static String nz(String v) { return v == null ? "" : v; }
    private static String puntos(int n) { return ".".repeat(n); }
}
