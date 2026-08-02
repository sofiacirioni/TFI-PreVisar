package ar.edu.utn.frc.previsar.security;

import ar.edu.utn.frc.previsar.entities.Usuario;
import ar.edu.utn.frc.previsar.enums.Rol;
import ar.edu.utn.frc.previsar.repositories.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Tests del puente entre Spring Security y la tabla usuario.
 *
 * El caso importante es la cuenta dada de baja: el soft-delete pone activo = false
 * y acá eso tiene que traducirse en un UserDetails deshabilitado, que es lo que
 * bloquea el login.
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UsuarioRepository usuarioRepository;

    @InjectMocks private CustomUserDetailsService userDetailsService;

    private Usuario usuario(boolean activo, Rol rol) {
        return Usuario.builder()
                .id(1L).email("sofia@example.com").passwordHash("$2a$10$hash")
                .rol(rol).activo(activo)
                .build();
    }

    @Test
    @DisplayName("Carga el usuario con su email, hash y rol como authority ROLE_*")
    void cargaElUsuarioConSuRol() {
        when(usuarioRepository.findByEmail("sofia@example.com"))
                .thenReturn(Optional.of(usuario(true, Rol.PROFESIONAL)));

        UserDetails detalles = userDetailsService.loadUserByUsername("sofia@example.com");

        assertThat(detalles.getUsername()).isEqualTo("sofia@example.com");
        assertThat(detalles.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(detalles.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_PROFESIONAL");
        assertThat(detalles.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("Una cuenta dada de baja queda deshabilitada y no puede loguearse")
    void cuentaDadaDeBajaQuedaDeshabilitada() {
        when(usuarioRepository.findByEmail("sofia@example.com"))
                .thenReturn(Optional.of(usuario(false, Rol.PROFESIONAL)));

        assertThat(userDetailsService.loadUserByUsername("sofia@example.com").isEnabled()).isFalse();
    }

    @Test
    @DisplayName("El rol ADMIN también se mapea con el prefijo ROLE_")
    void mapeaElRolAdmin() {
        when(usuarioRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(usuario(true, Rol.ADMIN)));

        assertThat(userDetailsService.loadUserByUsername("admin@example.com").getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("Un email inexistente lanza UsernameNotFoundException")
    void emailInexistenteFalla() {
        when(usuarioRepository.findByEmail("fantasma@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("fantasma@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("fantasma@example.com");
    }
}
