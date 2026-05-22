package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.config.JwtProperties;
import ar.edu.utn.frc.previsar.dtos.request.LoginRequestDto;
import ar.edu.utn.frc.previsar.dtos.request.RegisterRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.AuthResponseDto;
import ar.edu.utn.frc.previsar.entities.CondicionIva;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Regional;
import ar.edu.utn.frc.previsar.entities.Usuario;
import ar.edu.utn.frc.previsar.enums.Rol;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.repositories.CondicionIvaRepository;
import ar.edu.utn.frc.previsar.repositories.ProfesionalRepository;
import ar.edu.utn.frc.previsar.repositories.RegionalRepository;
import ar.edu.utn.frc.previsar.repositories.UsuarioRepository;
import ar.edu.utn.frc.previsar.security.JwtService;
import ar.edu.utn.frc.previsar.services.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.beans.Transient;

/**
 * Implementación del servicio de autenticación.
 *
 * Responsabilidades:
 *   - Registrar profesionales (Usuario + Profesional en una transacción).
 *   - Autenticar credenciales y generar JWT.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UsuarioRepository usuarioRepository;
    private final ProfesionalRepository profesionalRepository;
    private final RegionalRepository regionalRepository;
    private final CondicionIvaRepository condicionIvaRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponseDto registrar(RegisterRequestDto request) {
        log.info("Registrando nuevo profesional con email {}", request.getEmail());

        // 1. Validar unicidad
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException("Ya existe un usuario con ese email");
        }
        if (profesionalRepository.existsByCuit(request.getCuit())) {
            throw new BusinessException("Ya existe un profesional con ese CUIT");
        }
        if (profesionalRepository.existsByMatricula(request.getMatricula())) {
            throw new BusinessException("Ya existe un profesional con esa matrícula");
        }

        // 2. Cargar referencias a catálogos
        Regional regional = regionalRepository.findById(request.getRegionalId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Regional no encontrada: " + request.getRegionalId()));

        CondicionIva condicionIva = condicionIvaRepository.findById(request.getCondicionIvaId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "CondicionIva no encontrada: " + request.getCondicionIvaId()));

        // 3. Crear Usuario con password hasheado
        Usuario usuario = Usuario.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .rol(Rol.PROFESIONAL)
                .activo(true)
                .build();

        usuario = usuarioRepository.save(usuario);

        // 4. Crear Profesional asociado
        Profesional profesional = Profesional.builder()
                .usuario(usuario)
                .nombre(request.getNombre())
                .apellido(request.getApellido())
                .dni(request.getDni())
                .cuit(request.getCuit())
                .matricula(request.getMatricula())
                .titulo(request.getTitulo())
                .domicilio(request.getDomicilio())
                .telefono(request.getTelefono())
                .regional(regional)
                .condicionIva(condicionIva)
                .afiliadoCaja8470(request.getAfiliadoCaja8470())
                .build();

        profesionalRepository.save(profesional);

        log.info("Profesional registrado: id usuario={}, email={}",
                usuario.getId(), usuario.getEmail());

        // 5. Generar token y devolver
        String token = jwtService.generarToken(usuario);
        return construirAuthResponse(token, usuario);
    }

    @Override
    public AuthResponseDto login(LoginRequestDto request) {
        log.info("Intento de login para email {}", request.getEmail());

        // 1. Validar credenciales (lanza excepción si fallan)
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(), request.getPassword()));

        // 2. Si llega acá, las credenciales son válidas. Carga el usuario.
        Usuario usuario = usuarioRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario no encontrado tras autenticación"));

        log.info("Login exitoso para email {}", usuario.getEmail());

        // 3. Generar token y devolver
        String token = jwtService.generarToken(usuario);
        return construirAuthResponse(token, usuario);
    }

    // -------------------------- Helpers --------------------------

    private AuthResponseDto construirAuthResponse(String token, Usuario usuario) {
        return AuthResponseDto.builder()
                .token(token)
                .tipoToken("Bearer")
                .expiraEnMs(jwtProperties.getExpirationMs())
                .email(usuario.getEmail())
                .rol(usuario.getRol().name())
                .build();
    }

}
