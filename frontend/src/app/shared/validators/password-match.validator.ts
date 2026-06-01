import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

/**
 * Validator de grupo: verifica que dos campos del FormGroup tengan el mismo valor.
 * Se aplica al FormGroup completo, no a un control individual.
 *
 * Uso: this.fb.group({...}, { validators: passwordMatchValidator('password', 'confirmarPassword') })
 *
 * El error se setea en el FormGroup con la key 'passwordMismatch'.
 */
export function passwordMatchValidator(
  passwordField: string,
  confirmField: string
): ValidatorFn {
  return (group: AbstractControl): ValidationErrors | null => {
    const password = group.get(passwordField)?.value;
    const confirm = group.get(confirmField)?.value;

    if (!password || !confirm) {
      return null; // Si alguno está vacío, otro validator se encarga
    }

    return password === confirm ? null : { passwordMismatch: true };
  };
}