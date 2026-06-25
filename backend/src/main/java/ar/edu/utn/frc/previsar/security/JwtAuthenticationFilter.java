package ar.edu.utn.frc.previsar.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Filtro que se ejecuta UNA VEZ por request, antes de llegar al controller.
 *
 * Lee el header "Authorization: Bearer xxx", valida el JWT y, si es válido,
 * carga el usuario en el SecurityContext de Spring Security.
 *
 * Este filtro NO rechaza requests. Solo decide si la request está autenticada
 * o no. La decisión de "este endpoint requiere autenticación" la toma
 * SecurityConfig más adelante en la cadena.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        Optional<String> tokenOpt = extraerTokenDelHeader(request);

        if (tokenOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = tokenOpt.get();
        Optional<Claims> claimsOpt = jwtService.validarYExtraerClaims(token);

        if (claimsOpt.isEmpty()) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = claimsOpt.get().getSubject();

        // Solo autentica si no hay autenticación previa en este contexto
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                var authentication = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,  // credenciales: no las necesitamos, el JWT ya las validó
                        userDetails.getAuthorities()
                );
                authentication.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception e) {
                log.debug("No se pudo cargar el usuario {}: {}", email, e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extrae el token del header Authorization. Devuelve vacío si:
     *   - el header no está presente,
     *   - el header no empieza con "Bearer ",
     *   - el token después del "Bearer " está vacío.
     */
    private Optional<String> extraerTokenDelHeader(HttpServletRequest request) {
        String header = request.getHeader(AUTH_HEADER);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        String token = header.substring(BEARER_PREFIX.length()).trim();
        return token.isEmpty() ? Optional.empty() : Optional.of(token);
    }
}
