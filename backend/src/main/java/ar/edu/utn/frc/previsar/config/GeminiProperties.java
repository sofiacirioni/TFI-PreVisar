package ar.edu.utn.frc.previsar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "previsar.gemini")
public record GeminiProperties(String model, int timeoutSegundos, int dpi, int reintentos) {
    public GeminiProperties {
        if (model == null || model.isBlank()) model = "gemini-3.5-flash";
        if (timeoutSegundos <= 0) timeoutSegundos = 60;   // imágenes @110 DPI tardan ~15-25s; margen holgado
        if (dpi <= 0) dpi = 110;                          // 150 daba PNG de ~1.7MB → inferencia lenta y timeouts
        if (reintentos <= 0) reintentos = 3;              // Gemini devuelve 503/429 transitorios; hay que reintentar
    }
}
