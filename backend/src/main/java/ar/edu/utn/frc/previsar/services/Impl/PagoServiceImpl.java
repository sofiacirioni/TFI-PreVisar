package ar.edu.utn.frc.previsar.services.Impl;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mercadopago.client.payment.PaymentClient;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.net.MPSearchRequest;
import com.mercadopago.resources.payment.Payment;

import ar.edu.utn.frc.previsar.config.MercadoPagoProperties;
import ar.edu.utn.frc.previsar.entities.Pago;
import ar.edu.utn.frc.previsar.enums.EstadoArancel;
import ar.edu.utn.frc.previsar.enums.EstadoPago;
import ar.edu.utn.frc.previsar.exception.PagoException;
import ar.edu.utn.frc.previsar.repositories.ExpedienteRepository;
import ar.edu.utn.frc.previsar.repositories.PagoRepository;
import ar.edu.utn.frc.previsar.services.EmailService;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import ar.edu.utn.frc.previsar.services.PagoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoServiceImpl implements PagoService {
    /** Tope de pagos a traer por expediente: el arancel se paga una vez, varios son reintentos. */
    private static final int MAX_PAGOS_SYNC = 50;

    private final PagoRepository pagoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final ExpedienteService expedienteService;
    private final MercadoPagoProperties props;
    private final EmailService emailService;

    @Override
    @Transactional
    public ResponseEntity<Void> procesarNotificacion(String type, String dataIdQuery,
            String xSignature, String xRequestId, Map<String, Object> body) {

        // Trazabilidad: sin esto el webhook es una caja negra cuando MP dice que
        // notificó y en la app "no pasó nada".
        log.info("Webhook MP recibido: type={} dataId={} firmada={}", type, dataIdQuery, xSignature != null);

        String paymentId = dataIdQuery != null ? dataIdQuery : extraerDataId(body);
        if (!"payment".equals(type) || paymentId == null) {
            log.info("Webhook ignorado (no es un evento de payment): type={} id={}", type, paymentId);
            return ResponseEntity.ok().build();
        }

        if (!firmaValida(paymentId, xRequestId, xSignature)) {
            log.warn("Webhook con firma inválida para payment {} — se rechaza", paymentId);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        long mpPaymentId;
        try {
            mpPaymentId = Long.parseLong(paymentId);
        } catch (NumberFormatException e) {
            // Permanente: reintentar no lo arregla. 200 para que MP deje de insistir.
            log.warn("data.id no numérico ({}): se ignora la notificación", paymentId);
            return ResponseEntity.ok().build();
        }

        Payment mpPago;
        try {
            mpPago = new PaymentClient().get(mpPaymentId); // fuente de verdad
        } catch (MPApiException e) {
            if (e.getStatusCode() == 404) {
                // El pago no existe o es de otra cuenta: no va a aparecer por reintentar.
                // Es el caso de "Simular notificación" del panel, que manda un id ficticio.
                // Se ACK-ea con 200; devolver 5xx dejaba a MP reintentando para siempre.
                log.warn("Pago {} inexistente en MP (404): se ignora la notificación", paymentId);
                return ResponseEntity.ok().build();
            }
            log.error("MP respondió {} al consultar el pago {}", e.getStatusCode(), paymentId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build(); // 5xx → MP reintenta
        } catch (Exception e) {
            // Transitorio (red, timeout): que MP reintente.
            log.error("No se pudo consultar el pago {} en MP", paymentId, e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).build();
        }

        acreditar(paymentId, mpPago);
        return ResponseEntity.ok().build();
    }

    @Override
    @Transactional
    public EstadoArancel sincronizarConMp(Long expedienteId) {
        expedienteService.verificarPropio(expedienteId); // 404 ante ajenos

        List<Payment> pagos;
        try {
            // external_reference = expedienteId, el mismo hilo que usa el webhook.
            // limit/offset son obligatorios: si quedan en null el SDK explota con un
            // NPE al armar los query params (Map.Entry con valor nulo).
            MPSearchRequest busqueda = MPSearchRequest.builder()
                    .filters(Map.of("external_reference", String.valueOf(expedienteId)))
                    .limit(MAX_PAGOS_SYNC)
                    .offset(0)
                    .build();
            pagos = new PaymentClient().search(busqueda).getResults();
        } catch (Exception e) {
            throw new PagoException("No se pudieron consultar los pagos en Mercado Pago", e);
        }

        // Mismo upsert que el webhook: llamar esto N veces no duplica filas.
        pagos.forEach(p -> acreditar(String.valueOf(p.getId()), p));

        EstadoArancel estado = EstadoArancel.de(
                pagoRepository.existsByExpedienteIdAndEstado(expedienteId, EstadoPago.APROBADO),
                pagoRepository.existsByExpedienteIdAndEstado(expedienteId, EstadoPago.PENDIENTE));
        log.info("Sync expediente {}: {} pago(s) en MP → {}", expedienteId, pagos.size(), estado);
        return estado;
    }

    /** Idempotente: upsert por mp_payment_id. Notifica al profesional solo en la transición a APROBADO. */
    private void acreditar(String paymentId, Payment mpPago) {
        Long expedienteId = Long.valueOf(mpPago.getExternalReference());
        EstadoPago estado = mapearEstado(mpPago.getStatus());

        pagoRepository.findByMpPaymentId(paymentId).ifPresentOrElse(
                p -> {   // ya existía: solo actualiza
                    boolean eraAprobado = p.getEstado() == EstadoPago.APROBADO;
                    p.setEstado(estado);
                    pagoRepository.save(p);
                    // Solo al PASAR a aprobado: reprocesar el mismo webhook no re-notifica.
                    if (!eraAprobado && estado == EstadoPago.APROBADO) notificarPago(p);
                },
                () -> {
                    Pago nuevo = pagoRepository.save(Pago.builder()
                            .expediente(expedienteRepository.getReferenceById(expedienteId))
                            .mpPaymentId(paymentId)
                            .estado(estado)
                            .monto(mpPago.getTransactionAmount())
                            .build());
                    if (estado == EstadoPago.APROBADO) notificarPago(nuevo);
                });
        log.info("Pago {} del expediente {} → {}", paymentId, expedienteId, estado);
    }

    /**
     * Resuelve el destinatario (dueño del expediente) DENTRO de la transacción y delega
     * el envío al EmailService (asíncrono). Se pasan valores, no la entidad: el correo
     * se manda en otro hilo y ahí las relaciones lazy ya estarían detached.
     */
    private void notificarPago(Pago pago) {
        String email = pago.getExpediente().getProfesional().getUsuario().getEmail();
        emailService.notificarPagoAcreditado(pago.getExpediente().getId(), email, pago.getMonto());
    }

    private EstadoPago mapearEstado(String status) {
        return switch (status) {
            case "approved" -> EstadoPago.APROBADO;
            case "pending", "in_process", "authorized" -> EstadoPago.PENDIENTE;
            default -> EstadoPago.RECHAZADO; // rejected, cancelled, refunded...
        };
    }

    private boolean firmaValida(String dataId, String xRequestId, String xSignature) {
        // Sin secret configurado no se puede validar nada. Se procesa igual (si no,
        // en dev el webhook queda muerto y el 401 es indistinguible de un ataque),
        // pero se avisa fuerte. El riesgo es acotado: el pago SIEMPRE se re-consulta
        // a MP con nuestro token, asi que un webhook forjado no puede inventar una
        // aprobacion, solo forzar una re-sincronizacion de un pago real.
        if (props.webhookSecret() == null || props.webhookSecret().isBlank()) {
            log.warn("MP_WEBHOOK_SECRET sin configurar: se procesa el webhook SIN validar la firma. "
                    + "Configuralo (panel MP -> Webhooks) antes de produccion.");
            return true;
        }
        if (xSignature == null) {
            log.warn("Webhook sin header x-signature pero hay secret configurado — se rechaza");
            return false;
        }
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
