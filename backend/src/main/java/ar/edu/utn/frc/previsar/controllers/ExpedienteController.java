package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.DatosContratoDto;
import ar.edu.utn.frc.previsar.dtos.PreferenciaPagoDto;
import ar.edu.utn.frc.previsar.dtos.request.CalcularAportesRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.ExpedienteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.AportesResponseDto;
import ar.edu.utn.frc.previsar.dtos.response.EstadoArancelDto;
import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;
import ar.edu.utn.frc.previsar.pdf.PdfResponseFactory;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import ar.edu.utn.frc.previsar.services.Impl.PagoMpService;
import ar.edu.utn.frc.previsar.services.PagoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/expedientes")
@RequiredArgsConstructor
public class ExpedienteController {
    private final ExpedienteService expedienteService;
    private final PagoMpService pagoMpService;
    private final PagoService pagoService;

    @PostMapping
    public ResponseEntity<ExpedienteResponseDto> crear(@Valid @RequestBody ExpedienteRequestDto request,
                                                       UriComponentsBuilder uriBuilder) {
        ExpedienteResponseDto creado = expedienteService.crear(request);
        URI location = uriBuilder.path("/api/expedientes/{id}")
                .buildAndExpand(creado.getId())
                .toUri();
        return ResponseEntity.created(location).body(creado);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ExpedienteResponseDto> actualizarParcial(@PathVariable Long id,
                                                                @Valid @RequestBody ExpedienteRequestDto request) {
        return ResponseEntity.ok(expedienteService.actualizarParcial(id, request));
    }

    @PostMapping("/{id}/completar")
    public ResponseEntity<ExpedienteResponseDto> completar(@PathVariable Long id) {
        return ResponseEntity.ok(expedienteService.completar(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpedienteResponseDto> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(expedienteService.obtener(id));
    }

    @GetMapping
    public ResponseEntity<List<ExpedienteResponseDto>> listarMisExpedientes() {
        return ResponseEntity.ok(expedienteService.listarMisExpedientes());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        expedienteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/calcular-aportes")
    public ResponseEntity<AportesResponseDto> calcularAportes(@RequestBody CalcularAportesRequestDto request) {
        return ResponseEntity.ok(expedienteService.calcularAportes(request));
    }

    /** Campos editables del contrato. Se guardan en el expediente y el PDF los toma de ahí. */
    @PutMapping("/{id}/contrato/datos")
    public ResponseEntity<ExpedienteResponseDto> guardarDatosContrato(
            @PathVariable Long id, @Valid @RequestBody DatosContratoDto datos) {
        return ResponseEntity.ok(expedienteService.actualizarDatosContrato(id, datos));
    }

    // GET y no POST: el PDF es una lectura de datos ya persistidos, sin cuerpo de request.
    @GetMapping("/{id}/contrato")
    public ResponseEntity<ByteArrayResource> generarContrato(@PathVariable Long id) {
        return PdfResponseFactory.attachment(expedienteService.generarContrato(id), "contrato-locacion-" + id);
    }

    @GetMapping("/{id}/caratula")
    public ResponseEntity<ByteArrayResource> generarCaratula(@PathVariable Long id) {
        return PdfResponseFactory.attachment(expedienteService.generarCaratula(id), "caratula-" + id);
    }

    @PostMapping("/{id}/pago")
    public PreferenciaPagoDto iniciarPago(@PathVariable("id") Long expedienteId) {
        // La pertenencia (404 ante ajenos) ya la valida crearPreferenciaArancel vía obtener().
        return pagoMpService.crearPreferenciaArancel(expedienteId);
    }

    /**
     * Reconcilia el estado del arancel contra Mercado Pago. Red de seguridad del
     * webhook: el front lo llama al volver del pago, así el estado se refleja
     * aunque la notificación se haya perdido. Idempotente.
     */
    @PostMapping("/{id}/pago/sync")
    public ResponseEntity<EstadoArancelDto> sincronizarPago(@PathVariable("id") Long expedienteId) {
        // La pertenencia (404 ante ajenos) la valida sincronizarConMp.
        return ResponseEntity.ok(new EstadoArancelDto(pagoService.sincronizarConMp(expedienteId)));
    }
}
