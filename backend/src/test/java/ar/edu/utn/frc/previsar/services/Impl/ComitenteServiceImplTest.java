package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.request.ComitenteRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ComitenteResponseDto;
import ar.edu.utn.frc.previsar.entities.Comitente;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Usuario;
import ar.edu.utn.frc.previsar.enums.Rol;
import ar.edu.utn.frc.previsar.enums.TipoPersona;
import ar.edu.utn.frc.previsar.exception.BusinessException;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.ComitenteMapper;
import ar.edu.utn.frc.previsar.repositories.ComitenteRepository;
import ar.edu.utn.frc.previsar.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios de ComitenteServiceImpl.
 *
 * Foco principal: validar el aislamiento entre profesionales. Ningún
 * profesional debe poder ver, modificar o eliminar comitentes ajenos.
 */

@ExtendWith(MockitoExtension.class)
class ComitenteServiceImplTest {
    @Mock
    private ComitenteRepository comitenteRepository;
    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ComitenteMapper comitenteMapper;

    @InjectMocks
    private ComitenteServiceImpl comitenteService;

    private Profesional profesionalActual;
    private Profesional otroProfesional;
    private Comitente comitenteDelActual;
    private Comitente comitenteDelOtro;
    private ComitenteRequestDto request;

    @BeforeEach
    void setUp() {
        Usuario usuario1 = Usuario.builder()
                .id(1L).email("sofia@example.com").rol(Rol.PROFESIONAL).activo(true).build();
        profesionalActual = Profesional.builder()
                .id(10L).usuario(usuario1).nombre("Sofía").apellido("Cirioni")
                .cuit("27-40123456-3").build();

        Usuario usuario2 = Usuario.builder()
                .id(2L).email("otro@example.com").rol(Rol.PROFESIONAL).activo(true).build();
        otroProfesional = Profesional.builder()
                .id(20L).usuario(usuario2).nombre("Otro").apellido("Profesional")
                .cuit("20-12345678-9").build();

        comitenteDelActual = Comitente.builder()
                .id(100L)
                .profesional(profesionalActual)
                .tipoPersona(TipoPersona.JURIDICA)
                .nombreRazonSocial("Los Varta SA")
                .dniCuit("30-71252386-3")
                .domicilio("Esquiu 559")
                .build();

        comitenteDelOtro = Comitente.builder()
                .id(200L)
                .profesional(otroProfesional)
                .tipoPersona(TipoPersona.JURIDICA)
                .nombreRazonSocial("Empresa Ajena SA")
                .dniCuit("30-99999999-9")
                .domicilio("Calle Ajena 123")
                .build();

        request = ComitenteRequestDto.builder()
                .tipoPersona(TipoPersona.JURIDICA)
                .nombreRazonSocial("Los Varta SA")
                .dniCuit("30-71252386-3")
                .domicilio("Esquiu 559")
                .build();
    }

