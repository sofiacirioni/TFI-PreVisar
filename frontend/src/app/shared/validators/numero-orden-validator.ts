import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function numeroOrdenValidator(): ValidatorFn{
  return (control: AbstractControl): ValidationErrors | null => {
    const valor = control.value;
    if (!valor) return null; // el required, si aplica, lo maneja otro validator
    return /^\d{4}$/.test(valor) ? null : { numeroOrden: true };
  };
}