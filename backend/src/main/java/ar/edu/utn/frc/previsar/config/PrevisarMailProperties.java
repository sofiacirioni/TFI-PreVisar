package ar.edu.utn.frc.previsar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración propia del envío de correos (prefijo {@code previsar.mail}).
 *
 * Se llama Previsar... para NO chocar con {@code spring.mail.*} (host, puerto,
 * credenciales SMTP), que Spring Boot autoconfigura en su propio
 * {@code org.springframework.boot.autoconfigure.mail.MailProperties}. Acá viven
 * solo las decisiones de la aplicación:
 *
 * <ul>
 *   <li>{@code from}: remitente que ve el destinatario.</li>
 *   <li>{@code colegio}: casilla de la institución que recibe las solicitudes de
 *       rol revisor. Hoy es una sola (CIEC); a futuro podría resolverse por
 *       institución del profesional.</li>
 *   <li>{@code habilitado}: kill-switch. En false, no se envía nada (útil en dev
 *       o si aún no hay credenciales SMTP).</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "previsar.mail")
public record PrevisarMailProperties(String from, String colegio, boolean habilitado) {
    public PrevisarMailProperties {
        if (from == null || from.isBlank()) from = "no-reply@previsar.local";
    }
}
