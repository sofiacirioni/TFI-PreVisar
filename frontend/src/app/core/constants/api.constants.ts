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
  PROFESIONAL_SOLICITAR_REVISOR: `${environment.apiBaseUrl}/profesional/me/solicitar-rol-revisor`,

  // --- Comitentes ---
  COMITENTES: `${environment.apiBaseUrl}/comitentes`,
  COMITENTE_BY_ID: (id: number) => `${environment.apiBaseUrl}/comitentes/${id}`,

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
  // Campos editables del contrato: se guardan en el expediente y el PDF los toma de ahí.
  EXPEDIENTE_CONTRATO_DATOS: (id: number) =>
    `${environment.apiBaseUrl}/expedientes/${id}/contrato/datos`,
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

  // --- Revisor: análisis de expedientes completos (PDF) ---
  COMITENTE_BUSCAR_INCREMENTAL: `${environment.apiBaseUrl}/comitentes/buscar-incremental`,
  REVISOR_REVISIONES: `${environment.apiBaseUrl}/revisor/revisiones`,
  REVISOR_METRICAS: `${environment.apiBaseUrl}/revisor/revisiones/metricas`,
  REVISOR_REVISION_BY_ID: (id: number) =>
    `${environment.apiBaseUrl}/revisor/revisiones/${id}`,

  // --- Parámetros de aporte (gestión, solo revisor) ---
  APORTE_ARANCEL: `${environment.apiBaseUrl}/aportes/arancel`,

  // --- Estructura de expediente (seccion, documentos requeridos) ---
  ESTRUCTURA: `${environment.apiBaseUrl}/estructura`,
  // Estructura scopeada a un expediente propio (incluye ranuras desactivadas con archivo).
  ESTRUCTURA_EXPEDIENTE: (expedienteId: number) =>
    `${environment.apiBaseUrl}/estructura/expediente/${expedienteId}`,
  // Estructura de la provincia del revisor (para la pantalla de configuración). ?tipoTareaId=X
  ESTRUCTURA_MIA: `${environment.apiBaseUrl}/estructura/mia`,
  // CRUD de documentos requeridos (solo revisor de su provincia).
  ESTRUCTURA_SECCION_DOCUMENTOS: (seccionId: number) =>
    `${environment.apiBaseUrl}/estructura/secciones/${seccionId}/documentos`,
  ESTRUCTURA_DOCUMENTO: (documentoId: number) =>
    `${environment.apiBaseUrl}/estructura/documentos/${documentoId}`,
} as const;
