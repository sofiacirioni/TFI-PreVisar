package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.pdf.PdfTemplate;

public interface PdfGenerationService {
    byte[] generar(PdfTemplate template);
}
