export type RolUsuario = 'PROFESIONAL' | 'ADMIN';

export interface Usuario {
  readonly id: number;
  email: string;
  rol: RolUsuario;
  activo: boolean;
}

// --- Requests ---

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  // Credenciales
  email: string;
  password: string;
  // Datos del profesional
  nombre: string;
  apellido: string;
  dni: string;
  cuit: string;
  matricula: string;
  tituloId: number;
  tituloOtroDescripcion?: string;
  domicilio: string;
  telefono: string;
  regionalId: number;
  condicionIvaId: number;
  afiliadoCaja8470: boolean;
}

export interface CambiarPasswordRequest {
  passwordActual: string;
  passwordNueva: string;
}

// --- Response ---

export interface AuthResponse {
  token: string;
  tipoToken: string;
  expiraEnMs: number;
  email: string;
  rol: RolUsuario;
}