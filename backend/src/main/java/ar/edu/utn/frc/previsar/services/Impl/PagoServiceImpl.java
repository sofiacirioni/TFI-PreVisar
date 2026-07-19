package ar.edu.utn.frc.previsar.services.Impl;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.resources.payment.Payment;

import ar.edu.utn.frc.previsar.config.MercadoPagoProperties;
import ar.edu.utn.frc.previsar.entities.Pago;
import ar.edu.utn.frc.previsar.enums.EstadoPago;
import ar.edu.utn.frc.previsar.exception.PagoException;
import ar.edu.utn.frc.previsar.repositories.ExpedienteRepository;
import ar.edu.utn.frc.previsar.repositories.PagoRepository;
import ar.edu.utn.frc.previsar.services.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoServiceImpl implements PagoService {
    private final PagoRepository pagoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final MercadoPagoProperties props;

    @Override
    @Transactional
    public ResponseEntity<Void> procesarNotificacion(String type, String dataIdQuery,
            String xSignature, String xRequestId, Map<String, Object> body) {

        String paymentId = dataIdQuery != null ? dataIdQuery : extraerDataId(body);
        if (!"payment".equals(type) || paymentId == null)
            return ResponseEntity.ok().build(); // otro evento: ignorar

        if (!firmaValida(paymentId, xRequestId, xSignature)) {
            log.warn("Webhook con firma inválida para payment {}", paymentId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Payment mpPago;
        try {
            mpPago = new PaymentClient().get(Long.parseLong(paymentId)); // fuente de verdad
        } catch (Exception e) {
            log.error("No se pudo consultar el pago {} en MP", paymentId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build(); // 5xx → MP reintenta
        }

        acreditar(paymentId, mpPago);
        return ResponseEntity.ok().build();
    }

    /** Idempotente: upsert por mp_payment_id. */
    private void acreditar(String paymentId, Payment mpPago) {
        Long expedienteId = Long.valueOf(mpPago.getExternalReference());
        EstadoPago estado = mapearEstado(mpPago.getStatus());

        pagoRepository.findByMpPaymentId(paymentId).ifPresentOrElse(
                p -> {
                    p.setEstado(estado);
                    pagoRepository.save(p);
                }, // ya existía: solo actualiza
                () -> pagoRepository.save(Pago.builder()
                        .expediente(expedienteRepository.getReferenceById(expedienteId))
                        .mpPaymentId(paymentId)
                        .estado(estado)
                        .monto(mpPago.getTransactionAmount())
                        .build()));
        log.info("Pago {} del expediente {} → {}", paymentId, expedienteId, estado);
    }

    private EstadoPago mapearEstado(String status) {
        return switch (status) {
            case "approved" -> EstadoPago.APROBADO;
            case "pending", "in_process", "authorized" -> EstadoPago.PENDIENTE;
            default -> EstadoPago.RECHAZADO; // rejected, cancelled, refunded...
        };
    }

    private boolean firmaValida(String dataId, String xRequestId, String xSignature) {
        if (xSignature == null || props.webhookSecret() == null)
            return false;
        String ts = null, v1 = null;
        for (String parte : xSignature.split(",")) {
            String[] kv = parte.split("=", 2);
            if (kv.length == 2) {
                if (kv[0].trim().equals("ts"))
                    ts = kv[1].trim();
                else if (kv[0].trim().equals("v1"))
                    v1 = kv[1].trim();
            }
        }
        if (ts == null || v1 == null)
            return false;
        // plantilla EXACTA de MP; el id va en minúsculas si es alfanumérico (para
        // payment numérico da igual)
        String manifest = "id:" + dataId.toLowerCase() + ";request-id:" + xRequestId + ";ts:" + ts + ";";
        return hmacSha256(manifest, props.webhookSecret()).equals(v1);
    }

    private static String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] h = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new PagoException("Error calculando la firma del webhook", e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extraerDataId(Map<String, Object> body) {
        if (body == null)
            return null;
        Object data = body.get("data");
        return data instanceof Map<?, ?> m && m.get("id") != null ? m.get("id").toString() : null;
    }
}
