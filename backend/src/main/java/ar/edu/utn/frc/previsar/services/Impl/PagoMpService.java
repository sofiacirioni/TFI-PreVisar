package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.config.MercadoPagoProperties;
import ar.edu.utn.frc.previsar.dtos.PreferenciaPagoDto;
import ar.edu.utn.frc.previsar.dtos.request.CalcularAportesRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;
import ar.edu.utn.frc.previsar.exception.PagoException;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import com.mercadopago.client.preference.PreferenceBackUrlsRequest;
import com.mercadopago.client.preference.PreferenceClient;
import com.mercadopago.client.preference.PreferenceItemRequest;
import com.mercadopago.client.preference.PreferenceRequest;
import com.mercadopago.exceptions.MPApiException;
import com.mercadopago.exceptions.MPException;
import com.mercadopago.resources.preference.Preference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagoMpService {
    private final ExpedienteService expedienteService;
    private final MercadoPagoProperties props;

    public PreferenciaPagoDto crearPreferenciaArancel(Long expedienteId) {
        // obtener() ya valida existencia + pertenencia (404 ante ajenos).
        ExpedienteResponseDto exp = expedienteService.obtener(expedienteId);
        BigDecimal monto = calcularArancelCiec(exp);

        PreferenceItemRequest item = PreferenceItemRequest.builder()
                .id("arancel-ciec-" + expedienteId)
                .title("Arancel CIEC - Expediente " + expedienteId)
                .quantity(1)
                .currencyId("ARS")
                .unitPrice(monto)
                .build();

        PreferenceBackUrlsRequest backUrls = PreferenceBackUrlsRequest.builder()
                .success(props.backUrlBase() + "/pago/exito")
                .pending(props.backUrlBase() + "/pago/pendiente")
                .failure(props.backUrlBase() + "/pago/error")
                .build();

        PreferenceRequest.PreferenceRequestBuilder builder = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(backUrls)
                // notificationUrl va en la preferencia, NO en back_urls: es a donde MP
                // manda el webhook (SCRUM-187) que confirma el pago. Usa webhookUrlBase
                // (backend, publico) y NO backUrlBase (frontend): son destinos distintos.
                .notificationUrl(notificationUrl())
                .externalReference(String.valueOf(expedienteId)); // CLAVE: el webhook resuelve el
                                                                  // expediente por aca

        // auto_return exige el back_url success en HTTPS publico (ej. tunel ngrok).
        // En http://localhost MP rechaza la preferencia, asi que solo se activa con
        // HTTPS.
        if (props.backUrlBase().startsWith("https://")) {
            builder.autoReturn("approved");
        }

        PreferenceRequest request = builder.build();

        try {
            Preference pref = new PreferenceClient().create(request);
            // Deja constancia de con que notification_url quedo ATADA la preferencia:
            // el link viejo conserva la URL vieja, y sin este log no hay forma de
            // distinguir "MP no notifico" de "notifico a otra URL".
            log.info("Preferencia MP creada: id={} expediente={} notificationUrl={}",
                    pref.getId(), expedienteId, request.getNotificationUrl());
            return new PreferenciaPagoDto(pref.getId(), pref.getInitPoint());
        } catch (MPApiException e) {
            throw new PagoException("MP rechazo la preferencia: " + e.getApiResponse().getContent(), e);
        } catch (MPException e) {
            throw new PagoException("Error al crear la preferencia de pago", e);
        }
    }

    /**
     * URL publica del webhook (backend). Devuelve null si no esta configurada: es
     * preferible omitir el campo a mandarle a MP un host inalcanzable como
     * localhost, que hace que la notificacion se pierda en silencio.
     */
    private String notificationUrl() {
        String base = props.webhookUrlBase();
        if (base == null || base.isBlank()) {
            log.warn("previsar.mercadopago.webhook-url-base sin configurar: la preferencia se crea "
                    + "SIN notification_url, asi que MP no va a confirmar el pago. "
                    + "Configura MP_WEBHOOK_URL_BASE con la URL publica del backend (tunel ngrok).");
            return null;
        }
        return base.replaceAll("/+$", "") + "/api/pagos/webhook"; // tolera barra final
    }

    /**
     * Monto a cobrar por el arancel CIEC del expediente. Es el total del grupo
     * CIEC,
     * que YA INCLUYE el Registro de Obra (ROD, 5% s/honorarios) + el arancel
     * administrativo (fijo) — ver conceptos grupo CIEC en V014/V017.
     * NO sumar el 5% aparte: se estaria cobrando el ROD dos veces. El grupo CAJA
     * (aportes profesional/comitente) NO va en este cobro.
     */
    private BigDecimal calcularArancelCiec(ExpedienteResponseDto exp) {
        CalcularAportesRequestDto req = new CalcularAportesRequestDto();
        req.setTipoTareaId(exp.getTipoTareaId());
        req.setHonorariosReferenciales(exp.getHonorariosReferenciales());
        return expedienteService.calcularAportes(req).totalCiec();
    }
}
