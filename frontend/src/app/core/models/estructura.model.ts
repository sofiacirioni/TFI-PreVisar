export interface DocumentoRequerido {
  id: number;
  codigo: string;
  nombre: string;
  obligatorio: boolean;
  orden: number;
  permiteMultiples: boolean;
}

export interface SeccionEstructura {
  id: number;
  codigo: string;
  nombre: string;
  orden: number;
  documentos: DocumentoRequerido[];
}

export interface EstructuraExpediente {
  tipoTareaId: number;
  tipoTareaCodigo: string;
  secciones: SeccionEstructura[];
}