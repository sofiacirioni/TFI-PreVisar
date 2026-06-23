package ar.edu.utn.frc.previsar.pdf;

public record GenerarContratoRequest(
        java.math.BigDecimal honorariosPactados,
        String documentacionConfeccion,
        String tareasEspeciales,
        String formaPago,
        String plazoEntrega,
        String gastosEspeciales
) {
}
