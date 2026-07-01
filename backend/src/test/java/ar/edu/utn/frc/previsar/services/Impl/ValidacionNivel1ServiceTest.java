package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.dtos.ObservacionDto;
import ar.edu.utn.frc.previsar.dtos.ValidacionResultadoDto;
import ar.edu.utn.frc.previsar.entities.DocumentoCargado;
import ar.edu.utn.frc.previsar.entities.DocumentoRequerido;
import ar.edu.utn.frc.previsar.enums.NivelObservacion;
import ar.edu.utn.frc.previsar.repositories.DocumentoCargadoRepository;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Tests unitarios de ValidacionNivel1Service.
 *
 * Cubre la geometría A4 (esA4, package-private) de forma directa, y las reglas de
 * validar() que no dependen de un PDF válido (imagen, duplicados por hash, límite de
 * tamaño y PDF ilegible), mockeando repositorio y storage.
 */
@ExtendWith(MockitoExtension.class)
class ValidacionNivel1ServiceTest {

    @Mock ExpedienteService expedienteService;
    @Mock DocumentoCargadoRepository documentoCargadoRepository;
    @Mock FileStorageService storage;
    @InjectMocks ValidacionNivel1Service service;

    private static final long EXP_ID = 1L;

    // A4 en puntos: 595.276 x 841.890, tolerancia ±5.
    private static final float A4_W = 595.276f, A4_H = 841.890f;

    // ---------- Geometría A4 ----------

    @Test
    @DisplayName("esA4: A4 vertical exacto")
    void esA4_verticalExacto() {
        assertTrue(service.esA4(A4_W, A4_H));
    }

    @Test
    @DisplayName("esA4: A4 horizontal (apaisado) también es válido")
    void esA4_horizontal() {
        assertTrue(service.esA4(A4_H, A4_W));
    }

    @Test
    @DisplayName("esA4: dentro de la tolerancia de ±5 pt")
    void esA4_dentroDeTolerancia() {
        assertTrue(service.esA4(A4_W + 4f, A4_H - 3f));
    }

    @Test
    @DisplayName("esA4: fuera de tolerancia no es A4")
    void esA4_fueraDeTolerancia() {
        assertFalse(service.esA4(A4_W, A4_H + 8f)); // 8 pt > 5 pt
        assertFalse(service.esA4(700f, 900f));      // otro tamaño
    }

    // ---------- Reglas de validar() ----------

    @Test
    @DisplayName("validar: una imagen genera ES_IMAGEN (INFO) y sin generales")
    void validar_imagen_generaInfo() {
        DocumentoCargado img = imagen(1L, "a.png", "foto.png");
        when(documentoCargadoRepository.findByExpedienteIdAndActivoTrue(EXP_ID)).thenReturn(List.of(img));
        when(storage.leerBytes("a.png")).thenReturn(new byte[]{1, 2, 3});

        ValidacionResultadoDto r = service.validar(EXP_ID);

        assertEquals(1, r.documentos().size());
        ObservacionDto obs = r.documentos().get(0).observaciones().get(0);
        assertEquals("ES_IMAGEN", obs.codigo());
        assertEquals(NivelObservacion.INFO, obs.nivel());
        assertTrue(r.generales().isEmpty());
    }

    @Test
    @DisplayName("validar: dos archivos con el mismo contenido generan DUPLICADO")
    void validar_archivosIdenticos_generaDuplicado() {
        DocumentoCargado a = imagen(1L, "a.png", "copia1.png");
        DocumentoCargado b = imagen(2L, "b.png", "copia2.png");
        when(documentoCargadoRepository.findByExpedienteIdAndActivoTrue(EXP_ID)).thenReturn(List.of(a, b));
        when(storage.leerBytes("a.png")).thenReturn(new byte[]{9, 9, 9});
        when(storage.leerBytes("b.png")).thenReturn(new byte[]{9, 9, 9});   // mismo contenido

        ValidacionResultadoDto r = service.validar(EXP_ID);

        assertTrue(tieneCodigo(r.generales(), "DUPLICADO"));
    }

