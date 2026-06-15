package ar.edu.utn.frc.previsar.exception;

/**
 * Excepción para accesos denegados por reglas de negocio
 * (ej: el usuario está autenticado pero no tiene permiso para la acción).
 * Se traduce a HTTP 403 en el GlobalExceptionHandler.
 */

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String mensaje) {
        super(mensaje);
    }
}
