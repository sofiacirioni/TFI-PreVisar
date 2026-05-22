package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.request.ProfesionalUpdateRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ProfesionalResponseDto;
import ar.edu.utn.frc.previsar.services.ProfesionalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Endpoints del profesional autenticado.
 *
 * Usa el patrón "/me" para que el endpoint opere sobre el usuario logueado
 * sin necesidad de pasar id (evita que un usuario pueda espiar perfiles
 * ajenos cambiando el id en la URL).
 */

@RestController
@RequestMapping("/api/profesional")
@RequiredArgsConstructor
public class ProfesionalController {
    private final ProfesionalService profesionalService;

    @GetMapping("/me")
    public ResponseEntity<ProfesionalResponseDto> obtenerPerfilActual() {
        return ResponseEntity.ok(profesionalService.obtenerPerfilActual());
    }

    @PutMapping("/me")
    public ResponseEntity<ProfesionalResponseDto> actualizarPerfilActual(
            @Valid @RequestBody ProfesionalUpdateRequestDto request) {
        return ResponseEntity.ok(profesionalService.actualizarPerfilActual(request));
    }
}
