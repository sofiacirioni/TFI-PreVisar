import { AbstractControl, ValidationErrors, ValidatorFn } from "@angular/forms";

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