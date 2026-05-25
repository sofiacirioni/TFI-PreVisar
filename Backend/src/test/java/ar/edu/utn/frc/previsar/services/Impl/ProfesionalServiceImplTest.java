package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.request.ProfesionalUpdateRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ProfesionalResponseDto;
import ar.edu.utn.frc.previsar.entities.*;
import ar.edu.utn.frc.previsar.enums.Rol;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.ProfesionalMapper;
import ar.edu.utn.frc.previsar.repositories.CondicionIvaRepository;
import ar.edu.utn.frc.previsar.repositories.ProfesionalRepository;
import ar.edu.utn.frc.previsar.repositories.RegionalRepository;
import ar.edu.utn.frc.previsar.repositories.RolRevisorRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de ProfesionalServiceImpl.
 *
 * Cubre obtención y actualización del perfil del profesional autenticado,
 * incluyendo el cálculo del flag esRevisor a través de RolRevisorRepository.
 */

@ExtendWith(MockitoExtension.class)
class ProfesionalServiceImplTest {
    @Mock
    private ProfesionalRepository profesionalRepository;

    @Mock
    private RegionalRepository regionalRepository;

    @Mock
    private CondicionIvaRepository condicionIvaRepository;

    @Mock
    private RolRevisorRepository rolRevisorRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ProfesionalMapper profesionalMapper;

    @InjectMocks
    private ProfesionalServiceImpl profesionalService;

    private Profesional profesional;
    private Regional regional;
    private CondicionIva condicionIva;
    private ProfesionalUpdateRequestDto updateRequest;

    @BeforeEach
    void setUp() {
        Usuario usuario = Usuario.builder()
                .id(1L)
                .email("sofia@example.com")
                .rol(Rol.PROFESIONAL)
                .activo(true)
                .build();

        Provincia provincia = Provincia.builder()
                .id(1L).nombre("Córdoba").codigo("CBA").build();

        regional = Regional.builder()
                .id(3L).nombre("San Francisco").provincia(provincia).build();

        condicionIva = CondicionIva.builder()
                .id(1L)
                .codigo("RESPONSABLE_INSCRIPTO")
                .descripcion("Responsable Inscripto")
                .activo(true).build();

        profesional = Profesional.builder()
                .id(10L)
                .usuario(usuario)
                .nombre("Sofía")
                .apellido("Cirioni")
                .dni("40123456")
                .cuit("27-40123456-3")
                .matricula("99999")
                .titulo("Tec. en Programación")
                .domicilio("Calle Falsa 123")
                .telefono("3511234567")
                .regional(regional)
                .condicionIva(condicionIva)
                .afiliadoCaja8470(false)
                .build();

        updateRequest = ProfesionalUpdateRequestDto.builder()
                .nombre("Sofía Actualizada")
                .apellido("Cirioni")
                .titulo("Tec. en Programación")
                .domicilio("Calle Nueva 456")
                .telefono("3519999999")
                .regionalId(3L)
                .condicionIvaId(1L)
                .afiliadoCaja8470(true)
                .build();
    }

