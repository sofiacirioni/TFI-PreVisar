import { RolUsuario } from './auth.model';

export interface Profesional {
  readonly id: number;
  // Datos del usuario asociado
  email: string;
  rol: RolUsuario;
  activo: boolean;
  // Datos personales/profesionales
  nombre: string;
  apellido: string;
  dni: string;
  cuit: string;
  matricula: string;
  //Titulo (catalogo)
  tituloId: number;
  tituloNombre: string;
  tituloOtroDescripcion?: string;
  domicilio: string;
  telefono: string;
  // Catálogos: id + nombre denormalizado para mostrar
  regionalId: number;
  regionalNombre: string;
  provinciaNombre: string;
  condicionIvaId: number;
  condicionIvaDescripcion: string;
  afiliadoCaja8470: boolean;
  // Indica si tiene rol secundario de revisor
  esRevisor: boolean;
}

export interface ProfesionalUpdateRequest {
  nombre: string;
  apellido: string;
  tituloId: number;
  tituloOtroDescripcion?: string;
  domicilio: string;
  telefono?: string;     
  regionalId: number;
  condicionIvaId: number;
  afiliadoCaja8470: boolean;
}