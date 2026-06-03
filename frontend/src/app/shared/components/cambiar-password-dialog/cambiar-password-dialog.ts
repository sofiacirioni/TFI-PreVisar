import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { ProfesionalService } from '../../../core/services/profesional.service';
import { CambiarPasswordRequest } from '../../../core/models';
import { HttpErrorResponse } from '@angular/common/http';
import { passwordMatchValidator } from '@shared/validators/password-match.validator';
import { PasswordMismatchMatcher } from '@shared/validators/password-mismatch.matcher';

@Component({
  selector: 'app-cambiar-password-dialog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatDividerModule,
  ],
  templateUrl: './cambiar-password-dialog.html',
  styleUrl: './cambiar-password-dialog.scss',
})
export class CambiarPasswordDialog {
  private readonly fb = inject(FormBuilder);
  private readonly profesionalService = inject(ProfesionalService);
  private readonly dialogRef = inject(MatDialogRef<CambiarPasswordDialog, boolean>);

  readonly guardando = signal(false);
  readonly errorGeneral = signal<string | null>(null);

  readonly mostrarActual = signal(false);
  readonly mostrarNueva = signal(false);
  readonly mostrarConfirmar = signal(false);

  readonly mismatchMatcher = new PasswordMismatchMatcher();

  readonly form = this.fb.nonNullable.group(
    {
      passwordActual: ['', [Validators.required]],
      passwordNueva: ['', [Validators.required, Validators.minLength(8)]],
      confirmarPassword: ['', [Validators.required]],
    },
    {
      validators: [passwordMatchValidator('passwordNueva', 'confirmarPassword')],
    }
  );

  toggleActual(): void { this.mostrarActual.update((v) => !v); }
  toggleNueva(): void { this.mostrarNueva.update((v) => !v); }
  toggleConfirmar(): void { this.mostrarConfirmar.update((v) => !v); }

  cancelar(): void {
    this.dialogRef.close(false);
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.errorGeneral.set(null);

    const raw = this.form.getRawValue();
    const request: CambiarPasswordRequest = {
      passwordActual: raw.passwordActual,
      passwordNueva: raw.passwordNueva,
    };

    this.profesionalService.cambiarPassword(request).subscribe({
      next: () => {
        this.guardando.set(false);
        this.dialogRef.close(true);
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        this.manejarErrorBackend(err);
      },
    });
  }

  private manejarErrorBackend(err: HttpErrorResponse): void {
    if (err.status === 0) {
      this.errorGeneral.set('No se pudo conectar al servidor.');
      return;
    }

    const cuerpo = err.error;

    if (cuerpo?.erroresValidacion && Array.isArray(cuerpo.erroresValidacion)) {
      for (const e of cuerpo.erroresValidacion) {
        const control = this.form.get(e.campo);
        if (control) {
          control.setErrors({ backend: e.mensaje });
          control.markAsTouched();
        }
      }
      this.errorGeneral.set('Hay errores en el formulario.');
      return;
    }

    if (cuerpo?.mensaje) {
      // Si el backend dice "contraseña actual incorrecta", lo aplicamos a ese campo
      if (cuerpo.mensaje.toLowerCase().includes('actual')) {
        this.form.controls.passwordActual.setErrors({ backend: cuerpo.mensaje });
        this.form.controls.passwordActual.markAsTouched();
        this.errorGeneral.set(null);
      } else {
        this.errorGeneral.set(cuerpo.mensaje);
      }
      return;
    }

    this.errorGeneral.set('Ocurrió un error inesperado.');
  }
}
