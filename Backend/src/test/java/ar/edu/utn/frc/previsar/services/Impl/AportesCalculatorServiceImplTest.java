package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.AportesCalculados;
import ar.edu.utn.frc.previsar.entities.ParametroAporte;
import ar.edu.utn.frc.previsar.entities.TipoTarea;
import ar.edu.utn.frc.previsar.enums.BaseCalculo;
import ar.edu.utn.frc.previsar.enums.ConceptoAporte;
import ar.edu.utn.frc.previsar.enums.TipoValor;
import ar.edu.utn.frc.previsar.repositories.ParametroAporteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AportesCalculatorServiceImplTest {
    @Mock
    private ParametroAporteRepository parametroRepo;

    @InjectMocks
    private AportesCalculatorServiceImpl service;

    private TipoTarea prdtrt;

    @BeforeEach
    void setUp() {
        prdtrt = new TipoTarea();
        prdtrt.setCodigo("PR-DT-RT");
        prdtrt.setAplicaRod(true);
        prdtrt.setAplicaArancelAdmin(true);
        prdtrt.setAplicaCaja(true);

        when(parametroRepo.findVigente(eq(ConceptoAporte.ROD), any()))
                .thenReturn(Optional.of(porcentaje(new BigDecimal("5.0000"))));
        when(parametroRepo.findVigente(eq(ConceptoAporte.CAJA_PROFESIONAL), any()))
                .thenReturn(Optional.of(porcentaje(new BigDecimal("9.0000"))));
        when(parametroRepo.findVigente(eq(ConceptoAporte.CAJA_COMITENTE), any()))
                .thenReturn(Optional.of(porcentaje(new BigDecimal("9.0000"))));
        when(parametroRepo.findVigente(eq(ConceptoAporte.ARANCEL_ADMIN), any()))
                .thenReturn(Optional.of(fijo(new BigDecimal("19000.0000"))));
    }

    @Test
    void calcula_aportes_PR_DT_RT() {
        BigDecimal honorarios = new BigDecimal("5623389.04");

        AportesCalculados r = service.calcular(prdtrt, honorarios);

        assertThat(r.aporteRod()).isEqualByComparingTo("281169.45");
        assertThat(r.aporteCajaProfesional()).isEqualByComparingTo("506105.01");
        assertThat(r.aporteCajaComitente()).isEqualByComparingTo("506105.01");
        assertThat(r.aporteArancelAdmin()).isEqualByComparingTo("19000.00");
        assertThat(r.totalCiec()).isEqualByComparingTo("300169.45");
        assertThat(r.total()).isEqualByComparingTo("1312379.47");
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