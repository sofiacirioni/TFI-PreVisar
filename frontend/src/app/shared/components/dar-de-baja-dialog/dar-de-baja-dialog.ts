import { Component, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { HttpErrorResponse } from '@angular/common/http';
import { ProfesionalService } from '../../../core/services/profesional.service';
import { BajaCuentaRequest } from '../../../core/models';

/**
 * Diálogo de baja de cuenta (soft-delete).
 *
 * Pide la contraseña actual como confirmación de una acción destructiva y no
 * auto-reversible. Al confirmar con éxito, cierra devolviendo `true` para que
 * el llamador cierre la sesión.
 */
@Component({
  selector: 'app-dar-de-baja-dialog',
  imports: [
    RouterLink,
    ReactiveFormsModule,
    MatDialogModule,
    MatButtonModule,
    MatFormFieldModule,
    MatIconModule,
    MatInputModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './dar-de-baja-dialog.html',
  styleUrl: './dar-de-baja-dialog.scss',
})
export class DarDeBajaDialog {
  private readonly fb = inject(FormBuilder);
  private readonly profesionalService = inject(ProfesionalService);
  private readonly dialogRef = inject(MatDialogRef<DarDeBajaDialog, boolean>);

  readonly procesando = signal(false);
  readonly errorGeneral = signal<string | null>(null);
  readonly mostrarPassword = signal(false);

  readonly form = this.fb.nonNullable.group({
    password: ['', [Validators.required]],
  });

  togglePassword(): void {
    this.mostrarPassword.update((v) => !v);
  }

  cancelar(): void {
    this.dialogRef.close(false);
  }

  confirmar(): void {
    if (this.form.invalid || this.procesando()) {
      this.form.markAllAsTouched();
      return;
    }

    this.procesando.set(true);
    this.errorGeneral.set(null);

    const request: BajaCuentaRequest = { password: this.form.getRawValue().password };

    this.profesionalService.darDeBaja(request).subscribe({
      next: () => {
        this.procesando.set(false);
        this.dialogRef.close(true);
      },
      error: (err: HttpErrorResponse) => {
        this.procesando.set(false);
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

    // Contraseña incorrecta → mostrarla en el propio campo.
    if (cuerpo?.mensaje?.toLowerCase().includes('contraseña')) {
      this.form.controls.password.setErrors({ backend: cuerpo.mensaje });
      this.form.controls.password.markAsTouched();
      return;
    }

    this.errorGeneral.set(cuerpo?.mensaje ?? 'Ocurrió un error inesperado. Intentá de nuevo.');
  }
}
