export interface Provincia {
  readonly id: number;
  nombre: string;
  codigo: string;
}

export interface Regional {
  readonly id: number;
  nombre: string;
  provinciaId: number;
}

export interface CondicionIva {
  readonly id: number;
  nombre: string;
  codigoAfip: string;
}