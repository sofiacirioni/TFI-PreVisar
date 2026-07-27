export interface DocumentoRequerido {
  id: number;
  codigo: string;
  nombre: string;
  obligatorio: boolean;
  orden: number;
  permiteMultiples: boolean;
  generable: boolean;
  /** El documento fue retirado de la estructura vigente pero este expediente ya tiene un archivo cargado ahí. */
  desactivado: boolean;
}

/** Alta/edición de un documento requerido (config del revisor). */
export interface DocumentoRequeridoRequest {
  codigo: string;
  nombre: string;
  obligatorio: boolean;
  orden?: number;
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