package ar.edu.utn.frc.previsar.controllers;

import ar.edu.utn.frc.previsar.dtos.request.LoginRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.RegisterRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.AuthResponseDto;
import ar.edu.utn.frc.previsar.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints públicos de autenticación.
 * Definidos como /auth/** y declarados públicos en SecurityConfig.
 */

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * Registra un nuevo profesional.
     * Devuelve el token JWT para que pueda autenticarse automáticamente
     * después del registro.
     */
    @PostMapping("/register")
    public ResponseEntity<AuthResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        AuthResponseDto response = authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Login con email + password. Devuelve un JWT.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}
