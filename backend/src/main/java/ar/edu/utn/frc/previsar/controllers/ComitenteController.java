package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.request.ComitenteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ComitenteResponseDto;
import ar.edu.utn.frc.previsar.services.ComitenteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

/**
 * CRUD de comitentes del profesional autenticado.
 *
 * Cada profesional opera solo sobre su propia cartera. La verificación
 * de propiedad se hace en el service.
 */

@RestController
@RequestMapping("/api/comitentes")
@RequiredArgsConstructor
public class ComitenteController {
    private final ComitenteService comitenteService;

    @GetMapping
    public ResponseEntity<List<ComitenteResponseDto>> listar() {
        return ResponseEntity.ok(comitenteService.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ComitenteResponseDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(comitenteService.obtenerPorId(id));
    }

    @GetMapping("/buscar")
    public ResponseEntity<ComitenteResponseDto> buscarPorDniCuit(
            @RequestParam String dniCuit) {
        Optional<ComitenteResponseDto> resultado =
                comitenteService.buscarPorDniCuit(dniCuit);
        return resultado
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ComitenteResponseDto> crear(
            @Valid @RequestBody ComitenteRequestDto request) {
        ComitenteResponseDto creado = comitenteService.crear(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creado);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ComitenteResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ComitenteRequestDto request) {
        return ResponseEntity.ok(comitenteService.actualizar(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        comitenteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
