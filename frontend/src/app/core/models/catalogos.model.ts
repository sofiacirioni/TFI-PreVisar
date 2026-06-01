export interface Provincia {
  readonly id: number;
  nombre: string;
  codigo: string;
}

export interface Regional {
  readonly id: number;
  nombre: string;
  provinciaId: number;
  provinciaNombre?: string;
}

export interface CondicionIva {
  readonly id: number;
  descripcion: string;
  codigo: string;
}

export interface Titulo {
  readonly id: number;
  nombre: string;
  permiteTextoLibre: boolean;
}