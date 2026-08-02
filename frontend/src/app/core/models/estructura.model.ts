export interface DocumentoRequerido {
  id: number;
  codigo: string;
  nombre: string;
  obligatorio: boolean;
  orden: number;
  permiteMultiples: boolean;
  generable: boolean;
  /** Se espera A4. En false para los de gran formato (planos): la validación omite el chequeo. */
  validaA4: boolean;
  /** El documento fue retirado de la estructura vigente pero este expediente ya tiene un archivo cargado ahí. */
  desactivado: boolean;
}

/**
 * Alta/edición de un documento requerido (config del revisor).
 *
 * Los flags son opcionales: lo que no se manda, el backend lo resuelve con el
 * default del negocio en el alta y conserva el valor actual en la edición. Por eso
 * un reordenamiento (que solo manda el orden) no apaga nada.
 */
export interface DocumentoRequeridoRequest {
  codigo: string;
  nombre: string;
  obligatorio: boolean;
  orden?: number;
  permiteMultiples?: boolean;
  generable?: boolean;
  validaA4?: boolean;
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