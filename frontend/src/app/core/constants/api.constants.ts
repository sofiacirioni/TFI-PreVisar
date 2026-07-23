import { environment } from '@env/environment';

export const API = {
  // --- Auth (públicos) ---
  AUTH_LOGIN: `${environment.authBaseUrl}/login`,
  AUTH_REGISTER: `${environment.authBaseUrl}/register`,

  // --- Catálogos (públicos) ---
  CATALOGO_PROVINCIAS: `${environment.apiBaseUrl}/catalogos/provincias`,
  CATALOGO_REGIONALES: `${environment.apiBaseUrl}/catalogos/regionales`,
  CATALOGO_CONDICIONES_IVA: `${environment.apiBaseUrl}/catalogos/condiciones-iva`,
  CATALOGO_TITULOS: `${environment.apiBaseUrl}/catalogos/titulos`,
  CATALOGO_ESPECIALIDADES: `${environment.apiBaseUrl}/catalogos/especialidades`,
  CATALOGO_TIPOS_TAREA: `${environment.apiBaseUrl}/catalogos/tipos-tarea`, // ?especialidadId=XXX (opcional)

  // --- Profesional ---
  PROFESIONAL_ME: `${environment.apiBaseUrl}/profesional/me`,
  PROFESIONAL_CAMBIAR_PASSWORD: `${environment.apiBaseUrl}/profesional/me/cambiar-password`,
  PROFESIONAL_BAJA: `${environment.apiBaseUrl}/profesional/me/baja`,

  // --- Comitentes ---
  COMITENTES: `${environment.apiBaseUrl}/comitentes`,
  COMITENTE_BY_ID: (id: number) => `${environment.apiBaseUrl}/comitentes/${id}`,
  COMITENTE_BUSCAR: `${environment.apiBaseUrl}/comitentes/buscar`, // ?dniCuit=XXX

  // --- Obras anidadas bajo comitente ---
  COMITENTE_OBRAS: (comitenteId: number) =>
    `${environment.apiBaseUrl}/comitentes/${comitenteId}/obras`,

  // --- Obras standalone ---
  OBRAS: `${environment.apiBaseUrl}/obras`,
  OBRA_BY_ID: (id: number) => `${environment.apiBaseUrl}/obras/${id}`,

  // --- Expedientes ---
  EXPEDIENTES: `${environment.apiBaseUrl}/expedientes`,
  EXPEDIENTE_BY_ID: (id: number) => `${environment.apiBaseUrl}/expedientes/${id}`,
  EXPEDIENTE_COMPLETAR: (id: number) => `${environment.apiBaseUrl}/expedientes/${id}/completar`,
  EXPEDIENTES_CALCULAR_APORTES: `${environment.apiBaseUrl}/expedientes/calcular-aportes`,

  EXPEDIENTE_DOCUMENTOS: (id: number) => `${environment.apiBaseUrl}/expedientes/${id}/documentos`,
  EXPEDIENTE_DOCUMENTO: (id: number, docId: number) =>
    `${environment.apiBaseUrl}/expedientes/${id}/documentos/${docId}`,
  EXPEDIENTE_CONTRATO: (id: number) => `${environment.apiBaseUrl}/expedientes/${id}/contrato`,
  EXPEDIENTE_CARATULA: (id: number) => `${environment.apiBaseUrl}/expedientes/${id}/caratula`,
  // Inicia el pago del arancel: crea la preferencia de MP y devuelve el initPoint.
  EXPEDIENTE_PAGO: (id: number) => `${environment.apiBaseUrl}/expedientes/${id}/pago`,
  EXPEDIENTE_PAGO_SYNC: (id: number) => `${environment.apiBaseUrl}/expedientes/${id}/pago/sync`,
  // Validación nivel 1 (on-demand). Vive en DocumentoController: GET /documentos/validacion.
  EXPEDIENTE_VALIDACION: (id: number) =>
    `${environment.apiBaseUrl}/expedientes/${id}/documentos/validacion`,
  // El análisis IA es por ranura/slot (validacion/ia?documentoRequeridoId=X).
  EXPEDIENTE_IA: (id: number, docReqId: number) =>
    `${environment.apiBaseUrl}/expedientes/${id}/documentos/validacion/ia?documentoRequeridoId=${docReqId}`,
  EXPEDIENTE_IA_ESTADO: (id: number, docReqId: number) =>
    `${environment.apiBaseUrl}/expedientes/${id}/documentos/validacion/ia/estado?documentoRequeridoId=${docReqId}`,
  EXPEDIENTE_COMPILADO: (id: number) =>
    `${environment.apiBaseUrl}/expedientes/${id}/documentos/compilado`,

  // --- Parámetros de aporte (gestión, solo revisor) ---
  APORTE_ARANCEL: `${environment.apiBaseUrl}/aportes/arancel`,

  // --- Estructura de expediente (seccion, documentos requeridos) ---
  ESTRUCTURA: `${environment.apiBaseUrl}/estructura`,
} as const;
