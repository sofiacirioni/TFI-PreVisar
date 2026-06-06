package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.request.ExpedienteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ExpedienteResponseDto;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/expedientes")
public class ExpedienteController {
    private final ExpedienteService expedienteService;

    public ExpedienteController(ExpedienteService expedienteService) {
        this.expedienteService = expedienteService;
    }

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
}
