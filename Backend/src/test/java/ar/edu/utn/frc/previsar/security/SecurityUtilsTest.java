package ar.edu.utn.frc.previsar.security;

import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Usuario;
import ar.edu.utn.frc.previsar.enums.Rol;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.repositories.ProfesionalRepository;
import ar.edu.utn.frc.previsar.repositories.UsuarioRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de SecurityUtils.
 *
 * SecurityUtils depende del SecurityContext de Spring Security para
 * saber qué usuario está autenticado. En los tests lo seteamos manualmente
 * y lo limpiamos al terminar para no contaminar otros tests.
 */

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProfesionalRepository profesionalRepository;

    @InjectMocks
    private SecurityUtils securityUtils;

    private Usuario usuario;
    private Profesional profesional;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .email("sofia@example.com")
                .rol(Rol.PROFESIONAL)
                .activo(true)
                .build();

        profesional = Profesional.builder()
                .id(10L)
                .usuario(usuario)
                .nombre("Sofía")
                .build();
    }

    @AfterEach
    void tearDown() {
        // Limpiar el SecurityContext después de cada test para no
        // contaminar otros tests.
        SecurityContextHolder.clearContext();
    }

    /**
     * Helper para configurar un usuario autenticado en el SecurityContext.
     */
    private void autenticarComo(String email) {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                email,
                null,
                Collections.emptyList()  // ← lista de authorities (vacía está bien)
        );
        SecurityContext context = new SecurityContextImpl(auth);
        SecurityContextHolder.setContext(context);
    }

    //Obtener email usuario actual
    @Test
    @DisplayName("getEmailUsuarioActual: devuelve el email del usuario autenticado")
    void getEmailUsuarioActual_conAuth_devuelveEmail() {
        autenticarComo("sofia@example.com");

        String email = securityUtils.getEmailUsuarioActual();

        assertEquals("sofia@example.com", email);
    }

    @Test
    @DisplayName("getEmailUsuarioActual: lanza IllegalStateException si no hay autenticación")
    void getEmailUsuarioActual_sinAuth_lanzaIllegalStateException() {
        // SecurityContext queda vacío (no llamamos a autenticarComo)
        assertThrows(IllegalStateException.class,
                () -> securityUtils.getEmailUsuarioActual());
    }

    //Obtener usuario actual
    @Test
    @DisplayName("getUsuarioActual: devuelve el Usuario del autenticado")
    void getUsuarioActual_existeEnBd_devuelveUsuario() {
        autenticarComo("sofia@example.com");
        when(usuarioRepository.findByEmail("sofia@example.com"))
                .thenReturn(Optional.of(usuario));

        Usuario resultado = securityUtils.getUsuarioActual();

        assertNotNull(resultado);
        assertEquals(1L, resultado.getId());
        assertEquals("sofia@example.com", resultado.getEmail());
    }

    @Test
    @DisplayName("getUsuarioActual: lanza 404 si el usuario autenticado no existe en BD")
    void getUsuarioActual_noExisteEnBd_lanza404() {
        autenticarComo("fantasma@example.com");
        when(usuarioRepository.findByEmail("fantasma@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> securityUtils.getUsuarioActual());
    }

    //Obtener profesional actual
    @Test
    @DisplayName("getProfesionalActual: devuelve el Profesional asociado al usuario actual")
    void getProfesionalActual_caminoFeliz_devuelveProfesional() {
        autenticarComo("sofia@example.com");
        when(usuarioRepository.findByEmail("sofia@example.com"))
                .thenReturn(Optional.of(usuario));
        when(profesionalRepository.findByUsuarioId(1L))
                .thenReturn(Optional.of(profesional));

        Profesional resultado = securityUtils.getProfesionalActual();

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals("Sofía", resultado.getNombre());
    }

    @Test
    @DisplayName("getProfesionalActual: lanza 404 si el usuario no tiene Profesional asociado")
    void getProfesionalActual_sinProfesional_lanza404() {
        // Usuario existe pero no es profesional (ej: rol ADMIN)
        autenticarComo("admin@example.com");
        Usuario admin = Usuario.builder()
                .id(2L).email("admin@example.com").rol(Rol.ADMIN).activo(true).build();
        when(usuarioRepository.findByEmail("admin@example.com"))
                .thenReturn(Optional.of(admin));
        when(profesionalRepository.findByUsuarioId(2L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> securityUtils.getProfesionalActual());
    }
}