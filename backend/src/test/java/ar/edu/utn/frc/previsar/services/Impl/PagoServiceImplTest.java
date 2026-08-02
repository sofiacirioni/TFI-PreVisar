package ar.edu.utn.frc.previsar.services.Impl;

import ar.edu.utn.frc.previsar.config.MercadoPagoProperties;
import ar.edu.utn.frc.previsar.entities.Expediente;
import ar.edu.utn.frc.previsar.entities.Pago;
import ar.edu.utn.frc.previsar.entities.Profesional;
import ar.edu.utn.frc.previsar.entities.Usuario;
import ar.edu.utn.frc.previsar.enums.EstadoPago;
import ar.edu.utn.frc.previsar.repositories.ExpedienteRepository;
import ar.edu.utn.frc.previsar.repositories.PagoRepository;
import ar.edu.utn.frc.previsar.services.EmailService;
import ar.edu.utn.frc.previsar.services.ExpedienteService;
import com.mercadopago.resources.payment.Payment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests de la acreditación de pagos de Mercado Pago.
 *
 * Foco principal: un pago de la misma cuenta de MP originado FUERA de PreVisar no
 * trae external_reference. Antes eso era Long.valueOf(null) -> NPE -> 500, y como
 * MP reintenta ante 5xx, la misma notificación quedaba rebotando para siempre.
 *
 * Los métodos que se ejercitan son package-private, siguiendo la misma convención
 * que ValidacionNivel1Service y ValidacionNivel2Service: la ruta pública crea un
 * PaymentClient contra la API real y no se puede cubrir con un test unitario.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoServiceImplTest {

    @Mock private PagoRepository pagoRepository;
    @Mock private ExpedienteRepository expedienteRepository;
    @Mock private ExpedienteService expedienteService;
    @Mock private EmailService emailService;

    private PagoServiceImpl pagoService;

    @BeforeEach
    void setUp() {
        MercadoPagoProperties props = new MercadoPagoProperties(
                "TEST-token", "http://localhost:4200", "https://tunel.test", null);
        pagoService = new PagoServiceImpl(pagoRepository, expedienteRepository, expedienteService,
                props, emailService);
    }

    /** Pago tal como lo devuelve la API de MP. */
    private Payment pagoMp(String externalReference, String status) {
        Payment mpPago = mock(Payment.class);
        when(mpPago.getExternalReference()).thenReturn(externalReference);
        when(mpPago.getStatus()).thenReturn(status);
        when(mpPago.getTransactionAmount()).thenReturn(new BigDecimal("15000.00"));
        return mpPago;
    }

    // -------------------- Pagos que no son de PreVisar --------------------

    @Test
    @DisplayName("Pago sin external_reference: se ignora sin explotar ni persistir nada")
    void pagoSinExternalReferenceSeIgnora() {
        assertDoesNotThrow(() -> pagoService.acreditar("999", pagoMp(null, "approved")));

        verify(pagoRepository, never()).save(any(Pago.class));
        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("external_reference no numérico: mismo trato, se descarta")
    void externalReferenceNoNumericoSeIgnora() {
        assertDoesNotThrow(() -> pagoService.acreditar("999", pagoMp("carrito-web-42", "approved")));

        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    @DisplayName("external_reference en blanco: se descarta")
    void externalReferenceEnBlancoSeIgnora() {
        assertDoesNotThrow(() -> pagoService.acreditar("999", pagoMp("   ", "approved")));

        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    @DisplayName("expedienteDe devuelve null en vez de lanzar cuando la referencia no sirve")
    void expedienteDeEsNullSafe() {
        assertNull(pagoService.expedienteDe(pagoMp(null, "approved")));
        assertNull(pagoService.expedienteDe(pagoMp("", "approved")));
        assertNull(pagoService.expedienteDe(pagoMp("abc", "approved")));
        assertEquals(42L, pagoService.expedienteDe(pagoMp(" 42 ", "approved")));
    }

    // -------------------- Acreditación normal --------------------

    @Test
    @DisplayName("Pago nuevo aprobado: se persiste contra su expediente y se notifica al profesional")
    void pagoNuevoAprobadoSeAcreditaYNotifica() {
        Usuario usuario = Usuario.builder().id(7L).email("profesional@example.com").build();
        Profesional profesional = Profesional.builder().id(70L).usuario(usuario).build();
        Expediente expediente = new Expediente();
        expediente.setId(42L);
        expediente.setProfesional(profesional);

        when(expedienteRepository.getReferenceById(42L)).thenReturn(expediente);
        when(pagoRepository.findByMpPaymentId("999")).thenReturn(Optional.empty());
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        pagoService.acreditar("999", pagoMp("42", "approved"));

        ArgumentCaptor<Pago> captor = ArgumentCaptor.forClass(Pago.class);
        verify(pagoRepository).save(captor.capture());
        Pago guardado = captor.getValue();
        assertEquals("999", guardado.getMpPaymentId());
        assertEquals(EstadoPago.APROBADO, guardado.getEstado());
        assertEquals(42L, guardado.getExpediente().getId());

        verify(emailService).notificarPagoAcreditado(42L, "profesional@example.com",
                new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("Reprocesar un pago ya aprobado no vuelve a notificar (idempotencia)")
    void reprocesarPagoAprobadoNoRenotifica() {
        Pago yaAprobado = Pago.builder()
                .id(5L).mpPaymentId("999").estado(EstadoPago.APROBADO)
                .monto(new BigDecimal("15000.00"))
                .build();
        when(pagoRepository.findByMpPaymentId("999")).thenReturn(Optional.of(yaAprobado));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        pagoService.acreditar("999", pagoMp("42", "approved"));

        verifyNoInteractions(emailService);
    }

    @Test
    @DisplayName("Un pago pendiente que pasa a aprobado sí notifica")
    void pagoQuePasaDePendienteAAprobadoNotifica() {
        Usuario usuario = Usuario.builder().id(7L).email("profesional@example.com").build();
        Profesional profesional = Profesional.builder().id(70L).usuario(usuario).build();
        Expediente expediente = new Expediente();
        expediente.setId(42L);
        expediente.setProfesional(profesional);

        Pago pendiente = Pago.builder()
                .id(5L).mpPaymentId("999").estado(EstadoPago.PENDIENTE)
                .expediente(expediente).monto(new BigDecimal("15000.00"))
                .build();
        when(pagoRepository.findByMpPaymentId("999")).thenReturn(Optional.of(pendiente));
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        pagoService.acreditar("999", pagoMp("42", "approved"));

        assertEquals(EstadoPago.APROBADO, pendiente.getEstado());
        verify(emailService).notificarPagoAcreditado(42L, "profesional@example.com",
                new BigDecimal("15000.00"));
    }

    @Test
    @DisplayName("El mapeo de estados de MP cubre aprobado, en curso y rechazado")
    void mapeoDeEstados() {
        assertEquals(EstadoPago.APROBADO, pagoService.mapearEstado("approved"));
        assertEquals(EstadoPago.PENDIENTE, pagoService.mapearEstado("pending"));
        assertEquals(EstadoPago.PENDIENTE, pagoService.mapearEstado("in_process"));
        assertEquals(EstadoPago.PENDIENTE, pagoService.mapearEstado("authorized"));
        assertEquals(EstadoPago.RECHAZADO, pagoService.mapearEstado("rejected"));
        assertEquals(EstadoPago.RECHAZADO, pagoService.mapearEstado("cancelled"));
        assertEquals(EstadoPago.RECHAZADO, pagoService.mapearEstado("refunded"));
    }

    // -------------------- Webhook --------------------

    @Test
    @DisplayName("El webhook responde 200 aunque el evento no sea de tipo payment")
    void webhookIgnoraEventosQueNoSonPayment() {
        var respuesta = pagoService.procesarNotificacion("merchant_order", "123", null, null, null);
        assertEquals(200, respuesta.getStatusCode().value());
    }

    @Test
    @DisplayName("El webhook responde 200 ante un data.id no numérico (reintentar no lo arregla)")
    void webhookIgnoraDataIdNoNumerico() {
        var respuesta = pagoService.procesarNotificacion("payment", "no-es-un-id", null, null, null);
        assertEquals(200, respuesta.getStatusCode().value());
    }
}
