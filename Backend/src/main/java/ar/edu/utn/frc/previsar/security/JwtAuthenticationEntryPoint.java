package ar.edu.utn.frc.previsar.security;

import ar.edu.utn.frc.previsar.dtos.response.ErrorResponseDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

/**
 * Punto de entrada para requests no autenticadas a endpoints protegidos.
 *
 * Spring Security llama a este componente cuando una request anónima (sin
 * token o con token inválido) intenta acceder a un endpoint protegido.
 *
 * Por defecto, Spring devolvería 403 Forbidden. Acá lo cambiamos a 401
 * Unauthorized, que es semánticamente más correcto: "no estás autenticado"
 * (401) es distinto a "estás autenticado pero no tenés permiso" (403).
 *
 * También devolvemos un JSON con el mismo formato que el resto de los
 * errores de la API (ErrorResponse), para consistencia.
 */

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        ErrorResponseDto body = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.UNAUTHORIZED.value())
                .error(HttpStatus.UNAUTHORIZED.getReasonPhrase())
                .mensaje("Autenticación requerida para acceder a este recurso")
                .path(request.getRequestURI())
                .build();

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        objectMapper.writeValue(response.getOutputStream(), body);
    }
}
