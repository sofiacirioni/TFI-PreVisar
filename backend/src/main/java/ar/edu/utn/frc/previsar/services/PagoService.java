package ar.edu.utn.frc.previsar.services;

import java.util.Map;

import org.springframework.http.ResponseEntity;

public interface PagoService {
    /**
     * Procesa una notificacion (webhook) de Mercado Pago. Valida la firma,
     * consulta el pago en MP como fuente de verdad y acredita el estado en el
     * expediente de forma idempotente (upsert por mp_payment_id).
     *
     * @return 200 si se proceso o se ignoro; 401 si la firma es invalida;
     *         502 si no se pudo consultar el pago en MP (MP reintentara).
     */
    ResponseEntity<Void> procesarNotificacion(String type, String dataIdQuery,
            String xSignature, String xRequestId, Map<String, Object> body);
}
