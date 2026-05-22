package ar.edu.utn.frc.previsar.exception;

/**
 * Excepción para "recurso no encontrado". Se traduce a HTTP 404.
 */

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String mensaje) {
        super(mensaje);
    }
}
