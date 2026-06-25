package ar.edu.utn.frc.previsar.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "previsar.storage")
public record StorageProperties(String basePath) {
}
