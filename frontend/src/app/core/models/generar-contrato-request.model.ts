// Campos editables del contrato que el profesional completa desde el panel
// lateral. Todos opcionales: el backend cae a puntos suspensivos si vienen vacíos.
export interface GenerarContratoRequest {
  honorariosPactados?: number | null;
  documentacionConfeccion?: string | null;
  tareasEspeciales?: string | null;
  formaPago?: string | null;
  plazoEntrega?: string | null;
  gastosEspeciales?: string | null;
}
