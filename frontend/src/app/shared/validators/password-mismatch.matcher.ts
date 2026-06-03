import { AbstractControl, FormGroupDirective, NgForm } from '@angular/forms';
import { ErrorStateMatcher } from '@angular/material/core';

/**
 * Hace que un input ligado a un campo de "confirmar contraseña" entre en
 * estado de error cuando el FormGroup padre tiene seteado 'passwordMismatch',
 * aun cuando el control individual sea válido. Sin esto, mat-error queda oculto.
 */
export class PasswordMismatchMatcher implements ErrorStateMatcher {
  isErrorState(
    control: AbstractControl | null,
    form: FormGroupDirective | NgForm | null
  ): boolean {
    if (!control) return false;
    const interactuado = control.touched || control.dirty || !!form?.submitted;
    const controlInvalido = control.invalid && interactuado;
    const grupoMismatch =
      !!control.parent?.errors?.['passwordMismatch'] && interactuado;
    return controlInvalido || grupoMismatch;
  }
}
