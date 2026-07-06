package ar.edu.utn.frc.previsar.config;

import com.mercadopago.MercadoPagoConfig;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

/**
 * Inicializa el SDK de Mercado Pago. El SDK usa configuracion estatica global
 * ({@link MercadoPagoConfig}), por eso el token se setea una sola vez al arrancar.
 * <p>
 * Esta clase NO se llama {@code MercadoPagoConfig} a proposito: hacerlo tapaba
 * (shadow) la clase del SDK y las llamadas estaticas no compilaban.
 */
@Configuration
@RequiredArgsConstructor
public class MercadoPagoClientConfig {
    private final MercadoPagoProperties props;

    @PostConstruct
    public void init() {
        MercadoPagoConfig.setAccessToken(props.accessToken());
        MercadoPagoConfig.setConnectionRequestTimeout(5000);
        MercadoPagoConfig.setSocketTimeout(5000);
    }
}
