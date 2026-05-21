package ar.edu.utn.frc.previsar.security;

import ar.edu.utn.frc.previsar.entities.Usuario;
import ar.edu.utn.frc.previsar.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Implementación de UserDetailsService que conecta Spring Security con
 * la tabla usuario de la base de datos.
 *
 * Spring Security llama a loadUserByUsername(email) cuando necesita
 * cargar un usuario para autenticación o autorización.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + email));

        // El rol se convierte a SimpleGrantedAuthority con prefijo "ROLE_"
        // (es la convención de Spring Security para roles).
        var autoridad = new SimpleGrantedAuthority("ROLE_" + usuario.getRol().name());

        return User.builder()
                .username(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .authorities(List.of(autoridad))
                .disabled(!usuario.getActivo())
                .build();
    }
}
