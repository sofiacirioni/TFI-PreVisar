package ar.edu.utn.frc.previsar.exception;

/**
 * Falla al interactuar con la pasarela de pagos (Mercado Pago): creacion de
 * preferencia rechazada, error de red o timeout. Es un servicio externo, por lo
 * que el handler global la traduce a HTTP 502.
 */
public class PagoException extends RuntimeException {
    public PagoException(String message, Throwable cause) {
        super(message, cause);
    }
}
