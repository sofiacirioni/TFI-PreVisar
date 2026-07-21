import { EstadoArancel } from './expediente.model';

// Espeja ar.edu.utn.frc.previsar.dtos.PreferenciaPagoDto del backend.
export interface PreferenciaPago {
  preferenceId: string;
  initPoint: string;
}

// Estado de la ruta de retorno (deriva de las back_urls configuradas en MP).
export type EstadoPagoRetorno = 'exito' | 'pendiente' | 'error';

// Respuesta de la reconciliación contra MP (espeja EstadoArancelDto).
export interface EstadoArancelResponse {
  estadoArancel: EstadoArancel;
}
