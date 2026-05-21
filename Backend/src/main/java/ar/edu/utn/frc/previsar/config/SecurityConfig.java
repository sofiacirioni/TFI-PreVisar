package ar.edu.utn.frc.previsar.config;

import ar.edu.utn.frc.previsar.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuración central de Spring Security para PreVisar.
 *
 * Define:
 *   - Qué endpoints son públicos (no requieren autenticación).
 *   - Qué endpoints requieren autenticación.
 *   - Cómo se gestionan las sesiones (stateless porque usamos JWT).
 *   - Qué password encoder se usa (BCrypt).
 *   - Dónde se inserta el filtro JWT en la cadena.
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final UserDetailsService userDetailsService;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // CSRF deshabilitado porque usamos JWT, no cookies de sesión.
                // CSRF protege contra ataques con cookies; sin cookies no aplica.
                .csrf(AbstractHttpConfigurer::disable)

                // Stateless: el server NO guarda sesiones. Cada request se autentica
                // sola con su JWT.
                .sessionManagement(sm -> sm
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Reglas de autorización por endpoint
                .authorizeHttpRequests(auth -> auth
                        // Endpoints públicos
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/error").permitAll()

                        // Todo lo demás requiere autenticación
                        .anyRequest().authenticated()
                )

                // Insertar nuestro filtro JWT antes del filtro estándar de Spring
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt para hashear passwords. Bean que Spring usa donde haga falta.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Provider que conecta el UserDetailsService con el password encoder.
     * Lo usa el AuthenticationManager para autenticar usuarios.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Bean del AuthenticationManager, expuesto para que el AuthService lo use
     * en el login. Spring lo construye automáticamente con el provider de arriba.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
