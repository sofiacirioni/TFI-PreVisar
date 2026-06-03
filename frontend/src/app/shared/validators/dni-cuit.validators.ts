import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";

/**
 * Calcula el dígito verificador de un CUIT/CUIL argentino usando módulo 11.
 *
 * Recibe los primeros 10 dígitos como string (prefijo + DNI con padding).
 * Devuelve el dígito verificador (0-9) o null si los 10 dígitos no son válidos.
 *
 * Algoritmo oficial AFIP:
 *   - Pesos: [5, 4, 3, 2, 7, 6, 5, 4, 3, 2]
 *   - Suma de (dígito * peso) para cada posición
 *   - Resto = suma % 11
 *   - Si resto = 0  → verificador = 0
 *   - Si resto = 1  → caso especial: se descarta o se cambia prefijo (raro)
 *   - Si resto > 1  → verificador = 11 - resto
 */
export function calcularDigitoVerificadorCuit(primerosDiezDigitos: string): number | null {
  if (!/^\d{10}$/.test(primerosDiezDigitos)) return null;

  const pesos = [5, 4, 3, 2, 7, 6, 5, 4, 3, 2];
  let suma = 0;

  for (let i = 0; i < 10; i++) {
    suma += parseInt(primerosDiezDigitos[i], 10) * pesos[i];
  }

  const resto = suma % 11;
  if (resto === 0) return 0;
  if (resto === 1) return null;  // CUIT con resto 1 es inválido por convención AFIP
  return 11 - resto;
}

/**
 * Extrae los 11 dígitos puros de un CUIT que venga con guiones u otro formato.
 * Devuelve null si no son exactamente 11 dígitos.
 */
function extraerDigitosCuit(cuit: string): string | null {
  const soloDigitos = cuit.replace(/\D/g, '');
  return soloDigitos.length === 11 ? soloDigitos : null;
}

/**
 * Valida que el campo tenga formato de DNI argentino (7 u 8 dígitos sin puntos).
 * Si el campo está vacío, devuelve null (no se queja; otro validator se encarga de "required").
 */
export const dniValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (!value) return null;
  return /^\d{7,8}$/.test(value) ? null : { dniInvalido: true };
};

/**
 * Valida que el campo tenga formato de CUIT argentino: XX-XXXXXXXX-X.
 * No valida dígito verificador (lo hace el backend si quisiera).
 */
export const cuitValidator: ValidatorFn = (control: AbstractControl): ValidationErrors | null => {
  const value = control.value;
  if (!value) return null;
  return /^\d{2}-\d{8}-\d{1}$/.test(value) ? null : { cuitInvalido: true };
};

/**
 * Acepta DNI (7-8 dígitos) O CUIT (XX-XXXXXXXX-X) según el tipo de persona.
 * Útil para comitente, donde una persona física se identifica con DNI y una jurídica con CUIT.
 *
 * Devuelve un validator que se aplica a un control y consulta otro control del mismo form
 * (el del tipo de persona) para decidir qué patrón aplicar.
 */
export function dniOCuitSegunTipoValidator(tipoPersonaControlName: string): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) return null;

    const tipoPersona = control.parent?.get(tipoPersonaControlName)?.value;
    if (!tipoPersona) return null;

    if (tipoPersona === 'FISICA') {
      // DNI: 7-8 dígitos, o también CUIL/CUIT si quisieran usarlo (es válido en personas físicas)
      const esDni = /^\d{7,8}$/.test(value);
      const esCuit = /^\d{2}-\d{8}-\d{1}$/.test(value);
      return (esDni || esCuit) ? null : { dniInvalido: true };
    }

    if (tipoPersona === 'JURIDICA') {
      // Solo CUIT
      return /^\d{2}-\d{8}-\d{1}$/.test(value) ? null : { cuitInvalido: true };
    }

    return null;
  };
}

// ============================================
// Validación A: dígito verificador del CUIT
// ============================================

