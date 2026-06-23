export interface DocumentoCargado {
  id: number;
  documentoRequeridoId: number;
  nombreOriginal: string;
  tipoMime: string;
  tamanoBytes: number;
  createdAt: string;
}