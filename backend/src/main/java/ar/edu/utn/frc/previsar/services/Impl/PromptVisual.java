package ar.edu.utn.frc.previsar.services.Impl;

public final class PromptVisual {
    private PromptVisual() {}
    public static String para(String nombreDocumento) {
        return """
            Sos un revisor de expedientes técnicos del Colegio de Ingenieros. Analizá la imagen de esta \
            página del documento "%s" y detectá problemas de presentación que puedan hacer que el \
            expediente sea rechazado.

            Evaluá: legibilidad y calidad de imagen; orientación de la página; presencia de firmas \
            donde correspondan; sello profesional con número de matrícula legible; áreas cortadas, \
            tapadas o manchadas; y en planos, la legibilidad del rótulo.

            Devolvé SOLO un objeto JSON con esta forma exacta, sin texto adicional:
            {"problemas":[{"criterio":"LEGIBILIDAD|ORIENTACION|FIRMAS|SELLO|AREA_CORTADA|MANCHA|ROTULO",\
            "severidad":"ADVERTENCIA|INFO","detalle":"descripción breve"}]}
            Si la página está bien, devolvé {"problemas":[]}. No inventes problemas.
            """.formatted(nombreDocumento);
    }
}