    //Obtener perfil actual
    @Test
    @DisplayName("obtenerPerfilActual: devuelve el perfil del profesional (sin rol revisor)")
    void obtenerPerfilActual_sinRolRevisor_devuelveEsRevisorFalse() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesional);
        when(profesionalRepository.findByIdConRelaciones(10L))
                .thenReturn(Optional.of(profesional));
        when(rolRevisorRepository.existsByProfesionalId(10L)).thenReturn(false);

        ProfesionalResponseDto responseMock = ProfesionalResponseDto.builder()
                .id(10L)
                .email("sofia@example.com")
                .nombre("Sofía")
                .build();
        when(profesionalMapper.toResponse(profesional)).thenReturn(responseMock);

        ProfesionalResponseDto resultado = profesionalService.obtenerPerfilActual();

        assertNotNull(resultado);
        assertEquals(10L, resultado.getId());
        assertEquals(false, resultado.getEsRevisor());
    }

    @Test
    @DisplayName("obtenerPerfilActual: devuelve esRevisor=true si tiene RolRevisor")
    void obtenerPerfilActual_conRolRevisor_devuelveEsRevisorTrue() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesional);
        when(profesionalRepository.findByIdConRelaciones(10L))
                .thenReturn(Optional.of(profesional));
        when(rolRevisorRepository.existsByProfesionalId(10L)).thenReturn(true);

        ProfesionalResponseDto responseMock = ProfesionalResponseDto.builder()
                .id(10L).build();
        when(profesionalMapper.toResponse(profesional)).thenReturn(responseMock);

        ProfesionalResponseDto resultado = profesionalService.obtenerPerfilActual();

        assertEquals(true, resultado.getEsRevisor());
    }

    @Test
    @DisplayName("obtenerPerfilActual: lanza 404 si no se encuentra el profesional al recargar")
    void obtenerPerfilActual_noEncontrado_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesional);
        when(profesionalRepository.findByIdConRelaciones(10L))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> profesionalService.obtenerPerfilActual());
    }

    //Actualizar
    @Test
    @DisplayName("actualizarPerfilActual: actualiza los campos editables")
    void actualizarPerfilActual_caminoFeliz_modificaCampos() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesional);
        when(regionalRepository.findById(3L)).thenReturn(Optional.of(regional));
        when(condicionIvaRepository.findById(1L)).thenReturn(Optional.of(condicionIva));
        when(profesionalRepository.findByIdConRelaciones(10L))
                .thenReturn(Optional.of(profesional));
        when(rolRevisorRepository.existsByProfesionalId(10L)).thenReturn(false);

        ProfesionalResponseDto responseMock = ProfesionalResponseDto.builder()
                .id(10L).build();
        when(profesionalMapper.toResponse(any(Profesional.class))).thenReturn(responseMock);

        profesionalService.actualizarPerfilActual(updateRequest);

        // Los campos editables se modificaron en la entidad
        assertEquals("Sofía Actualizada", profesional.getNombre());
        assertEquals("Calle Nueva 456", profesional.getDomicilio());
        assertEquals("3519999999", profesional.getTelefono());
        assertEquals(true, profesional.getAfiliadoCaja8470());

        // Los campos inmutables NO cambiaron
        assertEquals("40123456", profesional.getDni());
        assertEquals("27-40123456-3", profesional.getCuit());
        assertEquals("99999", profesional.getMatricula());
    }

    @Test
    @DisplayName("actualizarPerfilActual: lanza 404 si la regional no existe")
    void actualizarPerfilActual_regionalInexistente_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesional);
        when(regionalRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> profesionalService.actualizarPerfilActual(updateRequest));

        // Los datos del profesional no debieron cambiar
        assertEquals("Sofía", profesional.getNombre());
    }

    @Test
    @DisplayName("actualizarPerfilActual: lanza 404 si la condicionIva no existe")
    void actualizarPerfilActual_condicionIvaInexistente_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesional);
        when(regionalRepository.findById(3L)).thenReturn(Optional.of(regional));
        when(condicionIvaRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> profesionalService.actualizarPerfilActual(updateRequest));

        assertEquals("Sofía", profesional.getNombre());
    }

    @Test
    @DisplayName("actualizarPerfilActual: setea esRevisor=true cuando corresponde")
    void actualizarPerfilActual_conRolRevisor_devuelveEsRevisorTrue() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesional);
        when(regionalRepository.findById(3L)).thenReturn(Optional.of(regional));
        when(condicionIvaRepository.findById(1L)).thenReturn(Optional.of(condicionIva));
        when(profesionalRepository.findByIdConRelaciones(10L))
                .thenReturn(Optional.of(profesional));
        when(rolRevisorRepository.existsByProfesionalId(10L)).thenReturn(true);

        ProfesionalResponseDto responseMock = ProfesionalResponseDto.builder()
                .id(10L).build();
        when(profesionalMapper.toResponse(any(Profesional.class))).thenReturn(responseMock);

        ProfesionalResponseDto resultado = profesionalService.actualizarPerfilActual(updateRequest);

        assertEquals(true, resultado.getEsRevisor());
    }
}