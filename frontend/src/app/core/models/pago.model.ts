// Espeja ar.edu.utn.frc.previsar.dtos.PreferenciaPagoDto del backend.
export interface PreferenciaPago {
  preferenceId: string;
  initPoint: string;
}

// Estado de la ruta de retorno (deriva de las back_urls configuradas en MP).
export type EstadoPagoRetorno = 'exito' | 'pendiente' | 'error';
