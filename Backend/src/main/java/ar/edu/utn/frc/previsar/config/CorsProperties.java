package ar.edu.utn.frc.previsar.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuración de CORS — qué orígenes pueden consumir la API.
 * Cargado desde application.properties / variables de entorno.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "previsar.cors")
public class CorsProperties {
    /** Lista de orígenes permitidos (ej: http://localhost:4200). */
    private List<String> allowedOrigins;
}
