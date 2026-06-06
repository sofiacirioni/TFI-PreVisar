export type EstadoExpediente = 'BORRADOR' | 'COMPLETO';

// Lo que MANDAMOS al backend. Todo opcional = guardado parcial.
export interface ExpedienteRequest {
  obraId?: number | null;
  tipoTareaId?: number | null;
  honorariosReferenciales?: number | null;
}

// Lo que DEVUELVE el backend.
export interface ExpedienteResponse {
  id: number;
  estado: EstadoExpediente;
  obraId: number | null;
  tipoTareaId: number | null;
  tipoTareaCodigo: string | null;
  tipoTareaNombre: string | null;
  honorariosReferenciales: number | null;
  aporteRod: number | null;
  aporteArancelAdmin: number | null;
  aporteCajaProfesional: number | null;
  aporteCajaComitente: number | null;
  // Totales derivados que expone el getter del back (Jackson los serializa)
  totalCiec: number | null;
  totalCaja: number | null;
  total: number | null;
  createdAt: string;
  updatedAt: string;
}