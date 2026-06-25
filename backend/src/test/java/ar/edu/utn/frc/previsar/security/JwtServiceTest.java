package ar.edu.utn.frc.previsar.security;

import ar.edu.utn.frc.previsar.config.JwtProperties;
import ar.edu.utn.frc.previsar.entities.Usuario;
import ar.edu.utn.frc.previsar.enums.Rol;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de JwtService.
 *
 * No requiere arrancar Spring Boot completo. Instanciamos JwtService a mano
 * con un JwtProperties controlado por el test.
 */
class JwtServiceTest {
    private JwtService jwtService;
    private JwtProperties jwtProperties;
    private Usuario usuarioDePrueba;

    @BeforeEach
    void setUp() {
        // Clave de prueba: 64 bytes en Base64 (≥256 bits requeridos por HS256)
        jwtProperties = new JwtProperties();
        jwtProperties.setSecret(
                "dGVzdC1zZWNyZXQta2V5LWxhcmdhLW11eS1sYXJnYS1wYXJhLXBhc2Fy" +
                        "LWxhLXZhbGlkYWNpb24tZGUtdGFtYW5vLWRlLWNsYXZlLWVuLWhzMjU2"
        );
        jwtProperties.setExpirationMs(3_600_000L); // 1 hora
        jwtProperties.setIssuer("previsar-api-test");

        jwtService = new JwtService(jwtProperties);

        usuarioDePrueba = Usuario.builder()
                .id(42L)
                .email("sofia@example.com")
                .passwordHash("no-importa-para-este-test")
                .rol(Rol.PROFESIONAL)
                .activo(true)
                .build();
    }

    @Test
    @DisplayName("genera un token no vacío a partir de un usuario")
    void generarToken_devuelveTokenNoVacio() {
        String token = jwtService.generarToken(usuarioDePrueba);

        assertNotNull(token);
        assertFalse(token.isBlank());
        // Un JWT siempre tiene 3 partes separadas por punto
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    @DisplayName("un token recién generado se valida correctamente")
    void validarYExtraerClaims_tokenValido_devuelveClaims() {
        String token = jwtService.generarToken(usuarioDePrueba);

        Optional<Claims> claimsOpt = jwtService.validarYExtraerClaims(token);

        assertTrue(claimsOpt.isPresent());
    }

    @Test
    @DisplayName("los claims del token contienen el email, userId y rol del usuario")
    void validarYExtraerClaims_claimsCoincidenConUsuario() {
        String token = jwtService.generarToken(usuarioDePrueba);

        Claims claims = jwtService.validarYExtraerClaims(token).orElseThrow();

        assertEquals("sofia@example.com", claims.getSubject());
        assertEquals(42, claims.get("userId", Integer.class));
        assertEquals("PROFESIONAL", claims.get("rol", String.class));
        assertEquals("previsar-api-test", claims.getIssuer());
    }

    @Test
    @DisplayName("un token modificado (firma inválida) no se valida")
    void validarYExtraerClaims_tokenModificado_devuelveOptionalVacio() {
        String tokenOriginal = jwtService.generarToken(usuarioDePrueba);

        // Alteramos un caracter del MEDIO de la firma. El último caracter no sirve
        // porque en HS512 los últimos 2 bits son padding implícito de base64URL, y
        // varios caracteres mapean a los mismos bits significativos (flaky).
        int inicioFirma = tokenOriginal.lastIndexOf('.') + 1;
        int mitadFirma = inicioFirma + (tokenOriginal.length() - inicioFirma) / 2;
        char original = tokenOriginal.charAt(mitadFirma);
        String tokenAlterado = tokenOriginal.substring(0, mitadFirma)
                + (original == 'A' ? 'B' : 'A')
                + tokenOriginal.substring(mitadFirma + 1);

        Optional<Claims> claimsOpt = jwtService.validarYExtraerClaims(tokenAlterado);

        assertTrue(claimsOpt.isEmpty());
    }

    @Test
    @DisplayName("un token con formato basura no se valida")
    void validarYExtraerClaims_tokenBasura_devuelveOptionalVacio() {
        Optional<Claims> claimsOpt = jwtService.validarYExtraerClaims("esto-no-es-un-jwt");

        assertTrue(claimsOpt.isEmpty());
    }

    @Test
    @DisplayName("un token null o vacío no se valida")
    void validarYExtraerClaims_tokenNullOVacio_devuelveOptionalVacio() {
        assertTrue(jwtService.validarYExtraerClaims(null).isEmpty());
        assertTrue(jwtService.validarYExtraerClaims("").isEmpty());
        assertTrue(jwtService.validarYExtraerClaims("   ").isEmpty());
    }


    @Test
    @DisplayName("extraerEmail devuelve el subject del token")
    void extraerEmail_tokenValido_devuelveEmail() {
        String token = jwtService.generarToken(usuarioDePrueba);

        Optional<String> emailOpt = jwtService.extraerEmail(token);

        assertTrue(emailOpt.isPresent());
        assertEquals("sofia@example.com", emailOpt.get());
    }
}