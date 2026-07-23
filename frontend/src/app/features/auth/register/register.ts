import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
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
import { NgxMatSelectSearchModule } from 'ngx-mat-select-search';

import { AuthService } from '@core/services/auth.service';
import { CatalogoService } from '@core/services/catalogo.service';
import { Regional, CondicionIva, RegisterRequest, Titulo } from '@core/models';
import { passwordMatchValidator } from '@shared/validators/password-match.validator';
import { PasswordMismatchMatcher } from '@shared/validators/password-mismatch.matcher';
import { toSignal } from '@angular/core/rxjs-interop';
import { cuitDigitoVerificadorValidator, cuitPrefijoValidator, dniCuitCoherenciaValidator, dniValidator } from '../../../shared/validators/dni-cuit.validators';
import { numeroOrdenValidator } from '../../../shared/validators/numero-orden-validator';
import { TERMINOS_VERSION } from '../../legal/legal.constants';

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
    NgxMatSelectSearchModule
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

  readonly mismatchMatcher = new PasswordMismatchMatcher();

  // Catálogos
  readonly regionales = signal<Regional[]>([]);
  readonly condicionesIva = signal<CondicionIva[]>([]);
  readonly titulos = signal<Titulo[]>([]);

  readonly form = this.fb.nonNullable.group(
    {
      // Sección 1: credenciales
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      confirmarPassword: ['', [Validators.required]],

      // Sección 2: datos personales
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      apellido: ['', [Validators.required, Validators.maxLength(100)]],
      dni: ['', [Validators.required, dniValidator]],
      cuit: ['',[Validators.required, Validators.pattern(/^\d{2}-\d{8}-\d{1}$/),
        cuitDigitoVerificadorValidator,
        cuitPrefijoValidator('PROFESIONAL'),]],
      domicilio: ['', [Validators.required, Validators.maxLength(255)]],
      telefono: ['', [Validators.maxLength(30)]],

      // Sección 3: datos profesionales
      matricula: ['', [Validators.required, Validators.maxLength(50)]],
      numeroOrden: ['', [Validators.required, numeroOrdenValidator()]],
      tituloId: [null as number | null, [Validators.required]],
      tituloOtroDescripcion: ['', [Validators.maxLength(150)]],
      regionalId: [null as number | null, [Validators.required]],
      condicionIvaId: [null as number | null, [Validators.required]],
      afiliadoCaja8470: [false, [Validators.required]],

      // Aceptación de Términos y Condiciones: obligatoria para registrarse.
      aceptaTerminos: [false, [Validators.requiredTrue]],
    },
    {
      validators: [passwordMatchValidator('password', 'confirmarPassword'),
        dniCuitCoherenciaValidator('dni', 'cuit')
      ],
    }
  );

  // Signal que refleja el id de título seleccionado (reactivo)
  private readonly tituloIdSignal = toSignal(this.form.controls.tituloId.valueChanges, {
    initialValue: this.form.controls.tituloId.value,
  });

  // True si el título seleccionado tiene permite_texto_libre = true
  readonly tituloRequiereDescripcion = computed(() => {
    const id = this.tituloIdSignal();
    if (!id) return false;
    const titulo = this.titulos().find((t) => t.id === id);
    return titulo?.permiteTextoLibre ?? false;
  });

  // Control y signal del buscador del select de títulos
  readonly tituloFilterCtrl = new FormControl('', { nonNullable: true });
  private readonly tituloFilterSignal = toSignal(this.tituloFilterCtrl.valueChanges, {
    initialValue: '',
  });

  // Lista filtrada por el texto del buscador (case-insensitive)
  readonly titulosFiltrados = computed(() => {
    const filtro = this.tituloFilterSignal().toLowerCase().trim();
    const lista = this.titulos();
    if (!filtro) return lista;
    return lista.filter((t) => t.nombre.toLowerCase().includes(filtro));
  });

  ngOnInit(): void {
    this.cargarCatalogos();

    // Limpiar el campo libre cuando se cambia a un título que no lo requiere
    this.form.controls.tituloId.valueChanges.subscribe(() => {
      if (!this.tituloRequiereDescripcion()) {
        this.form.controls.tituloOtroDescripcion.setValue('');
        this.form.controls.tituloOtroDescripcion.setErrors(null);
      }
    });

    // Re-disparar validación del CUIT cuando cambia el DNI
    this.form.controls.dni.valueChanges.subscribe(() => {
      this.form.controls.cuit.updateValueAndValidity({ emitEvent: false });
    });
  }

  private cargarCatalogos(): void {
    this.cargandoCatalogos.set(true);
    forkJoin({
      regionales: this.catalogoService.listarRegionales(),
      condicionesIva: this.catalogoService.listarCondicionesIva(),
      titulos: this.catalogoService.listarTitulos(),
    }).subscribe({
      next: ({ regionales, condicionesIva, titulos }) => {
        this.regionales.set(regionales);
        this.condicionesIva.set(condicionesIva);
        this.titulos.set(titulos);
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

  // Validación condicional: si el título requiere descripción, debe venir
  const raw = this.form.getRawValue();
  if (this.tituloRequiereDescripcion() && !raw.tituloOtroDescripcion.trim()) {
    this.form.controls.tituloOtroDescripcion.setErrors({ requeridoSiOtro: true });
    this.form.controls.tituloOtroDescripcion.markAsTouched();
    return;
  }

  this.errorGeneral.set(null);
  this.cargando.set(true);

  const credenciales: RegisterRequest = {
    email: raw.email,
    password: raw.password,
    nombre: raw.nombre,
    apellido: raw.apellido,
    dni: raw.dni,
    cuit: raw.cuit,
    matricula: raw.matricula,
    numeroOrden: raw.numeroOrden,
    tituloId: raw.tituloId!,
    tituloOtroDescripcion: this.tituloRequiereDescripcion()
      ? raw.tituloOtroDescripcion.trim()
      : undefined,
    domicilio: raw.domicilio,
    telefono: raw.telefono,
    regionalId: raw.regionalId!,
    condicionIvaId: raw.condicionIvaId!,
    afiliadoCaja8470: raw.afiliadoCaja8470,
    terminosVersion: TERMINOS_VERSION,
  };

  this.authService.register(credenciales).subscribe({
    next: () => {
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