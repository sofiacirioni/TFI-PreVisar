import { environment } from '@env/environment';

export const API = {
  // --- Auth (públicos) ---
  AUTH_LOGIN: `${environment.authBaseUrl}/login`,
  AUTH_REGISTER: `${environment.authBaseUrl}/register`,

  // --- Catálogos (públicos) ---
  CATALOGO_PROVINCIAS: `${environment.apiBaseUrl}/catalogos/provincias`,
  CATALOGO_REGIONALES: `${environment.apiBaseUrl}/catalogos/regionales`,
  CATALOGO_CONDICIONES_IVA: `${environment.apiBaseUrl}/catalogos/condiciones-iva`,

  // --- Profesional ---
  PROFESIONAL_ME: `${environment.apiBaseUrl}/profesional/me`,

  // --- Comitentes ---
  COMITENTES: `${environment.apiBaseUrl}/comitentes`,
  COMITENTE_BY_ID: (id: number) => `${environment.apiBaseUrl}/comitentes/${id}`,
  COMITENTE_BUSCAR: `${environment.apiBaseUrl}/comitentes/buscar`,  // ?dniCuit=XXX

  // --- Obras anidadas bajo comitente ---
  COMITENTE_OBRAS: (comitenteId: number) =>
    `${environment.apiBaseUrl}/comitentes/${comitenteId}/obras`,

  // --- Obras standalone ---
  OBRAS: `${environment.apiBaseUrl}/obras`,
  OBRA_BY_ID: (id: number) => `${environment.apiBaseUrl}/obras/${id}`,
} as const;