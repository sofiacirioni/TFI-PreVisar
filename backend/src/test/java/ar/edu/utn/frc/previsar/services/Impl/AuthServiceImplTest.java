package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.config.JwtProperties;
import ar.edu.utn.frc.previsar.dtos.request.LoginRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.RegisterRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.AuthResponseDto;
import ar.edu.utn.frc.previsar.entities.*;
import ar.edu.utn.frc.previsar.enums.Rol;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.repositories.*;
import ar.edu.utn.frc.previsar.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de AuthServiceImpl.
 *
 * Mockea todas las dependencias (repositories, encoder, jwtService,
 * authenticationManager) y valida la lógica del service en aislamiento.
 */

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {
    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private RegionalRepository regionalRepository;

    @Mock
    private CondicionIvaRepository condicionIvaRepository;

    @Mock
    private TituloRepository tituloRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private JwtProperties jwtProperties;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequestDto registerRequest;
    private Regional regional;
    private CondicionIva condicionIva;
    private Titulo titulo;

    @BeforeEach
    void setUp() {
        // Datos de prueba reusables en varios tests
        Provincia cordoba = Provincia.builder()
                .id(1L).nombre("Córdoba").codigo("CBA").build();

        regional = Regional.builder()
                .id(3L).nombre("San Francisco").provincia(cordoba).build();

        condicionIva = CondicionIva.builder()
                .id(1L).codigo("RESPONSABLE_INSCRIPTO")
                .descripcion("Responsable Inscripto").activo(true).build();

        titulo = Titulo.builder()
                .id(2L).nombre("Tec. en Programacion")
                .permiteTextoLibre(false).activo(true).build();

        registerRequest = RegisterRequestDto.builder()
                .email("sofia@example.com")
                .password("miPassword123")
                .nombre("Sofía")
                .apellido("Cirioni")
                .dni("40123456")
                .cuit("27-40123456-5")
                .matricula("99999")
                .tituloId(2L)
                .domicilio("Calle Falsa 123")
                .telefono("3511234567")
                .regionalId(3L)
                .condicionIvaId(1L)
                .afiliadoCaja8470(false)
                .terminosVersion("1.0")
                .build();
    }

    // Tests de registro
    @Test
    @DisplayName("registrar: camino feliz crea Usuario y Profesional y devuelve token")
    void registrar_caminoFeliz_creaEntidadesYDevuelveToken() {
        // Arrange: configurar mocks para el escenario exitoso
        when(usuarioRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(profesionalRepository.existsByCuit(registerRequest.getCuit())).thenReturn(false);
        when(profesionalRepository.existsByMatricula(registerRequest.getMatricula())).thenReturn(false);
        when(regionalRepository.findById(3L)).thenReturn(Optional.of(regional));
        when(condicionIvaRepository.findById(1L)).thenReturn(Optional.of(condicionIva));
        when(tituloRepository.findById(2L)).thenReturn(Optional.of(titulo));
        when(passwordEncoder.encode("miPassword123")).thenReturn("$2a$10$hashFake");

        Usuario usuarioGuardado = Usuario.builder()
                .id(1L)
                .email("sofia@example.com")
                .passwordHash("$2a$10$hashFake")
                .rol(Rol.PROFESIONAL)
                .activo(true)
                .build();
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioGuardado);
        when(profesionalRepository.save(any(Profesional.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        when(jwtService.generarToken(any(Usuario.class))).thenReturn("token-fake-jwt");
        when(jwtProperties.getExpirationMs()).thenReturn(3_600_000L);

        // Act
        AuthResponseDto response = authService.registrar(registerRequest);

        // Assert: la respuesta
        assertNotNull(response);
        assertEquals("token-fake-jwt", response.getToken());
        assertEquals("Bearer", response.getTipoToken());
        assertEquals("sofia@example.com", response.getEmail());
        assertEquals("PROFESIONAL", response.getRol());

        // Assert: se guardó el Usuario con el password hasheado
        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        Usuario guardado = usuarioCaptor.getValue();
        assertEquals("sofia@example.com", guardado.getEmail());
        assertEquals("$2a$10$hashFake", guardado.getPasswordHash());
        assertEquals(Rol.PROFESIONAL, guardado.getRol());

        // Assert: se guardó el Profesional con los datos correctos
        ArgumentCaptor<Profesional> profCaptor = ArgumentCaptor.forClass(Profesional.class);
        verify(profesionalRepository).save(profCaptor.capture());
        Profesional profGuardado = profCaptor.getValue();
        assertEquals("Sofía", profGuardado.getNombre());
        assertEquals("27-40123456-5", profGuardado.getCuit());
        assertEquals(regional, profGuardado.getRegional());
        assertEquals(condicionIva, profGuardado.getCondicionIva());
    }

    @Test
    @DisplayName("registrar: email duplicado lanza BusinessException")
    void registrar_emailDuplicado_lanzaBusinessException() {
        when(usuarioRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.registrar(registerRequest));

        assertTrue(ex.getMessage().toLowerCase().contains("email"));

        // No debió haber intentado guardar nada
        verify(usuarioRepository, never()).save(any());
        verify(profesionalRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar: CUIT duplicado lanza BusinessException")
    void registrar_cuitDuplicado_lanzaBusinessException() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(profesionalRepository.existsByCuit(registerRequest.getCuit())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.registrar(registerRequest));

        assertTrue(ex.getMessage().toLowerCase().contains("cuit"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar: matrícula duplicada lanza BusinessException")
    void registrar_matriculaDuplicada_lanzaBusinessException() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(profesionalRepository.existsByCuit(any())).thenReturn(false);
        when(profesionalRepository.existsByMatricula(registerRequest.getMatricula())).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> authService.registrar(registerRequest));

        assertTrue(ex.getMessage().toLowerCase().contains("matr"));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar: regional inexistente lanza ResourceNotFoundException")
    void registrar_regionalInexistente_lanzaResourceNotFoundException() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(profesionalRepository.existsByCuit(any())).thenReturn(false);
        when(profesionalRepository.existsByMatricula(any())).thenReturn(false);
        when(regionalRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.registrar(registerRequest));

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar: condicionIva inexistente lanza ResourceNotFoundException")
    void registrar_condicionIvaInexistente_lanzaResourceNotFoundException() {
        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(profesionalRepository.existsByCuit(any())).thenReturn(false);
        when(profesionalRepository.existsByMatricula(any())).thenReturn(false);
        when(regionalRepository.findById(3L)).thenReturn(Optional.of(regional));
        when(condicionIvaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> authService.registrar(registerRequest));

        verify(usuarioRepository, never()).save(any());
    }

    //Test de login
    @Test
    @DisplayName("login: credenciales válidas devuelve token")
    void login_credencialesValidas_devuelveToken() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("sofia@example.com")
                .password("miPassword123")
                .build();

        Usuario usuario = Usuario.builder()
                .id(1L)
                .email("sofia@example.com")
                .passwordHash("$2a$10$hashFake")
                .rol(Rol.PROFESIONAL)
                .activo(true)
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "sofia@example.com", null);

        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(usuarioRepository.findByEmail("sofia@example.com"))
                .thenReturn(Optional.of(usuario));
        when(jwtService.generarToken(usuario)).thenReturn("token-fake-jwt");
        when(jwtProperties.getExpirationMs()).thenReturn(3_600_000L);

        AuthResponseDto response = authService.login(request);

        assertNotNull(response);
        assertEquals("token-fake-jwt", response.getToken());
        assertEquals("sofia@example.com", response.getEmail());
        assertEquals("PROFESIONAL", response.getRol());
    }

    @Test
    @DisplayName("login: credenciales inválidas propaga BadCredentialsException")
    void login_credencialesInvalidas_propagaExcepcion() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("sofia@example.com")
                .password("passwordIncorrecto")
                .build();

        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));

        // No debería haberse intentado generar un token
        verify(jwtService, never()).generarToken(any());
    }

    @Test
    @DisplayName("login: usuario no encontrado tras autenticación lanza ResourceNotFoundException")
    void login_usuarioNoEncontradoPostAuth_lanzaResourceNotFoundException() {
        LoginRequestDto request = LoginRequestDto.builder()
                .email("ghost@example.com")
                .password("cualquiera")
                .build();

        Authentication auth = new UsernamePasswordAuthenticationToken(
                "ghost@example.com", null);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(usuarioRepository.findByEmail("ghost@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.login(request));

        verify(jwtService, never()).generarToken(any());
    }
}