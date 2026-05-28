export type TipoPersona = 'FISICA' | 'JURIDICA';

export interface Comitente {
  readonly id: number;
  tipoPersona: TipoPersona;
  nombreRazonSocial: string;
  dniCuit: string;
  domicilio: string;
  email: string;
  telefono: string;
  readonly createdAt: string;
  readonly updatedAt: string;
}

export interface ComitenteRequest {
  tipoPersona: TipoPersona;
  nombreRazonSocial: string;
  dniCuit: string;
  domicilio: string;
  email: string;
  telefono?: string;
}