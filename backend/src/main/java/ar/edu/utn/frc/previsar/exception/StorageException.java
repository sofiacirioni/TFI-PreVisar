package ar.edu.utn.frc.previsar.exception;

/**
 * Falla al guardar, leer o eliminar un archivo en el almacenamiento.
 * La maneja el GlobalExceptionHandler como HTTP 500.
 */
public class StorageException extends RuntimeException {
    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
