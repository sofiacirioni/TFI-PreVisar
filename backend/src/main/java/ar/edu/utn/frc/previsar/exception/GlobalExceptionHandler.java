package ar.edu.utn.frc.previsar.exception;

import ar.edu.utn.frc.previsar.dtos.response.ErrorResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.naming.AuthenticationException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Maneja excepciones globalmente y devuelve respuestas HTTP consistentes.
 * Cada @ExceptionHandler captura un tipo de excepción y la traduce.
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * Errores de validación de @Valid en DTOs.
     * Devuelve HTTP 400 con lista de campos fallados.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDto> manejarValidacion(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        List<ErrorResponseDto.ErrorCampo> errores = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::mapearFieldError)
                .toList();
        ErrorResponseDto body = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .error(HttpStatus.BAD_REQUEST.getReasonPhrase())
                .mensaje("Errores de validación en los datos enviados")
                .path(request.getRequestURI())
                .erroresValidacion(errores)
                .build();
        return ResponseEntity.badRequest().body(body);
    }

    /**
     * Reglas de negocio violadas (ej: email duplicado).
     * Devuelve HTTP 400.
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDto> manejarBusiness(
            BusinessException ex,
            HttpServletRequest request) {
        return construirRespuesta(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    /**
     * Acceso denegado por reglas de negocio (usuario autenticado sin permiso).
     * Devuelve HTTP 403.
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponseDto> manejarForbidden(
            ForbiddenException ex,
            HttpServletRequest request) {
        return construirRespuesta(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    /**
     * Recurso no encontrado. Devuelve HTTP 404.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponseDto> manejarNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request) {
        return construirRespuesta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    /**
     * Credenciales incorrectas en el login. Devuelve HTTP 401.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponseDto> manejarBadCredentials(
            BadCredentialsException ex,
            HttpServletRequest request) {
        return construirRespuesta(
                HttpStatus.UNAUTHORIZED,
                "Email o password incorrectos",
                request);
    }

    /**
     * Otras fallas de autenticación. Devuelve HTTP 401.
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponseDto> manejarAuth(
            AuthenticationException ex,
            HttpServletRequest request) {
        return construirRespuesta(
                HttpStatus.UNAUTHORIZED,
                "No autorizado: " + ex.getMessage(),
                request);
    }

    /**
     * Catch-all para excepciones no esperadas. Devuelve HTTP 500.
     * IMPORTANTE: registra el stacktrace porque indica un bug.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDto> manejarGenericas(
            Exception ex,
            HttpServletRequest request) {
        log.error("Error inesperado", ex);
        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Error interno del servidor",
                request);
    }

    /**
     * Excepciones relacionadas con la generacion de documentos pdf
     *
     */
    @ExceptionHandler(PdfGenerationException.class)
    public ResponseEntity<ErrorResponseDto> handlePdfGeneration(
            PdfGenerationException ex,
            HttpServletRequest request) {
        log.error("Error generando PDF", ex);
        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo generar el documento PDF",
                request);
    }

    /**
     * Fallas de almacenamiento de archivos (guardar/leer/eliminar). Devuelve HTTP 500.
     */
    @ExceptionHandler(StorageException.class)
    public ResponseEntity<ErrorResponseDto> handleStorage(
            StorageException ex,
            HttpServletRequest request) {
        log.error("Error de almacenamiento de archivos", ex);
        return construirRespuesta(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "No se pudo procesar el archivo",
                request);
    }

    /**
     * Fallas al consultar la API de Gemini o al rasterizar el PDF para la
     * validacion visual (nivel 3). Es un servicio externo: HTTP 502.
     */
    @ExceptionHandler(GeminiException.class)
    public ResponseEntity<ErrorResponseDto> handleGemini(
            GeminiException ex,
            HttpServletRequest request) {
        log.error("Error en la validacion visual (Gemini)", ex);
        return construirRespuesta(
                HttpStatus.BAD_GATEWAY,
                "No se pudo completar la validación visual del expediente",
                request);
    }

    // -------------------------- Helpers --------------------------

    private ResponseEntity<ErrorResponseDto> construirRespuesta(
            HttpStatus status, String mensaje, HttpServletRequest request) {

        ErrorResponseDto body = ErrorResponseDto.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(status.getReasonPhrase())
                .mensaje(mensaje)
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.status(status).body(body);
    }

    private ErrorResponseDto.ErrorCampo mapearFieldError(FieldError fe) {
        return ErrorResponseDto.ErrorCampo.builder()
                .campo(fe.getField())
                .mensaje(fe.getDefaultMessage())
                .build();
    }
}
