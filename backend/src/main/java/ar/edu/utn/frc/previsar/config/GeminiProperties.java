package ar.edu.utn.frc.previsar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "previsar.gemini")
public record GeminiProperties(String model, int timeoutSegundos, int dpi) {
    public GeminiProperties {
        if (model == null || model.isBlank()) model = "gemini-3.5-flash";
        if (timeoutSegundos <= 0) timeoutSegundos = 30;
        if (dpi <= 0) dpi = 150;
    }
}
