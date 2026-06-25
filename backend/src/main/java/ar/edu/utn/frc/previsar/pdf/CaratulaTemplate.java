package ar.edu.utn.frc.previsar.pdf;

import org.openpdf.text.*;
import org.openpdf.text.pdf.PdfPCell;
import org.openpdf.text.pdf.PdfPTable;

public class CaratulaTemplate implements PdfTemplate {
    private final CaratulaData d;
    public CaratulaTemplate(CaratulaData d) { this.d = d; }

    @Override
    public String nombreArchivo() {
        return "caratula";
    }

    @Override
    public void escribir(Document doc) throws DocumentException {
        PdfPTable t = new PdfPTable(1);
        t.setWidthPercentage(100);

        t.addCell(encabezado());
        t.addCell(seccion("DATOS DEL PROFESIONAL"));
        t.addCell(campo("TÍTULO", d.profesionalTitulo()));
        t.addCell(campo("APELLIDO Y NOMBRE", d.profesionalApellidoNombre()));
        t.addCell(campo("MATRÍCULA PROF. / Nº ORDEN", d.profesionalMatriculaOrden()));

        t.addCell(seccion("DATOS DEL COMITENTE"));
        t.addCell(campo("NOMBRE COMPLETO / RAZÓN SOCIAL", d.comitenteNombreRazonSocial()));
        t.addCell(campo("CUIT/CUIL Nº", d.comitenteCuit()));
        t.addCell(campo("DISTRIBUIDORA ELÉCTRICA", d.distribuidoraElectrica()));

        t.addCell(dosColumnas("Nº EXPEDIENTE ERSeP", "Nº EXPEDIENTE EPEC")); // en blanco

        t.addCell(campo("DESCRIPCIÓN DE LA OBRA / ESTUDIO", d.descripcionObra()));
        t.addCell(campo("UBICACIÓN DE LA OBRA", d.ubicacionObra()));
        t.addCell(campo("TAREA PROFESIONAL", d.tareaProfesional()));

        t.addCell(firmas("FIRMA PROFESIONAL", "FIRMA COMITENTE", "FIRMA VISADOR"));
        doc.add(t);
    }

    private PdfPCell encabezado() {
        PdfPTable h = new PdfPTable(new float[]{1, 4});
        h.setWidthPercentage(100);
        // Logo opcional: agregá /static/cec-logo.png en resources; si no está, queda solo el texto.
        PdfPCell logo;
        try {
            Image img = Image.getInstance(getClass().getResource("/static/cec-logo.png"));
            img.scaleToFit(70, 70);
            logo = new PdfPCell(img, false);
        } catch (Exception e) {
            logo = new PdfPCell(new Phrase(""));
        }
        logo.setHorizontalAlignment(Element.ALIGN_CENTER);
        logo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        logo.setBorder(Rectangle.NO_BORDER);
        h.addCell(logo);

        Paragraph tit = new Paragraph();
        tit.add(new Chunk("COLEGIO DE INGENIEROS ESPECIALISTAS DE CÓRDOBA\n",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        tit.add(new Chunk("Ley Nº 7673\nJujuy 441 - (5000) Córdoba - www.ciec.com.ar",
                FontFactory.getFont(FontFactory.HELVETICA, 7)));
        PdfPCell tc = new PdfPCell(tit);
        tc.setBorder(Rectangle.NO_BORDER);
        tc.setHorizontalAlignment(Element.ALIGN_CENTER);
        tc.setVerticalAlignment(Element.ALIGN_MIDDLE);
        h.addCell(tc);

        PdfPCell cell = new PdfPCell(h);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell seccion(String texto) {
        PdfPCell c = new PdfPCell(new Phrase(texto, PdfStyles.seccionSans()));
        c.setBackgroundColor(PdfStyles.GRIS_SECCION);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setPadding(5);
        return c;
    }

    private PdfPCell campo(String label, String valor) {
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + ":  ", PdfStyles.etiquetaSans()));
        p.add(new Chunk(valor == null || valor.isBlank() ? "" : valor, PdfStyles.valorSans()));
        PdfPCell c = new PdfPCell(p);
        c.setPadding(8);
        c.setMinimumHeight(26);
        return c;
    }

    private PdfPCell dosColumnas(String l1, String l2) {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        for (String l : new String[]{l1, l2}) {
            PdfPCell c = new PdfPCell(new Phrase(l, PdfStyles.etiquetaSans()));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(6);
            c.setMinimumHeight(30);
            t.addCell(c);
        }
        PdfPCell wrap = new PdfPCell(t);
        wrap.setPadding(0);
        return wrap;
    }

    private PdfPCell firmas(String... labels) {
        PdfPTable t = new PdfPTable(labels.length);
        t.setWidthPercentage(100);
        for (String l : labels) {
            PdfPCell c = new PdfPCell(new Phrase(l, PdfStyles.etiquetaSans()));
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_TOP);
            c.setPadding(6);
            c.setMinimumHeight(80);   // espacio para firmar a mano
            t.addCell(c);
        }
        PdfPCell wrap = new PdfPCell(t);
        wrap.setPadding(0);
        return wrap;
    }
}
