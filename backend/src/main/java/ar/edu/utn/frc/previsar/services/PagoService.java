package ar.edu.utn.frc.previsar.services;

import java.util.Map;

import org.springframework.http.ResponseEntity;

import ar.edu.utn.frc.previsar.enums.EstadoArancel;

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

    /**
     * Reconcilia contra Mercado Pago los pagos del expediente, buscandolos por
     * {@code external_reference}. Es la red de seguridad del webhook: si la
     * notificacion se perdio (caida, deploy, timeout, o MP que no la manda), esto
     * trae el estado real igual. Idempotente: reusa el mismo upsert por
     * mp_payment_id, asi que llamarlo N veces no duplica nada.
     *
     * @return el estado del arancel resultante para el expediente.
     */
    EstadoArancel sincronizarConMp(Long expedienteId);
}
