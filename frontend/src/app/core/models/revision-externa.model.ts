/** Estado del análisis de un expediente subido por el revisor. */
export type EstadoRevision = 'EN_PROGRESO' | 'COMPLETADO' | 'ERROR';

/** Fila del historial de revisiones. */
export interface RevisionResumen {
  readonly id: number;
  nombreArchivo: string;
  estado: EstadoRevision;
  createdAt: string; // ISO (Instant serializado)
}

/** Un documento identificado por la IA dentro del expediente. */
export interface DocumentoIdentificado {
  tipo: string;
  paginas: string;
  observacion?: string;
}

/** Un problema de calidad detectado por la IA en una página. */
export interface ProblemaDetectado {
  pagina: number;
  tipo: string; // ILEGIBLE | CORTADO | PIXELADO | TORCIDO | TAPADO | ROTULO | FIRMA_SELLO
  detalle: string;
}

/** Resumen crudo que devuelve la IA (forma del JSON del prompt). */
export interface ResumenRevision {
  totalPaginas?: number;
  tipoExpediente?: string;
  profesional?: string;
  comitente?: string;
  documentos?: DocumentoIdentificado[];
  problemas?: ProblemaDetectado[];
}

/** Un par etiqueta/cantidad ya agregado por el backend. */
export interface ConteoMetrica {
  etiqueta: string;
  cantidad: number;
}

/**
 * Métricas del historial del revisor. Los conteos por estado se podrían sacar
 * del listado, pero `problemasPorTipo` no: vive dentro del `resultado` de cada
 * revisión, que el listado no devuelve.
 */
export interface RevisionMetricas {
  totalAnalizados: number;
  completados: number;
  conError: number;
  enProgreso: number;
  problemasPorTipo: ConteoMetrica[];
  /** Null si todavía no hay ningún análisis completado. */
  promedioProblemas: number | null;
}

/** Detalle de una revisión. `resultado` es null mientras EN_PROGRESO o si hubo ERROR. */
export interface RevisionDetalle {
  readonly id: number;
  nombreArchivo: string;
  estado: EstadoRevision;
  detalle?: string | null; // motivo cuando estado = ERROR
  resultado?: ResumenRevision | null;
  createdAt: string;
}
