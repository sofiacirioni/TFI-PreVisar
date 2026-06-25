package ar.edu.utn.frc.previsar.pdf;

public record ContratoData(
        String profesionalNombre, String profesionalMatriculaOrden,
        String profesionalEspecialidad, String profesionalDomicilio,
        String comitenteNombre, String comitenteCuit, String comitenteDomicilio,
        String tareaProfesional,
        String obraDomicilio, String obraLocalidad, String obraProvincia,
        java.math.BigDecimal honorariosPactados,
        java.math.BigDecimal honorariosReferenciales,
        String ciudad,
        String documentacionConfeccion, String tareasEspeciales,
        String formaPago, String plazoEntrega, String gastosEspeciales
) {}
