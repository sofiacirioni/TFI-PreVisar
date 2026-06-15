package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.request.ActualizarArancelRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ArancelResponseDto;
import ar.edu.utn.frc.previsar.services.ParametroAporteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de parámetros de aporte.
 *
 * Por ahora expone la actualización del arancel administrativo, que solo
 * puede realizar un revisor. Internamente no es un UPDATE: cierra el valor
 * vigente (vigencia_hasta = ayer) e inserta una nueva fila vigente, para
 * conservar el histórico de valores.
 */

@RestController
@RequestMapping("/api/aportes")
@RequiredArgsConstructor
@Tag(name = "Aportes", description = "Parámetros de aporte: arancel administrativo")
public class ParametroAporteController {
    private final ParametroAporteService parametroAporteService;

    @GetMapping("/arancel")
    @Operation(summary = "Devuelve el valor vigente del arancel administrativo")
    public ResponseEntity<ArancelResponseDto> obtenerArancel() {
        return ResponseEntity.ok(parametroAporteService.obtenerArancelVigente());
    }

    @PutMapping("/arancel")
    @Operation(summary = "Actualiza el arancel administrativo (solo revisor)")
    public ResponseEntity<Void> actualizarArancel(
            @Valid @RequestBody ActualizarArancelRequestDto request) {
        parametroAporteService.actualizarArancel(request);
        return ResponseEntity.noContent().build();
    }
}
