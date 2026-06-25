package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.AportesCalculadosDto;
import ar.edu.utn.frc.previsar.entities.ConceptoAporte;
import ar.edu.utn.frc.previsar.entities.ParametroAporte;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.entities.TipoTareaAporte;
import ar.edu.utn.frc.previsar.enums.BaseCalculo;
import ar.edu.utn.frc.previsar.enums.GrupoAporte;
import ar.edu.utn.frc.previsar.enums.TipoValor;
import ar.edu.utn.frc.previsar.repositories.ParametroAporteRepository;
import ar.edu.utn.frc.previsar.repositories.TipoTareaAporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AportesCalculatorServiceImplTest {
    @Mock
    private TipoTareaAporteRepository tipoTareaAporteRepo;
    @Mock
    private ParametroAporteRepository parametroRepo;

    @InjectMocks
    private AportesCalculatorServiceImpl service;

    private TipoTarea prdtrt;

    @BeforeEach
    void setUp() {
        prdtrt = new TipoTarea();
        prdtrt.setId(1L);
        prdtrt.setCodigo("PR-DT-RT");

        ConceptoAporte registroObra = concepto(1L, "REGISTRO_OBRA", "Registro de Obra", GrupoAporte.CIEC);
        ConceptoAporte arancel = concepto(2L, "ARANCEL_ADMIN", "Arancel administrativo", GrupoAporte.CIEC);
        ConceptoAporte cajaProf = concepto(3L, "CAJA_PROFESIONAL", "Aporte a la Caja (profesional)", GrupoAporte.CAJA);
        ConceptoAporte cajaComit = concepto(4L, "CAJA_COMITENTE", "Aporte a la Caja (comitente)", GrupoAporte.CAJA);

        when(tipoTareaAporteRepo.findByTipoTareaConConcepto(eq(1L))).thenReturn(List.of(
                tta(registroObra),
                tta(arancel),
                tta(cajaProf),
                tta(cajaComit)));

        when(parametroRepo.findVigente(eq(1L), any()))
                .thenReturn(Optional.of(porcentaje(new BigDecimal("5.0000"))));
        when(parametroRepo.findVigente(eq(2L), any()))
                .thenReturn(Optional.of(fijo(new BigDecimal("19000.0000"))));
        when(parametroRepo.findVigente(eq(3L), any()))
                .thenReturn(Optional.of(porcentaje(new BigDecimal("9.0000"))));
        when(parametroRepo.findVigente(eq(4L), any()))
                .thenReturn(Optional.of(porcentaje(new BigDecimal("9.0000"))));
    }

    @Test
    void calcula_aportes_PR_DT_RT() {
        BigDecimal honorarios = new BigDecimal("5623389.04");

        AportesCalculadosDto r = service.calcular(prdtrt, honorarios);

        assertThat(r.lineas()).hasSize(4);
        assertThat(montoDe(r, "REGISTRO_OBRA")).isEqualByComparingTo("281169.45");
        assertThat(montoDe(r, "ARANCEL_ADMIN")).isEqualByComparingTo("19000.00");
        assertThat(montoDe(r, "CAJA_PROFESIONAL")).isEqualByComparingTo("506105.01");
        assertThat(montoDe(r, "CAJA_COMITENTE")).isEqualByComparingTo("506105.01");
        assertThat(r.totalCiec()).isEqualByComparingTo("300169.45");
        assertThat(r.totalCaja()).isEqualByComparingTo("1012210.02");
        assertThat(r.total()).isEqualByComparingTo("1312379.47");
    }

    private BigDecimal montoDe(AportesCalculadosDto r, String codigo) {
        return r.lineas().stream()
                .filter(l -> l.conceptoCodigo().equals(codigo))
                .map(AportesCalculadosDto.LineaAporte::monto)
                .findFirst()
                .orElseThrow();
    }

    private ConceptoAporte concepto(Long id, String codigo, String nombre, GrupoAporte grupo) {
        ConceptoAporte c = new ConceptoAporte();
        c.setId(id);
        c.setCodigo(codigo);
        c.setNombre(nombre);
        c.setGrupo(grupo);
        return c;
    }

    private TipoTareaAporte tta(ConceptoAporte concepto) {
        TipoTareaAporte tta = new TipoTareaAporte();
        tta.setConcepto(concepto);
        return tta;
    }

    private ParametroAporte porcentaje(BigDecimal valor) {
        ParametroAporte p = new ParametroAporte();
        p.setTipoValor(TipoValor.PORCENTAJE);
        p.setBaseCalculo(BaseCalculo.HONORARIOS);
        p.setValor(valor);
        return p;
    }

    private ParametroAporte fijo(BigDecimal valor) {
        ParametroAporte p = new ParametroAporte();
        p.setTipoValor(TipoValor.FIJO);
        p.setValor(valor);
        return p;
    }
}
