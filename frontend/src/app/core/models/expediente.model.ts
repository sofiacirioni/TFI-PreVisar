export type EstadoExpediente = 'BORRADOR' | 'EN_PROCESO';

// Estado del arancel derivado de los pagos (espeja ar.edu.utn.frc.previsar.enums.EstadoArancel).
export type EstadoArancel = 'NINGUNO' | 'PENDIENTE' | 'APROBADO';

// Lo que MANDAMOS al backend. Todo opcional = guardado parcial.
export interface ExpedienteRequest {
  obraId?: number | null;
  nombre?: string | null;
  tipoTareaId?: number | null;
  honorariosReferenciales?: number | null;
}

// Lo que DEVUELVE el backend.
export interface ExpedienteResponse {
  id: number;
  nombre?: string | null;
  estado: EstadoExpediente;
  obraId: number | null;
  obraDesignacion: string | null;
  provinciaId: number | null;
  provinciaNombre: string | null;
  comitenteId: number | null;
  comitenteNombre: string | null;
  comitenteEmail: string | null;
  tipoTareaId: number | null;
  tipoTareaCodigo: string | null;
  tipoTareaNombre: string | null;
  especialidadId: number | null;
  especialidadNombre: string | null;
  honorariosReferenciales: number | null;
  estadoArancel: EstadoArancel;
  createdAt: string;
  updatedAt: string;
}

/**
 * Ruta donde se retoma un expediente. Un EN_PROCESO ya pasó la carga de datos
 * iniciales, así que el profesional debe volver al armado y no al wizard.
 * Vive acá para que la regla no se repita en cada pantalla que abre un expediente
 * (lista, dashboard, …) y no vuelva a quedar desalineada entre ellas.
 */
export function rutaRetomarExpediente(exp: Pick<ExpedienteResponse, 'id' | 'estado'>): unknown[] {
  return exp.estado === 'EN_PROCESO'
    ? ['/expedientes', exp.id, 'armado']
    : ['/expedientes', exp.id];
}