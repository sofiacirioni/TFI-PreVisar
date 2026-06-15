export interface CalcularAportesRequest {
  tipoTareaId: number;
  honorariosReferenciales: number;
}

export interface LineaAporte {
  conceptoCodigo: string;
  conceptoNombre: string;
  grupo: 'CIEC' | 'CAJA';
  monto: number;
}

export interface AportesResponse {
  lineas: LineaAporte[];
  totalCiec: number;
  totalCaja: number;
  total: number;
}

/** Valor vigente del arancel administrativo. */
export interface ArancelVigente {
  valor: number | null;
  vigenciaDesde: string | null;
}

export interface ActualizarArancelRequest {
  valor: number;
}