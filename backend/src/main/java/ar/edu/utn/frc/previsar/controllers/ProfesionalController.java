package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.request.BajaCuentaRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.CambiarPasswordRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.ProfesionalUpdateRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.SolicitudRolRevisorRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ProfesionalResponseDto;
import ar.edu.utn.frc.previsar.services.ProfesionalService;
import io.swagger.v3.oas.annotations.Operation;
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

    @PostMapping("/me/solicitar-rol-revisor")
    @Operation(summary = "Solicita el rol de revisor: notifica a la institución por correo")
    public ResponseEntity<Void> solicitarRolRevisor(
            @Valid @RequestBody(required = false) SolicitudRolRevisorRequestDto request) {
        profesionalService.solicitarRolRevisor(request != null ? request.getMensaje() : null);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/me/cambiar-password")
    @Operation(summary = "Cambia la contraseña del profesional autenticado")
    public ResponseEntity<Void> cambiarPassword(
            @Valid @RequestBody CambiarPasswordRequestDto request) {
        profesionalService.cambiarPassword(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/baja")
    @Operation(summary = "Da de baja (deshabilita) la cuenta del profesional autenticado")
    public ResponseEntity<Void> darDeBaja(
            @Valid @RequestBody BajaCuentaRequestDto request) {
        profesionalService.darDeBajaCuenta(request);
        return ResponseEntity.noContent().build();
    }
}
