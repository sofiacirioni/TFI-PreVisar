package ar.edu.utn.frc.previsar.services;

import ar.edu.utn.frc.previsar.dtos.request.LoginRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.RegisterRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.AuthResponseDto;

/**
 * Contrato del servicio de autenticación.
 *
 * Define las operaciones disponibles para registro y login de profesionales.
 * La implementación concreta vive en service.impl.AuthServiceImpl.
 */
public interface AuthService {
    /**
     * Registra un nuevo profesional. Crea simultáneamente Usuario + Profesional
     * en una transacción única.
     *
     * @param request datos del registro
     * @return respuesta con el JWT generado para el usuario recién creado
     * @throws ar.edu.utn.frc.previsar.exception.BusinessException si el email,
     *         CUIT o matrícula ya existen
     * @throws ar.edu.utn.frc.previsar.exception.ResourceNotFoundException si la
     *         regional o condicionIva referenciadas no existen
     */
    AuthResponseDto registrar(RegisterRequestDto request);

    /**
     * Autentica un usuario por email + password y devuelve un JWT.
     *
     * @param request credenciales de login
     * @return respuesta con el JWT
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         si las credenciales son incorrectas
     */
    AuthResponseDto login(LoginRequestDto request);
}
