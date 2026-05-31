import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { AuthService } from '../../../core/services/auth.service';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { LoginRequest } from '@core/models';

@Component({
  selector: 'app-login',
  imports: [
    RouterLink,
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './login.html',
  styleUrl: './login.scss',
})

export class Login {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  // Estado UI
  readonly cargando = signal(false);
  readonly errorGeneral = signal<string | null>(null);
  readonly mostrarPassword = signal(false);

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  toggleMostrarPassword(): void {
    this.mostrarPassword.update((v) => !v);
  }

  submit(): void {
    if (this.form.invalid || this.cargando()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorGeneral.set(null);
    this.cargando.set(true);

    const credenciales: LoginRequest = this.form.getRawValue();

    this.authService.login(credenciales).subscribe({
      next: () => {
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.errorGeneral.set(this.mensajeDeError(err));
      },
    });
  }

  /** Traduce un HttpErrorResponse a un mensaje legible. */
  private mensajeDeError(err: HttpErrorResponse): string {
    if (err.status === 401) {
      return 'Email o contraseña incorrectos.';
    }
    if (err.status === 0) {
      return 'No se pudo conectar al servidor. Verificá tu conexión.';
    }
    if (err.error?.mensaje) {
      return err.error.mensaje;
    }
    return 'Ocurrió un error inesperado. Intentá de nuevo.';
  }
}
