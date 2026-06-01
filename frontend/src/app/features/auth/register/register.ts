import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';

import { AuthService } from '@core/services/auth.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { Regional, CondicionIva, RegisterRequest } from '@core/models';
import { passwordMatchValidator } from '@shared/validators/password-match.validator';

@Component({
  selector: 'app-register',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatCheckboxModule,
    MatDividerModule,
  ],
  templateUrl: './register.html',
  styleUrl: './register.scss',
})
export class Register implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly authService = inject(AuthService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly router = inject(Router);

  // Estado UI
  readonly cargando = signal(false);
  readonly cargandoCatalogos = signal(true);
  readonly errorGeneral = signal<string | null>(null);
  readonly mostrarPassword = signal(false);
  readonly mostrarConfirmPassword = signal(false);

  // Catálogos
  readonly regionales = signal<Regional[]>([]);
  readonly condicionesIva = signal<CondicionIva[]>([]);

  readonly form = this.fb.nonNullable.group(
    {
      // Sección 1: credenciales
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmarPassword: ['', [Validators.required]],

      // Sección 2: datos personales
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      apellido: ['', [Validators.required, Validators.maxLength(100)]],
      dni: ['', [Validators.required, Validators.pattern(/^\d{7,8}$/)]],
      cuit: ['', [Validators.required, Validators.pattern(/^\d{2}-\d{8}-\d{1}$/)]],
      domicilio: ['', [Validators.required, Validators.maxLength(255)]],
      telefono: ['', [Validators.maxLength(30)]],

      // Sección 3: datos profesionales
      matricula: ['', [Validators.required, Validators.maxLength(50)]],
      titulo: ['', [Validators.required, Validators.maxLength(150)]],
      regionalId: [null as number | null, [Validators.required]],
      condicionIvaId: [null as number | null, [Validators.required]],
      afiliadoCaja8470: [false, [Validators.required]],
    },
    {
      validators: [passwordMatchValidator('password', 'confirmarPassword')],
    }
  );

  ngOnInit(): void {
    this.cargarCatalogos();
  }

  private cargarCatalogos(): void {
    this.cargandoCatalogos.set(true);
    forkJoin({
      regionales: this.catalogoService.listarRegionales(),
      condicionesIva: this.catalogoService.listarCondicionesIva(),
    }).subscribe({
      next: ({ regionales, condicionesIva }) => {
        this.regionales.set(regionales);
        this.condicionesIva.set(condicionesIva);
        this.cargandoCatalogos.set(false);
      },
      error: () => {
        this.errorGeneral.set('No se pudieron cargar los datos del formulario. Recargá la página.');
        this.cargandoCatalogos.set(false);
      },
    });
  }

  toggleMostrarPassword(): void {
    this.mostrarPassword.update((v) => !v);
  }

  toggleMostrarConfirmPassword(): void {
    this.mostrarConfirmPassword.update((v) => !v);
  }

  submit(): void {
    if (this.form.invalid || this.cargando()) {
      this.form.markAllAsTouched();
      return;
    }

    this.errorGeneral.set(null);
    this.cargando.set(true);

    // Armamos el RegisterRequest (sin confirmarPassword, que es solo UI)
    const raw = this.form.getRawValue();
    const credenciales: RegisterRequest = {
      email: raw.email,
      password: raw.password,
      nombre: raw.nombre,
      apellido: raw.apellido,
      dni: raw.dni,
      cuit: raw.cuit,
      matricula: raw.matricula,
      titulo: raw.titulo,
      domicilio: raw.domicilio,
      telefono: raw.telefono,
      regionalId: raw.regionalId!,
      condicionIvaId: raw.condicionIvaId!,
      afiliadoCaja8470: raw.afiliadoCaja8470,
    };

    this.authService.register(credenciales).subscribe({
      next: () => {
        // El AuthService ya guardó token y signal, navegamos al dashboard
        this.router.navigate(['/dashboard']);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        this.manejarErrorBackend(err);
      },
    });
  }

  /**
   * Si el backend devuelve errores de validación por campo (400 con erroresValidacion),
   * los aplica al control correspondiente para que se muestren inline.
   * Para errores genéricos, los pone en errorGeneral.
   */
  private manejarErrorBackend(err: HttpErrorResponse): void {
    if (err.status === 0) {
      this.errorGeneral.set('No se pudo conectar al servidor. Verificá tu conexión.');
      return;
    }

    const cuerpo = err.error;

    // Si vienen errores por campo, los aplicamos al form
    if (cuerpo?.erroresValidacion && Array.isArray(cuerpo.erroresValidacion)) {
      let aplicoAlguno = false;
      for (const e of cuerpo.erroresValidacion) {
        const control = this.form.get(e.campo);
        if (control) {
          control.setErrors({ backend: e.mensaje });
          control.markAsTouched();
          aplicoAlguno = true;
        }
      }
      if (aplicoAlguno) {
        this.errorGeneral.set('Hay errores en el formulario. Revisalos abajo.');
        return;
      }
    }

    // Errores generales (ej. email/cuit/dni duplicado, 409, etc.)
    if (cuerpo?.mensaje) {
      this.errorGeneral.set(cuerpo.mensaje);
      return;
    }

    this.errorGeneral.set('Ocurrió un error inesperado. Intentá de nuevo.');
  }
} 