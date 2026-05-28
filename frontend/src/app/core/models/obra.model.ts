export interface Obra {
  readonly id: number;
  comitenteId: number;
  comitenteNombre: string;
  comitenteDniCuit: string;
  designacion: string;
  calle: string;
  numero: string;
  barrio: string;
  localidad: string;
  provinciaId: number;
  provinciaNombre: string;
  codigoPostal: string;
  // Datos catastrales (todos opcionales)
  circunscripcion?: string;
  seccion?: string;
  manzana?: string;
  parcela?: string;
  // Consolidada: XX-XX-XXX-XXX, null si faltan partes
  nomenclaturaCatastral?: string;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ObraRequest {
  designacion: string;
  calle: string;
  numero: string;
  barrio?: string;
  localidad: string;
  provinciaId: number;
  codigoPostal: string;
  circunscripcion?: string;
  seccion?: string;
  manzana?: string;
  parcela?: string;
}