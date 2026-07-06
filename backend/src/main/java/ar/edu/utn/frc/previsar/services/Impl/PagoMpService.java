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
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
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

        PreferenceRequest request = PreferenceRequest.builder()
                .items(List.of(item))
                .backUrls(backUrls)
                .autoReturn("approved")                                // si MP lo rechaza en localhost, sacar esta linea
                .externalReference(String.valueOf(expedienteId))       // CLAVE: el webhook (SCRUM-187) resuelve el expediente por aca
                .build();

        try {
            Preference pref = new PreferenceClient().create(request);
            return new PreferenciaPagoDto(pref.getId(), pref.getInitPoint());
        } catch (MPApiException e) {
            throw new PagoException("MP rechazo la preferencia: " + e.getApiResponse().getContent(), e);
        } catch (MPException e) {
            throw new PagoException("Error al crear la preferencia de pago", e);
        }
    }

    /**
     * Monto a cobrar por el arancel CIEC del expediente. Reutiliza el calculo de
     * aportes existente (grupo CIEC) en vez de acoplarse al repositorio.
     * TODO(SCRUM): sumar el recargo ROD 5% cuando se defina su base de calculo.
     */
    private BigDecimal calcularArancelCiec(ExpedienteResponseDto exp) {
        CalcularAportesRequestDto req = new CalcularAportesRequestDto();
        req.setTipoTareaId(exp.getTipoTareaId());
        req.setHonorariosReferenciales(exp.getHonorariosReferenciales());
        return expedienteService.calcularAportes(req).totalCiec();
    }
}
