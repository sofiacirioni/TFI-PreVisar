package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.ObservacionDto;
import ar.edu.utn.frc.previsar.enums.NivelObservacion;
import ar.edu.utn.frc.previsar.repositories.DocumentoCargadoRepository;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios de ValidacionNivel2Service.
 *
 * Se prueban las reglas puras (coherencia de honorarios ±5% y presencia del CUIT
 * del comitente) directamente sobre texto, sin cargar PDFs reales. Las dependencias
 * se mockean solo para construir el servicio; las reglas no las usan.
 */
@ExtendWith(MockitoExtension.class)
class ValidacionNivel2ServiceTest {

    @Mock ExpedienteService expedienteService;
    @Mock DocumentoCargadoRepository documentoCargadoRepository;
    @Mock FileStorageService fileStorageService;
    @InjectMocks ValidacionNivel2Service service;

    private static final BigDecimal REF = new BigDecimal("5623389.04");
    private static final String CUIT = "30712523863";   // solo dígitos

    private static String codigo(List<ObservacionDto> obs) { return obs.get(0).codigo(); }
    private static NivelObservacion nivel(List<ObservacionDto> obs) { return obs.get(0).nivel(); }

    // ---------- Regla: honorarios referenciales ±5% ----------

    @Test
    @DisplayName("Honorarios: monto exacto no genera observación")
    void honorarios_montoExacto_sinObservacion() {
        assertTrue(service.verificarHonorarios("Honorarios referenciales: $ 5.623.389,04", REF).isEmpty());
    }

    @Test
    @DisplayName("Honorarios: monto dentro del ±5% no genera observación")
    void honorarios_dentroDeTolerancia_sinObservacion() {
        // 5.900.000 difiere ~4,9% (< 5%)
        assertTrue(service.verificarHonorarios("Monto: 5.900.000,00", REF).isEmpty());
    }

    @Test
    @DisplayName("Honorarios: monto fuera del ±5% advierte")
    void honorarios_fueraDeTolerancia_advierte() {
        // 6.500.000 difiere ~15,6%
        var obs = service.verificarHonorarios("Monto pactado: 6.500.000,00", REF);
        assertEquals("HONORARIOS_NO_COINCIDEN", codigo(obs));
        assertEquals(NivelObservacion.ADVERTENCIA, nivel(obs));
    }

    @Test
    @DisplayName("Honorarios: basta que un monto entre varios entre en tolerancia")
    void honorarios_algunMontoEntreVariosCoincide_sinObservacion() {
        assertTrue(service.verificarHonorarios("Subtotal 1.312.379,47 — Honorarios 5.623.389,04", REF).isEmpty());
    }

    @Test
    @DisplayName("Honorarios: texto sin montos informa (INFO)")
    void honorarios_textoSinMontos_info() {
        var obs = service.verificarHonorarios("Contrato de locación de servicios profesionales", REF);
        assertEquals("SIN_MONTOS", codigo(obs));
        assertEquals(NivelObservacion.INFO, nivel(obs));
    }

    @Test
    @DisplayName("Honorarios: referencial nulo no valida")
    void honorarios_referencialNulo_sinObservacion() {
        assertTrue(service.verificarHonorarios("Monto: 6.500.000,00", null).isEmpty());
    }

    // ---------- Extracción de montos ----------

    @Test
    @DisplayName("extraerMontos: reconoce formato argentino y números planos")
    void extraerMontos_formatoArgentino_yPlano() {
        var montos = ValidacionNivel2Service.extraerMontos("Honorarios 5.623.389,04 - arancel 19.000,00");
        assertTrue(montos.stream().anyMatch(m -> m.compareTo(new BigDecimal("5623389.04")) == 0));
        assertTrue(montos.stream().anyMatch(m -> m.compareTo(new BigDecimal("19000.00")) == 0));
    }

    // ---------- Regla: CUIT del comitente en el contrato ----------

    @Test
    @DisplayName("CUIT: presente con guiones no genera observación")
    void cuit_presenteConGuiones_sinObservacion() {
        assertTrue(service.verificarCuit("CUIT del comitente: 30-71252386-3", CUIT).isEmpty());
    }

    @Test
    @DisplayName("CUIT: presente sin formato no genera observación")
    void cuit_presenteSinFormato_sinObservacion() {
        assertTrue(service.verificarCuit("...comitente 30712523863 domiciliado...", CUIT).isEmpty());
    }

    @Test
    @DisplayName("CUIT: ausente advierte")
    void cuit_ausente_advierte() {
        var obs = service.verificarCuit("CUIT: 20-11111111-2", CUIT);
        assertEquals("CUIT_NO_COINCIDE", codigo(obs));
        assertEquals(NivelObservacion.ADVERTENCIA, nivel(obs));
    }

    @Test
    @DisplayName("CUIT: en blanco no valida")
    void cuit_enBlanco_sinObservacion() {
        assertTrue(service.verificarCuit("cualquier texto sin cuit", "").isEmpty());
    }
}
