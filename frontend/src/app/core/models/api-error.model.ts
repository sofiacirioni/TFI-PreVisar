/**
 * Estructura del error que devuelve GlobalExceptionHandler del backend.
 */
export interface ApiError {
  timestamp: string;
  status: number;
  error: string;          // ej. "Bad Request"
  mensaje: string;        // mensaje legible (nota: "mensaje", no "message")
  path: string;
  erroresValidacion?: ErrorCampo[];
}

export interface ErrorCampo {
  campo: string;
  mensaje: string;
}