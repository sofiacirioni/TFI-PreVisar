package ar.edu.utn.frc.previsar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "previsar.mercadopago")
public record MercadoPagoProperties(String accessToken, String backUrlBase, String webhookSecret) {
}
