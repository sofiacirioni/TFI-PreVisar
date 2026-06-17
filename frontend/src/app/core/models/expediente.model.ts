export type EstadoExpediente = 'BORRADOR' | 'COMPLETO';

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
  tipoTareaId: number | null;
  tipoTareaCodigo: string | null;
  tipoTareaNombre: string | null;
  especialidadId: number | null;
  especialidadNombre: string | null;
  honorariosReferenciales: number | null;
  createdAt: string;
  updatedAt: string;
}