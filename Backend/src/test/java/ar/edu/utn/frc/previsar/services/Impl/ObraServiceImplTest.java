package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.request.ObraRequestDto;
import ar.edu.utn.frc.previsar.dtos.response.ObraResponseDto;
import ar.edu.utn.frc.previsar.entities.Comitente;
import ar.edu.utn.frc.previsar.entities.Obra;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Usuario;
import ar.edu.utn.frc.previsar.enums.Rol;
import ar.edu.utn.frc.previsar.enums.TipoPersona;
import ar.edu.utn.frc.previsar.exception.ResourceNotFoundException;
import ar.edu.utn.frc.previsar.mapper.ObraMapper;
import ar.edu.utn.frc.previsar.repositories.ComitenteRepository;
import ar.edu.utn.frc.previsar.repositories.ObraRepository;
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
 * Tests unitarios de ObraServiceImpl.
 *
 * Foco principal:
 *   - Aislamiento entre profesionales (obras de uno no se ven desde otro).
 *   - Cascade de visibilidad (si el comitente padre está soft-deleted,
 *     las obras tampoco se ven).
 */

@ExtendWith(MockitoExtension.class)
class ObraServiceImplTest {
    @Mock
    private ObraRepository obraRepository;

    @Mock
    private ComitenteRepository comitenteRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ObraMapper obraMapper;

    @InjectMocks
    private ObraServiceImpl obraService;

    private Profesional profesionalActual;
    private Profesional otroProfesional;
    private Comitente comitenteDelActual;
    private Comitente comitenteDelOtro;
    private Obra obraDelActual;
    private Obra obraDelOtro;
    private ObraRequestDto request;

    @BeforeEach
    void setUp() {
        Usuario usuario1 = Usuario.builder()
                .id(1L).email("sofia@example.com").rol(Rol.PROFESIONAL).activo(true).build();
        profesionalActual = Profesional.builder()
                .id(10L).usuario(usuario1).nombre("Sofía").build();

        Usuario usuario2 = Usuario.builder()
                .id(2L).email("otro@example.com").rol(Rol.PROFESIONAL).activo(true).build();
        otroProfesional = Profesional.builder()
                .id(20L).usuario(usuario2).nombre("Otro").build();

        comitenteDelActual = Comitente.builder()
                .id(100L)
                .profesional(profesionalActual)
                .tipoPersona(TipoPersona.JURIDICA)
                .nombreRazonSocial("Los Varta SA")
                .dniCuit("30-71252386-3")
                .build();

        comitenteDelOtro = Comitente.builder()
                .id(200L)
                .profesional(otroProfesional)
                .tipoPersona(TipoPersona.JURIDICA)
                .nombreRazonSocial("Empresa Ajena SA")
                .dniCuit("30-99999999-9")
                .build();

        obraDelActual = Obra.builder()
                .id(1000L)
                .comitente(comitenteDelActual)
                .designacion("Proyecto BT MYKONOS")
                .calle("Rivadavia")
                .numero("162")
                .localidad("Villa María")
                .codigoPostal("5900")
                .build();

        obraDelOtro = Obra.builder()
                .id(2000L)
                .comitente(comitenteDelOtro)
                .designacion("Obra ajena")
                .calle("Ajena")
                .numero("1")
                .localidad("Ciudad ajena")
                .codigoPostal("X9999")
                .build();

        request = ObraRequestDto.builder()
                .designacion("Proyecto BT MYKONOS")
                .calle("Rivadavia")
                .numero("162")
                .localidad("Villa María")
                .codigoPostal("5900")
                .circunscripcion("01")
                .seccion("01")
                .manzana("072")
                .parcela("019")
                .build();
    }

    //Listar
    @Test
    @DisplayName("listarTodasMisObras: devuelve obras activas del profesional actual")
    void listarTodasMisObras_devuelveObrasDelActual() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(obraRepository.findActivasByProfesionalId(10L))
                .thenReturn(List.of(obraDelActual));
        when(obraMapper.toResponse(obraDelActual))
                .thenReturn(ObraResponseDto.builder().id(1000L).build());

        List<ObraResponseDto> resultado = obraService.listarTodasMisObras();

