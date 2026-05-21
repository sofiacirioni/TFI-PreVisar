package ar.edu.utn.frc.previsar.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Properties de configuración JWT.
 *
 * Se mapean automáticamente desde las properties con prefijo "previsar.jwt"
 * en application.properties. Spring las carga al arrancar.
 *
 * Ejemplo:
 *   previsar.jwt.secret=xxx       → JwtProperties.secret
 *   previsar.jwt.expiration-ms=...→ JwtProperties.expirationMs
 *   previsar.jwt.issuer=xxx       → JwtProperties.issuer
 */

@Component
@ConfigurationProperties(prefix = "previsar.jwt")
@Getter
@Setter
public class JwtProperties {
    /**
     * Clave secreta para firmar y verificar tokens (al menos 256 bits).
     */
    private String secret;

    /**
     * Duración del token en milisegundos. Por defecto 1 hora = 3600000.
     */
    private long expirationMs;

    /**
     * Identificador del emisor del token (campo "iss" del payload).
     */
    private String issuer;
}