    // Listar
    @Test
    @DisplayName("listar: devuelve los comitentes activos del profesional actual")
    void listar_devuelveComitentesDelActual() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findByProfesionalIdAndDeletedAtIsNull(10L))
                .thenReturn(List.of(comitenteDelActual));
        when(comitenteMapper.toResponse(comitenteDelActual))
                .thenReturn(ComitenteResponseDto.builder().id(100L).build());

        List<ComitenteResponseDto> resultado = comitenteService.listar();

        assertEquals(1, resultado.size());
        assertEquals(100L, resultado.get(0).getId());
    }

    // Obtener por Id
    @Test
    @DisplayName("obtenerPorId: devuelve el comitente que pertenece al profesional actual")
    void obtenerPorId_propio_devuelveComitente() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(100L)).thenReturn(Optional.of(comitenteDelActual));
        when(comitenteMapper.toResponse(comitenteDelActual))
                .thenReturn(ComitenteResponseDto.builder().id(100L).build());

        ComitenteResponseDto resultado = comitenteService.obtenerPorId(100L);

        assertNotNull(resultado);
        assertEquals(100L, resultado.getId());
    }

    @Test
    @DisplayName("obtenerPorId: lanza 404 si el comitente no existe")
    void obtenerPorId_inexistente_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> comitenteService.obtenerPorId(999L));
    }

    @Test
    @DisplayName("obtenerPorId: lanza 404 si el comitente es de OTRO profesional (aislamiento)")
    void obtenerPorId_deOtroProfesional_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(200L)).thenReturn(Optional.of(comitenteDelOtro));

        // Importante: NO se debe revelar que el id existe.
        // Devolver 404 (no 403) para no filtrar información.
        assertThrows(ResourceNotFoundException.class,
                () -> comitenteService.obtenerPorId(200L));

        // El mapper NO debió ser llamado (no debe llegar a serializar nada)
        verify(comitenteMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("obtenerPorId: lanza 404 si el comitente está soft-deleted")
    void obtenerPorId_softDeleted_lanza404() {
        comitenteDelActual.setDeletedAt(LocalDateTime.now());

        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(100L)).thenReturn(Optional.of(comitenteDelActual));

        assertThrows(ResourceNotFoundException.class,
                () -> comitenteService.obtenerPorId(100L));
    }

    // Buscar por DniCuit
    @Test
    @DisplayName("buscarPorDniCuit: devuelve el comitente si existe en la cartera")
    void buscarPorDniCuit_existe_devuelveComitente() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findByProfesionalIdAndDniCuitAndDeletedAtIsNull(
                10L, "30-71252386-3"))
                .thenReturn(Optional.of(comitenteDelActual));
        when(comitenteMapper.toResponse(comitenteDelActual))
                .thenReturn(ComitenteResponseDto.builder().id(100L).build());

        Optional<ComitenteResponseDto> resultado =
                comitenteService.buscarPorDniCuit("30-71252386-3");

        assertTrue(resultado.isPresent());
        assertEquals(100L, resultado.get().getId());
    }

    @Test
    @DisplayName("buscarPorDniCuit: devuelve Optional.empty si no existe")
    void buscarPorDniCuit_noExiste_devuelveEmpty() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findByProfesionalIdAndDniCuitAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.empty());

        Optional<ComitenteResponseDto> resultado =
                comitenteService.buscarPorDniCuit("99-99999999-9");

        assertTrue(resultado.isEmpty());
    }

    // Crear
    @Test
    @DisplayName("crear: alta exitosa cuando no hay duplicado activo")
    void crear_sinDuplicado_creaComitente() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.existsByProfesionalIdAndDniCuitAndDeletedAtIsNull(
                10L, "30-71252386-3")).thenReturn(false);
        when(comitenteRepository.save(any(Comitente.class)))
                .thenAnswer(inv -> {
                    Comitente c = inv.getArgument(0);
                    c.setId(101L);
                    return c;
                });
        when(comitenteMapper.toResponse(any(Comitente.class)))
                .thenReturn(ComitenteResponseDto.builder().id(101L).build());

        ComitenteResponseDto resultado = comitenteService.crear(request);

        assertNotNull(resultado);
        assertEquals(101L, resultado.getId());

        // Verificar que la entidad se construyó con el profesional correcto
        ArgumentCaptor<Comitente> captor = ArgumentCaptor.forClass(Comitente.class);
        verify(comitenteRepository).save(captor.capture());
        Comitente guardado = captor.getValue();
        assertEquals(profesionalActual, guardado.getProfesional());
        assertEquals("Los Varta SA", guardado.getNombreRazonSocial());
        assertEquals(TipoPersona.JURIDICA, guardado.getTipoPersona());
    }

    @Test
    @DisplayName("crear: lanza BusinessException si hay duplicado activo")
    void crear_duplicadoActivo_lanzaBusinessException() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.existsByProfesionalIdAndDniCuitAndDeletedAtIsNull(
                10L, "30-71252386-3")).thenReturn(true);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> comitenteService.crear(request));

        assertTrue(ex.getMessage().toLowerCase().contains("dni") ||
                ex.getMessage().toLowerCase().contains("cuit"));

        verify(comitenteRepository, never()).save(any());
    }

    // Actualizar
    @Test
    @DisplayName("actualizar: modifica los campos del comitente del profesional")
    void actualizar_propio_modificaCampos() {
        ComitenteRequestDto nuevoRequest = ComitenteRequestDto.builder()
                .tipoPersona(TipoPersona.JURIDICA)
                .nombreRazonSocial("Los Varta SA - Modificada")
                .dniCuit("30-71252386-3")
                .domicilio("Nueva Dirección 456")
                .email("nuevo@example.com")
                .telefono("3534567899")
                .build();

        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(100L)).thenReturn(Optional.of(comitenteDelActual));
        when(comitenteMapper.toResponse(comitenteDelActual))
                .thenReturn(ComitenteResponseDto.builder().id(100L).build());

        comitenteService.actualizar(100L, nuevoRequest);

        // Verificar que los campos cambiaron en la entidad
        assertEquals("Los Varta SA - Modificada", comitenteDelActual.getNombreRazonSocial());
        assertEquals("Nueva Dirección 456", comitenteDelActual.getDomicilio());
        assertEquals("nuevo@example.com", comitenteDelActual.getEmail());
        assertEquals("3534567899", comitenteDelActual.getTelefono());
    }

    @Test
    @DisplayName("actualizar: lanza 404 si el comitente pertenece a otro profesional (aislamiento)")
    void actualizar_deOtroProfesional_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(200L)).thenReturn(Optional.of(comitenteDelOtro));

        assertThrows(ResourceNotFoundException.class,
                () -> comitenteService.actualizar(200L, request));

        // Los datos del comitente ajeno NO deben haber cambiado
        assertEquals("Empresa Ajena SA", comitenteDelOtro.getNombreRazonSocial());
    }

    // Eliminar
    @Test
    @DisplayName("eliminar: hace soft delete del comitente del profesional")
    void eliminar_propio_hacesoftDelete() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(100L)).thenReturn(Optional.of(comitenteDelActual));

        assertNull(comitenteDelActual.getDeletedAt());

        comitenteService.eliminar(100L);

        // El comitente debió quedar con deletedAt seteado
        assertNotNull(comitenteDelActual.getDeletedAt());
    }

    @Test
    @DisplayName("eliminar: lanza 404 si el comitente no existe")
    void eliminar_inexistente_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> comitenteService.eliminar(999L));
    }

    @Test
    @DisplayName("eliminar: lanza 404 si el comitente pertenece a otro profesional (aislamiento)")
    void eliminar_deOtroProfesional_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(200L)).thenReturn(Optional.of(comitenteDelOtro));

        assertThrows(ResourceNotFoundException.class,
                () -> comitenteService.eliminar(200L));

        // El comitente ajeno NO debió quedar marcado como eliminado
        assertNull(comitenteDelOtro.getDeletedAt());
    }

    @Test
    @DisplayName("eliminar: lanza 404 si el comitente ya está soft-deleted")
    void eliminar_yaSoftDeleted_lanza404() {
        LocalDateTime deletedAtOriginal = LocalDateTime.now().minusDays(1);
        comitenteDelActual.setDeletedAt(deletedAtOriginal);

        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(100L)).thenReturn(Optional.of(comitenteDelActual));

        assertThrows(ResourceNotFoundException.class,
                () -> comitenteService.eliminar(100L));

        // No debió pisarse el deletedAt original
        assertEquals(deletedAtOriginal, comitenteDelActual.getDeletedAt());
    }

    @Test
    @DisplayName("buscarPorDniCuit: devuelve empty si el dniCuit existe pero pertenece a otro profesional (aislamiento)")
    void buscarPorDniCuit_existenteDeOtroProfesional_devuelveEmpty() {
        // El query del repo filtra por profesionalId, así que si el dniCuit
        // existe en la base pero es de otro profesional, devuelve Optional.empty().
        String dniCuit = "30-71252386-3";
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findByProfesionalIdAndDniCuitAndDeletedAtIsNull(
                10L, dniCuit))
                .thenReturn(Optional.empty());

        Optional<ComitenteResponseDto> resultado = comitenteService.buscarPorDniCuit(dniCuit);

        assertTrue(resultado.isEmpty());
        verify(comitenteMapper, never()).toResponse(any());
    }
}