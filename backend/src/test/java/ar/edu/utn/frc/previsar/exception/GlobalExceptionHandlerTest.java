package ar.edu.utn.frc.previsar.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests del mapeo excepción → HTTP del GlobalExceptionHandler.
 *
 * Foco: las fallas de autenticación de Spring Security lanzadas desde el login
 * tienen que salir como 401. Antes acá se importaba
 * {@code javax.naming.AuthenticationException} (JNDI), que nunca se lanza, así que
 * una cuenta dada de baja intentando loguearse caía en el catch-all y devolvía 500.
 *
 * Se usa standaloneSetup para ejercitar la resolución real de handlers de Spring
 * (que elige el más específico), en vez de llamar a mano a los métodos del advice.
 */
class GlobalExceptionHandlerTest {

    /** Controller de mentira: cada endpoint lanza la excepción que se quiere mapear. */
    @RestController
    static class ControllerQueFalla {

        @GetMapping("/boom/deshabilitada")
        String cuentaDeshabilitada() {
            throw new DisabledException("Cuenta deshabilitada");
        }

        @GetMapping("/boom/bloqueada")
        String cuentaBloqueada() {
            throw new LockedException("Cuenta bloqueada");
        }

        @GetMapping("/boom/credenciales")
        String credencialesInvalidas() {
            throw new BadCredentialsException("Bad credentials");
        }

        @GetMapping("/boom/negocio")
        String reglaDeNegocio() {
            throw new BusinessException("Regla violada");
        }

        @GetMapping("/boom/inesperada")
        String inesperada() {
            throw new IllegalStateException("bug");
        }
    }

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ControllerQueFalla())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    @DisplayName("Cuenta deshabilitada (baja de cuenta) devuelve 401, no 500")
    void cuentaDeshabilitadaDevuelve401() throws Exception {
        mockMvc.perform(get("/boom/deshabilitada"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/boom/deshabilitada"));
    }

    @Test
    @DisplayName("Cualquier AuthenticationException de Spring Security devuelve 401")
    void cuentaBloqueadaDevuelve401() throws Exception {
        mockMvc.perform(get("/boom/bloqueada"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    @DisplayName("BadCredentials conserva su handler específico y su mensaje propio")
    void badCredentialsUsaSuPropioHandler() throws Exception {
        mockMvc.perform(get("/boom/credenciales"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensaje").value("Email o password incorrectos"));
    }

    @Test
    @DisplayName("BusinessException sigue devolviendo 400")
    void businessDevuelve400() throws Exception {
        mockMvc.perform(get("/boom/negocio"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Regla violada"));
    }

    @Test
    @DisplayName("Una excepción no contemplada sigue cayendo en el catch-all 500")
    void inesperadaDevuelve500() throws Exception {
        mockMvc.perform(get("/boom/inesperada"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.mensaje").value("Error interno del servidor"));
    }
}
