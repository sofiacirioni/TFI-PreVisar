package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.request.ObraRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ObraResponseDto;
import ar.edu.utn.frc.previsar.services.ObraService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints de Obra.
 *
 * Combina dos URLs:
 *   /api/comitentes/{id}/obras  → listado y alta dentro del contexto del comitente
 *   /api/obras/{id}             → operaciones sobre una obra específica
 *   /api/obras                  → listado global de obras del profesional
 */

@RestController
@RequiredArgsConstructor
public class ObraController {
    private final ObraService obraService;

    // ----- Listado y alta dentro del contexto del comitente -----

    @GetMapping("/api/comitentes/{comitenteId}/obras")
    public ResponseEntity<List<ObraResponseDto>> listarPorComitente(
            @PathVariable Long comitenteId) {
        return ResponseEntity.ok(obraService.listarPorComitente(comitenteId));
    }

    @PostMapping("/api/comitentes/{comitenteId}/obras")
    public ResponseEntity<ObraResponseDto> crear(
            @PathVariable Long comitenteId,
            @Valid @RequestBody ObraRequestDto request) {
        ObraResponseDto creada = obraService.crear(comitenteId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(creada);
    }

    // ----- Operaciones por id de obra -----

    @GetMapping("/api/obras")
    public ResponseEntity<List<ObraResponseDto>> listarTodas() {
        return ResponseEntity.ok(obraService.listarTodasMisObras());
    }

    @GetMapping("/api/obras/{id}")
    public ResponseEntity<ObraResponseDto> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(obraService.obtenerPorId(id));
    }

    @PutMapping("/api/obras/{id}")
    public ResponseEntity<ObraResponseDto> actualizar(
            @PathVariable Long id,
            @Valid @RequestBody ObraRequestDto request) {
        return ResponseEntity.ok(obraService.actualizar(id, request));
    }

    @DeleteMapping("/api/obras/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        obraService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
