package ar.edu.utn.frc.previsar.config;

import com.google.genai.Client;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync   // habilita @Async (validación visual nivel 3 corre fuera del hilo del request)
public class GeminiConfig {
    @Bean
    public Client geminiClient() {
        return new Client();   // toma GOOGLE_API_KEY del entorno
    }
}
