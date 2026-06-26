export type NivelObservacion = 'INFO' | 'ADVERTENCIA';

export interface Observacion { codigo: string; nivel: NivelObservacion; mensaje: string; }

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