    @Test
    @DisplayName("validar: archivos con distinto contenido no generan DUPLICADO")
    void validar_archivosDistintos_sinDuplicado() {
        DocumentoCargado a = imagen(1L, "a.png", "uno.png");
        DocumentoCargado b = imagen(2L, "b.png", "dos.png");
        when(documentoCargadoRepository.findByExpedienteIdAndActivoTrue(EXP_ID)).thenReturn(List.of(a, b));
        when(storage.leerBytes("a.png")).thenReturn(new byte[]{1, 1, 1});
        when(storage.leerBytes("b.png")).thenReturn(new byte[]{2, 2, 2});

        ValidacionResultadoDto r = service.validar(EXP_ID);

        assertFalse(tieneCodigo(r.generales(), "DUPLICADO"));
    }

    @Test
    @DisplayName("validar: total > 32 MB genera EXCEDE_32MB (ADVERTENCIA)")
    void validar_excedeTamano_advierte() {
        DocumentoCargado grande = imagen(1L, "big.png", "grande.png");
        when(documentoCargadoRepository.findByExpedienteIdAndActivoTrue(EXP_ID)).thenReturn(List.of(grande));
        when(storage.leerBytes("big.png")).thenReturn(new byte[33 * 1024 * 1024]); // > 32 MB

        ValidacionResultadoDto r = service.validar(EXP_ID);

        assertTrue(r.generales().stream()
                .anyMatch(o -> o.codigo().equals("EXCEDE_32MB") && o.nivel() == NivelObservacion.ADVERTENCIA));
    }

    @Test
    @DisplayName("validar: PDF ilegible (slot que valida A4) genera NO_ES_PDF")
    void validar_pdfIlegible_generaNoEsPdf() {
        DocumentoCargado pdf = pdf(1L, "roto.pdf", "roto.pdf", true);
        when(documentoCargadoRepository.findByExpedienteIdAndActivoTrue(EXP_ID)).thenReturn(List.of(pdf));
        when(storage.leerBytes("roto.pdf")).thenReturn("no soy un pdf".getBytes());

        ValidacionResultadoDto r = service.validar(EXP_ID);

        assertEquals("NO_ES_PDF", r.documentos().get(0).observaciones().get(0).codigo());
    }

    @Test
    @DisplayName("validar: plano (valida_a4=false) omite el chequeo, ni siquiera intenta leer el PDF")
    void validar_slotSinA4_noObserva() {
        DocumentoCargado plano = pdf(1L, "plano.pdf", "plano.pdf", false);
        when(documentoCargadoRepository.findByExpedienteIdAndActivoTrue(EXP_ID)).thenReturn(List.of(plano));
        when(storage.leerBytes("plano.pdf")).thenReturn("contenido cualquiera".getBytes());

        ValidacionResultadoDto r = service.validar(EXP_ID);

        assertTrue(r.documentos().get(0).observaciones().isEmpty());
    }

    @Test
    @DisplayName("validar: expediente sin documentos no tiene observaciones")
    void validar_sinDocumentos_sinObservaciones() {
        when(documentoCargadoRepository.findByExpedienteIdAndActivoTrue(EXP_ID)).thenReturn(List.of());

        ValidacionResultadoDto r = service.validar(EXP_ID);

        assertFalse(r.tieneObservaciones());
    }

    // ---------- Helpers ----------

    private static boolean tieneCodigo(List<ObservacionDto> obs, String codigo) {
        return obs.stream().anyMatch(o -> o.codigo().equals(codigo));
    }

    private static DocumentoCargado imagen(long id, String ruta, String nombre) {
        return doc(id, ruta, nombre, "image/png", true);
    }

    private static DocumentoCargado pdf(long id, String ruta, String nombre, boolean validaA4) {
        return doc(id, ruta, nombre, "application/pdf", validaA4);
    }

    private static DocumentoCargado doc(long id, String ruta, String nombre, String mime, boolean validaA4) {
        return DocumentoCargado.builder()
                .id(id)
                .documentoRequerido(DocumentoRequerido.builder().id(100L + id).validaA4(validaA4).build())
                .nombreOriginal(nombre)
                .rutaRelativa(ruta)
                .tipoMime(mime)
                .build();
    }
}
