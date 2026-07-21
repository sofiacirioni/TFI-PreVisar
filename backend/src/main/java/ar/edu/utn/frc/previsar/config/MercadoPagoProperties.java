package ar.edu.utn.frc.previsar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config de Mercado Pago. Ojo con las DOS URLs, que son cosas distintas:
 * <ul>
 * <li>{@code backUrlBase}: a donde MP redirige el NAVEGADOR tras pagar. Apunta al
 * frontend (ej. http://localhost:4200).</li>
 * <li>{@code webhookUrlBase}: a donde MP hace la llamada SERVER-TO-SERVER del
 * webhook. Apunta al BACKEND y tiene que ser publica (tunel ngrok en local).
 * Compartir una sola base para ambas era el bug: la notificacion terminaba
 * yendo al frontend y a un host inalcanzable.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "previsar.mercadopago")
public record MercadoPagoProperties(String accessToken, String backUrlBase, String webhookUrlBase,
        String webhookSecret) {
}
