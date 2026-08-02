package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.response.TipoTareaResponseDto;
import ar.edu.utn.frc.previsar.entities.CondicionIva;
import ar.edu.utn.frc.previsar.entities.Provincia;
import ar.edu.utn.frc.previsar.entities.Regional;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.entities.Titulo;
import ar.edu.utn.frc.previsar.mapper.EspecialidadMapper;
import ar.edu.utn.frc.previsar.mapper.TipoTareaMapper;
import ar.edu.utn.frc.previsar.repositories.CondicionIvaRepository;
import ar.edu.utn.frc.previsar.repositories.EspecialidadRepository;
import ar.edu.utn.frc.previsar.repositories.ProvinciaRepository;
import ar.edu.utn.frc.previsar.repositories.RegionalRepository;
import ar.edu.utn.frc.previsar.repositories.TipoTareaRepository;
import ar.edu.utn.frc.previsar.repositories.TituloRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests de los catálogos que alimentan los desplegables del frontend.
 *
 * Lo que importa acá es que se devuelvan aplanados (la regional trae su provincia)
 * y que se respete el filtro de activos: un título o una especialidad dados de baja
 * no pueden seguir apareciendo en un formulario.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CatalogoServiceImplTest {

    @Mock private ProvinciaRepository provinciaRepository;
    @Mock private RegionalRepository regionalRepository;
    @Mock private CondicionIvaRepository condicionIvaRepository;
    @Mock private TituloRepository tituloRepository;
    @Mock private TipoTareaRepository tipoTareaRepository;
    @Mock private TipoTareaMapper tipoTareaMapper;
    @Mock private EspecialidadRepository especialidadRepository;
    @Mock private EspecialidadMapper especialidadMapper;

    @InjectMocks private CatalogoServiceImpl catalogoService;

    @Test
    @DisplayName("Las provincias vienen ordenadas por nombre")
    void listaProvincias() {
        when(provinciaRepository.findAll(any(Sort.class))).thenReturn(List.of(
                Provincia.builder().id(1L).nombre("Córdoba").codigo("CBA").build()));

        var provincias = catalogoService.listarProvincias();

        assertThat(provincias).hasSize(1);
        assertThat(provincias.get(0).getNombre()).isEqualTo("Córdoba");
        assertThat(provincias.get(0).getCodigo()).isEqualTo("CBA");
        verify(provinciaRepository).findAll(Sort.by("nombre"));
    }

    @Test
    @DisplayName("Cada regional viaja con su provincia aplanada, para el desplegable")
    void listaRegionalesConSuProvincia() {
        Provincia cordoba = Provincia.builder().id(1L).nombre("Córdoba").codigo("CBA").build();
        when(regionalRepository.findAll(any(Sort.class))).thenReturn(List.of(
                Regional.builder().id(10L).nombre("Regional Centro").provincia(cordoba).build()));

        var regionales = catalogoService.listarRegionales();

        assertThat(regionales).hasSize(1);
        assertThat(regionales.get(0).getNombre()).isEqualTo("Regional Centro");
        assertThat(regionales.get(0).getProvinciaId()).isEqualTo(1L);
        assertThat(regionales.get(0).getProvinciaNombre()).isEqualTo("Córdoba");
    }

    @Test
    @DisplayName("Las condiciones de IVA traen código y descripción")
    void listaCondicionesIva() {
        when(condicionIvaRepository.findAll()).thenReturn(List.of(
                CondicionIva.builder().id(1L).codigo("RI").descripcion("Responsable Inscripto")
                        .activo(true).build()));

        var condiciones = catalogoService.listarCondicionesIva();

        assertThat(condiciones).hasSize(1);
        assertThat(condiciones.get(0).getCodigo()).isEqualTo("RI");
        assertThat(condiciones.get(0).getDescripcion()).isEqualTo("Responsable Inscripto");
    }

    @Test
    @DisplayName("Los títulos solo traen los activos e indican cuál permite texto libre")
    void listaTitulosActivos() {
        when(tituloRepository.findAllByActivoTrueOrderByNombreAsc()).thenReturn(List.of(
                Titulo.builder().id(1L).nombre("Ingeniero Electricista")
                        .permiteTextoLibre(false).activo(true).build(),
                Titulo.builder().id(2L).nombre("Otro")
                        .permiteTextoLibre(true).activo(true).build()));

        var titulos = catalogoService.listarTitulos();

        assertThat(titulos).hasSize(2);
        assertThat(titulos.get(1).getNombre()).isEqualTo("Otro");
        assertThat(titulos.get(1).getPermiteTextoLibre()).isTrue();
    }

    @Test
    @DisplayName("Sin especialidad se listan todos los tipos de tarea activos")
    void listaTodosLosTiposDeTareaSinFiltro() {
        TipoTarea prDtRt = new TipoTarea();
        prDtRt.setId(10L);
        when(tipoTareaRepository.findByActivoTrueOrderByOrden()).thenReturn(List.of(prDtRt));
        when(tipoTareaMapper.toResponse(prDtRt)).thenReturn(new TipoTareaResponseDto());

        assertThat(catalogoService.listarTiposTarea(null)).hasSize(1);
        verify(tipoTareaRepository, never()).findByEspecialidadIdAndActivoTrueOrderByOrden(any());
    }

    @Test
    @DisplayName("Con especialidad se filtra por ella")
    void filtraTiposDeTareaPorEspecialidad() {
        TipoTarea prDtRt = new TipoTarea();
        prDtRt.setId(10L);
        when(tipoTareaRepository.findByEspecialidadIdAndActivoTrueOrderByOrden(1L))
                .thenReturn(List.of(prDtRt));
        when(tipoTareaMapper.toResponse(prDtRt)).thenReturn(new TipoTareaResponseDto());

        assertThat(catalogoService.listarTiposTarea(1L)).hasSize(1);
        verify(tipoTareaRepository, never()).findByActivoTrueOrderByOrden();
    }
}