        assertEquals(1, resultado.size());
        assertEquals(1000L, resultado.get(0).getId());
    }

    @Test
    @DisplayName("listarPorComitente: devuelve obras del comitente propio")
    void listarPorComitente_propio_devuelveObras() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(100L)).thenReturn(Optional.of(comitenteDelActual));
        when(obraRepository.findByComitenteIdAndDeletedAtIsNull(100L))
                .thenReturn(List.of(obraDelActual));
        when(obraMapper.toResponse(obraDelActual))
                .thenReturn(ObraResponseDto.builder().id(1000L).build());

        List<ObraResponseDto> resultado = obraService.listarPorComitente(100L);

        assertEquals(1, resultado.size());
    }

    @Test
    @DisplayName("listarPorComitente: lanza 404 si el comitente es de otro profesional")
    void listarPorComitente_comitenteAjeno_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(200L)).thenReturn(Optional.of(comitenteDelOtro));

        assertThrows(ResourceNotFoundException.class,
                () -> obraService.listarPorComitente(200L));

        verify(obraRepository, never()).findByComitenteIdAndDeletedAtIsNull(any());
    }

    //Obtener por id
    @Test
    @DisplayName("obtenerPorId: devuelve obra que pertenece al profesional actual")
    void obtenerPorId_propia_devuelveObra() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(obraRepository.findById(1000L)).thenReturn(Optional.of(obraDelActual));
        when(obraMapper.toResponse(obraDelActual))
                .thenReturn(ObraResponseDto.builder().id(1000L).build());

        ObraResponseDto resultado = obraService.obtenerPorId(1000L);

        assertNotNull(resultado);
        assertEquals(1000L, resultado.getId());
    }

    @Test
    @DisplayName("obtenerPorId: lanza 404 si la obra no existe")
    void obtenerPorId_inexistente_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(obraRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> obraService.obtenerPorId(9999L));
    }

    @Test
    @DisplayName("obtenerPorId: lanza 404 si la obra es de OTRO profesional (aislamiento)")
    void obtenerPorId_deOtroProfesional_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(obraRepository.findById(2000L)).thenReturn(Optional.of(obraDelOtro));

        assertThrows(ResourceNotFoundException.class,
                () -> obraService.obtenerPorId(2000L));

        verify(obraMapper, never()).toResponse(any());
    }

    @Test
    @DisplayName("obtenerPorId: lanza 404 si la obra está soft-deleted")
    void obtenerPorId_obraSoftDeleted_lanza404() {
        obraDelActual.setDeletedAt(LocalDateTime.now());

        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(obraRepository.findById(1000L)).thenReturn(Optional.of(obraDelActual));

        assertThrows(ResourceNotFoundException.class,
                () -> obraService.obtenerPorId(1000L));
    }

    @Test
    @DisplayName("obtenerPorId: lanza 404 si el COMITENTE padre está soft-deleted (cascade)")
    void obtenerPorId_comitenteSoftDeleted_lanza404() {
        // La obra está activa, pero el comitente padre fue eliminado.
        // Cascade de visibilidad: la obra también debe "desaparecer".
        comitenteDelActual.setDeletedAt(LocalDateTime.now());

        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(obraRepository.findById(1000L)).thenReturn(Optional.of(obraDelActual));

        assertThrows(ResourceNotFoundException.class,
                () -> obraService.obtenerPorId(1000L));
    }

    //Crear
    @Test
    @DisplayName("crear: alta exitosa para comitente del profesional actual")
    void crear_comitentePropio_creaObra() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(100L)).thenReturn(Optional.of(comitenteDelActual));
        when(obraRepository.save(any(Obra.class)))
                .thenAnswer(inv -> {
                    Obra o = inv.getArgument(0);
                    o.setId(1001L);
                    return o;
                });
        when(obraMapper.toResponse(any(Obra.class)))
                .thenReturn(ObraResponseDto.builder().id(1001L).build());

        ObraResponseDto resultado = obraService.crear(100L, request);

        assertNotNull(resultado);
        assertEquals(1001L, resultado.getId());

        // Verificar que la obra se asoció al comitente correcto
        ArgumentCaptor<Obra> captor = ArgumentCaptor.forClass(Obra.class);
        verify(obraRepository).save(captor.capture());
        Obra guardada = captor.getValue();
        assertEquals(comitenteDelActual, guardada.getComitente());
        assertEquals("Proyecto BT MYKONOS", guardada.getDesignacion());
        assertEquals("01", guardada.getCircunscripcion());
    }

    @Test
    @DisplayName("crear: lanza 404 si el comitente es de OTRO profesional")
    void crear_comitenteAjeno_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(comitenteRepository.findById(200L)).thenReturn(Optional.of(comitenteDelOtro));

        assertThrows(ResourceNotFoundException.class,
                () -> obraService.crear(200L, request));

        // No debió guardarse nada
        verify(obraRepository, never()).save(any());
    }

    //Actualizar
    @Test
    @DisplayName("actualizar: modifica los campos de la obra del profesional")
    void actualizar_propia_modificaCampos() {
        ObraRequestDto nuevoRequest = ObraRequestDto.builder()
                .designacion("Nuevo nombre de obra")
                .calle("Nueva Calle")
                .numero("999")
                .localidad("Nueva Localidad")
                .codigoPostal("5000")
                .build();

        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(obraRepository.findById(1000L)).thenReturn(Optional.of(obraDelActual));
        when(obraMapper.toResponse(obraDelActual))
                .thenReturn(ObraResponseDto.builder().id(1000L).build());

        obraService.actualizar(1000L, nuevoRequest);

        // Los campos cambiaron en la entidad
        assertEquals("Nuevo nombre de obra", obraDelActual.getDesignacion());
        assertEquals("Nueva Calle", obraDelActual.getCalle());
        assertEquals("999", obraDelActual.getNumero());
        assertEquals("Nueva Localidad", obraDelActual.getLocalidad());
    }

    @Test
    @DisplayName("actualizar: lanza 404 si la obra es de OTRO profesional")
    void actualizar_obraAjena_lanza404() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(obraRepository.findById(2000L)).thenReturn(Optional.of(obraDelOtro));

        assertThrows(ResourceNotFoundException.class,
                () -> obraService.actualizar(2000L, request));

        // La obra ajena no debió modificarse
        assertEquals("Obra ajena", obraDelOtro.getDesignacion());
    }

    //Eliminar
    @Test
    @DisplayName("eliminar: hace soft delete de la obra del profesional")
    void eliminar_propia_haceSoftDelete() {
        when(securityUtils.getProfesionalActual()).thenReturn(profesionalActual);
        when(obraRepository.findById(1000L)).thenReturn(Optional.of(obraDelActual));

        assertNull(obraDelActual.getDeletedAt());

        obraService.eliminar(1000L);

        assertNotNull(obraDelActual.getDeletedAt());
    }
}