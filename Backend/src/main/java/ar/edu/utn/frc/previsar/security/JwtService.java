package ar.edu.utn.frc.previsar.security;

import ar.edu.utn.frc.previsar.config.JwtProperties;
import ar.edu.utn.frc.previsar.entities.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.Optional;

/**
 * Servicio responsable de generar y validar JWTs.
 *
 * Responsabilidades:
 *   - Generar un token firmado a partir de un Usuario.
 *   - Validar un token recibido (firma, expiración, formato).
 *   - Extraer los claims (id, email, rol) de un token válido.
 *
 * NO se encarga de:
 *   - Saber si el usuario existe en la BD (eso lo hace UserDetailsService).
 *   - Aplicar reglas de autorización (eso lo hace SecurityConfig).
 *
 * Es puramente un "motor" criptográfico.
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    private final JwtProperties jwtProperties;

    /**
     * Genera un JWT firmado para el usuario dado.
     *
     * El token incluye:
     *   - subject:  email del usuario (identificador principal)
     *   - userId:   id numérico del usuario
     *   - rol:      rol del usuario
     *   - issuer:   "previsar-api"
     *   - issued:   timestamp de emisión
     *   - expires:  timestamp de expiración (now + expirationMs)
     */
    public String generarToken(Usuario usuario) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + jwtProperties.getExpirationMs());

        return Jwts.builder()
                .subject(usuario.getEmail())
                .claim("userId", usuario.getId())
                .claim("rol", usuario.getRol().name())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(ahora)
                .expiration(expiracion)
                .signWith(obtenerClaveDeFirma())
                .compact();
    }

    /**
     * Valida un token. Devuelve los claims si es válido, Optional.empty() si no.
     *
     * Causas de invalidez:
     *   - Firma inválida (token modificado o firmado con otra clave).
     *   - Token expirado.
     *   - Formato incorrecto.
     *   - Token nulo o vacío.
     */
    public Optional<Claims> validarYExtraerClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(obtenerClaveDeFirma())
                    .requireIssuer(jwtProperties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Optional.of(claims);
        } catch (JwtException e) {
            log.debug("Token JWT inválido: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Extrae el email (subject) de un token válido.
     */
    public Optional<String> extraerEmail(String token) {
        return validarYExtraerClaims(token).map(Claims::getSubject);
    }

    /**
     * Decodifica el secret de Base64 a una SecretKey utilizable por jjwt.
     */
    private SecretKey obtenerClaveDeFirma() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtProperties.getSecret());
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
