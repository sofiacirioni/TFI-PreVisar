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

export interface Especialidad {
  readonly id: number;
  codigo: string;
  nombre: string;
}

export interface TipoTarea {
  id: number;
  codigo: string;
  nombre: string;
  especialidadId: number | null;
  especialidadNombre: string | null;
}
