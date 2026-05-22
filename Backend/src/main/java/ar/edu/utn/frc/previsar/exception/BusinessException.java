package ar.edu.utn.frc.previsar.exception;

/**
 * Excepción para errores de reglas de negocio (ej: email ya existe).
 * Se traduce a HTTP 400 en el GlobalExceptionHandler.
 */

public class BusinessException extends RuntimeException {
    public BusinessException(String mensaje) {
        super(mensaje);
    }
}
