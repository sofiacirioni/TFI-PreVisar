package ar.edu.utn.frc.previsar.dtos.response;

import lombok.*;

/**
 * Respuesta devuelta tanto por /auth/register como por /auth/login.
 *
 * El token va a ser usado por el frontend en el header Authorization
 * de las siguientes requests.
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthResponseDto {
    private String token;

    private String tipoToken;        // Siempre "Bearer"

    private Long expiraEnMs;         // Cuánto dura el token

    private String email;            // Email del usuario autenticado

    private String rol;              // Rol principal del usuario
}
