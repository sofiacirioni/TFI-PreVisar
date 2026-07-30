// Campos editables del contrato de locación que el profesional completa en el armado.
// Se persisten en el expediente porque el PDF los lee desde ahí: la previsualización y
// el contrato que entra al compilado los genera el backend, que no ve el estado del
// navegador. Todos opcionales: el PDF deja una línea de puntos donde falta un dato.
export interface DatosContrato {
  honorariosPactados?: number | null;
  documentacionConfeccion?: string | null;
  tareasEspeciales?: string | null;
  formaPago?: string | null;
  plazoEntrega?: string | null;
  gastosEspeciales?: string | null;
}
