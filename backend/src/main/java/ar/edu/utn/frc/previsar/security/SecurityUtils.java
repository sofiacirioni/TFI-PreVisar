package ar.edu.utn.frc.previsar.security;

import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Usuario;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.repositories.ProfesionalRepository;
import ar.edu.utn.frc.previsar.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Helper para obtener el usuario autenticado en la request actual.
 *
 * El JwtAuthenticationFilter carga el usuario en el SecurityContext después
 * de validar el token. Acá lo recupera.
 */

@Component
@RequiredArgsConstructor
public class SecurityUtils {
    private final UsuarioRepository usuarioRepository;
    private final ProfesionalRepository profesionalRepository;

    /**
     * Devuelve el email del usuario autenticado en la request actual.
     * Si no hay usuario autenticado, lanza IllegalStateException.
     */
    public String getEmailUsuarioActual() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new IllegalStateException("No hay usuario autenticado");
        }
        return auth.getName();
    }

    /**
     * Devuelve la entidad Usuario del usuario autenticado.
     */
    public Usuario getUsuarioActual() {
        String email = getEmailUsuarioActual();
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario autenticado no encontrado en la BD: " + email));
    }

    /**
     * Devuelve el Profesional asociado al usuario autenticado.
     * Falla si el usuario no es un profesional (ej: si fuera ADMIN).
     */
    public Profesional getProfesionalActual() {
        Usuario usuario = getUsuarioActual();
        return profesionalRepository.findByUsuarioId(usuario.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "El usuario autenticado no tiene un Profesional asociado: "
                                + usuario.getEmail()));
    }

}
