package ar.edu.utn.frc.previsar.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frc.previsar.services.PagoService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pagos")
@RequiredArgsConstructor
public class PagoController {
    private final PagoService pagoService;

    /**
     * Webhook de Mercado Pago (publico, ver SecurityConfig). MP notifica en dos
     * formatos segun como este dada de alta la integracion:
     * <ul>
     * <li>Webhooks (actual): {@code ?type=payment&data.id=123}</li>
     * <li>IPN (legacy): {@code ?topic=payment&id=123}</li>
     * </ul>
     * Se aceptan ambos y se normalizan, porque si solo se contempla el primero el
     * otro se descarta en silencio y el pago nunca se acredita.
     */
    @PostMapping("/webhook")
    public ResponseEntity<Void> webhook(
            @RequestParam(name = "data.id", required = false) String dataIdQuery,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String topic,
            @RequestParam(name = "id", required = false) String idQuery,
            @RequestHeader(value = "x-signature", required = false) String xSignature,
            @RequestHeader(value = "x-request-id", required = false) String xRequestId,
            @RequestBody(required = false) Map<String, Object> body) {
        String tipo = type != null ? type : topic;
        String dataId = dataIdQuery != null ? dataIdQuery : idQuery;
        return pagoService.procesarNotificacion(tipo, dataId, xSignature, xRequestId, body);
    }

}
