package ar.edu.utn.frc.previsar.dtos;

public record DocumentoRequeridoDto(
        Long id,
        String codigo,
        String nombre,
        boolean obligatorio,
        int orden,
        boolean permiteMultiples,
        boolean generable,
        /** Se espera A4. En false para los de gran formato (planos): el nivel 1 omite el chequeo. */
        boolean validaA4,
        /**
         * true cuando el documento fue retirado de la estructura vigente (activo=false)
         * pero este expediente ya tiene un archivo cargado en esa ranura. Solo se marca
         * en la estructura scopeada por expediente; en la lectura por provincia es siempre false.
         */
        boolean desactivado
) {}
