export type NivelObservacion = 'INFO' | 'ADVERTENCIA';

// Espeja ar.edu.utn.frc.previsar.enums.OrigenObservacion del backend.
export type OrigenObservacion = 'DETERMINISTICO' | 'COHERENCIA' | 'IA_VISUAL';

export interface Observacion {
  codigo: string;
  nivel: NivelObservacion;
  origen: OrigenObservacion;
  mensaje: string;
}

export interface DocumentoValidado {
  documentoCargadoId: number;
  documentoRequeridoId: number;
  nombreOriginal: string;
  observaciones: Observacion[];
}

export interface ValidacionResultado {
  documentos: DocumentoValidado[];
  generales: Observacion[];
}