/**
 * Valida que el dígito verificador del CUIT sea matemáticamente correcto.
 * Acepta solo si el formato base (XX-XXXXXXXX-X) ya es válido.
 * No se queja si el campo está vacío (eso lo cubre Validators.required).
 */
export const cuitDigitoVerificadorValidator: ValidatorFn = (
  control: AbstractControl
): ValidationErrors | null => {
  const value = control.value;
  if (!value) return null;

  const digitos = value.replace(/\D/g, '');
  if (digitos.length !== 11) return null; // formato base inválido, otro validator se queja

  const primeros10 = digitos.substring(0, 10);
  const ultimo = parseInt(digitos[10], 10);
  const verificadorEsperado = calcularDigitoVerificadorCuit(primeros10);

  if (verificadorEsperado === null) {
    return { cuitVerificadorInvalido: true };
  }
  return ultimo === verificadorEsperado ? null : { cuitVerificadorInvalido: true };
};

// ============================================
// Validación B: prefijo del CUIT según contexto
// ============================================

export type TipoEntidadCuit = 'PROFESIONAL' | 'FISICA' | 'JURIDICA';

const PREFIJOS_FISICA = ['20', '23', '27'];
const PREFIJOS_JURIDICA = ['30', '33', '34'];

/**
 * Valida que el prefijo del CUIT sea coherente con el tipo de entidad.
 * - 'PROFESIONAL' o 'FISICA': debe ser 20, 23 o 27.
 * - 'JURIDICA': debe ser 30, 33 o 34.
 */
export function cuitPrefijoValidator(tipo: TipoEntidadCuit): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) return null;

    const digitos = value.replace(/\D/g, '');
    if (digitos.length !== 11) return null;

    const prefijo = digitos.substring(0, 2);

    if (tipo === 'JURIDICA') {
      return PREFIJOS_JURIDICA.includes(prefijo)
        ? null
        : { cuitPrefijoInvalido: { esperado: 'jurídica (30/33/34)', recibido: prefijo } };
    }

    // PROFESIONAL o FISICA
    return PREFIJOS_FISICA.includes(prefijo)
      ? null
      : { cuitPrefijoInvalido: { esperado: 'persona física (20/23/27)', recibido: prefijo } };
  };
}

// ============================================
// Validación C: coherencia DNI ↔ CUIT
// ============================================

/**
 * Validator de grupo: verifica que los 8 dígitos centrales del CUIT
 * coincidan con el DNI (con padding de ceros a la izquierda si es necesario).
 *
 * Se aplica al FormGroup completo y lee dos controles internos.
 * Setea el error en el control del CUIT para que aparezca debajo del campo correcto.
 */
export function dniCuitCoherenciaValidator(
  dniField: string,
  cuitField: string
): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const dni = group.get(dniField)?.value;
    const cuit = group.get(cuitField)?.value;

    if (!dni || !cuit) return null;

    const dniDigitos = String(dni).replace(/\D/g, '');
    const cuitDigitos = String(cuit).replace(/\D/g, '');

    if (dniDigitos.length < 7 || dniDigitos.length > 8) return null;
    if (cuitDigitos.length !== 11) return null;

    // Padding del DNI a 8 dígitos para comparar con el bloque central del CUIT
    const dniPadded = dniDigitos.padStart(8, '0');
    const centroCuit = cuitDigitos.substring(2, 10);

    if (dniPadded !== centroCuit) {
      const cuitControl = group.get(cuitField);
      const currentErrors = cuitControl?.errors ?? {};
      cuitControl?.setErrors({ ...currentErrors, cuitNoCoincideConDni: true });
      return { cuitNoCoincideConDni: true };
    }

    // Si antes había seteado este error, lo limpiamos
    const cuitControl = group.get(cuitField);
    if (cuitControl?.errors?.['cuitNoCoincideConDni']) {
      const { cuitNoCoincideConDni, ...resto } = cuitControl.errors;
      cuitControl.setErrors(Object.keys(resto).length > 0 ? resto : null);
    }

    return null;
  };
}
   
