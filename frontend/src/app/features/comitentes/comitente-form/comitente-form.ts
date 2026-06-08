import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatRadioModule } from '@angular/material/radio';
import { MatDividerModule } from '@angular/material/divider';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ComitenteService } from '../../../core/services/comitente.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ComitenteRequest, TipoPersona } from '../../../core/models';
import { cuitDigitoVerificadorValidator, cuitPrefijoValidator, dniOCuitSegunTipoValidator } from '../../../shared/validators/dni-cuit.validators';
import { HttpErrorResponse } from '@angular/common/http';
import { Comitente } from '../../../core/models/comitente.model';

@Component({
  selector: 'app-comitente-form',
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
    MatRadioModule,
    MatDividerModule,
    MatTooltipModule
  ],
  templateUrl: './comitente-form.html',
  styleUrl: './comitente-form.scss',
})
export class ComitenteForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly comitenteService = inject(ComitenteService);
  private readonly snackBar = inject(MatSnackBar);

  // Estado UI
  readonly cargando = signal(false);      // cargando datos en modo edición
  readonly guardando = signal(false);     // submit en curso
  readonly errorCarga = signal<string | null>(null);
  readonly errorGeneral = signal<string | null>(null);

  // Modo: alta o edición
  private readonly comitenteId = signal<number | null>(null);
  readonly modoEdicion = computed(() => this.comitenteId() !== null);

  //
  readonly comitenteEstado = signal<'inicial' | 'buscando' | 'encontrado' | 'nuevo'>('inicial');
  readonly comitenteSeleccionado = signal<Comitente | null>(null);

  // Validator de prefijo que respeta el tipoPersona del propio formulario
  private validarPrefijoSegunTipo = (control: AbstractControl): ValidationErrors | null => {
    const value = control.value;
    if (!value) return null;
    const digitos = value.replace(/\D/g, '');
    if (digitos.length !== 11) return null; // si es DNI puro, no aplica

    const tipoPersona = this.form?.controls.tipoPersona?.value ?? 'FISICA';
    const tipoEntidad = tipoPersona === 'JURIDICA' ? 'JURIDICA' : 'FISICA';
    return cuitPrefijoValidator(tipoEntidad)(control);
  };

  // Form
  readonly form = this.fb.nonNullable.group({
    tipoPersona: ['FISICA' as TipoPersona, [Validators.required]],
    nombreRazonSocial: ['', [Validators.required, Validators.maxLength(200)]],
    dniCuit: [ '', [ Validators.required,
      dniOCuitSegunTipoValidator('tipoPersona'),
      cuitDigitoVerificadorValidator,
      this.validarPrefijoSegunTipo ]],
    email: ['', [Validators.email, Validators.maxLength(255)]],
    telefono: ['', [Validators.maxLength(30)]],
    domicilio: ['', [Validators.required, Validators.maxLength(255)]],
  });

  // Label dinámico del campo dniCuit según el tipo de persona seleccionado
  readonly etiquetaDniCuit = computed(() => {
    const tipo = this.tipoPersonaSignal();
    return tipo === 'JURIDICA' ? 'CUIT' : 'DNI o CUIT';
  });

  readonly hintDniCuit = computed(() => {
    const tipo = this.tipoPersonaSignal();
    return tipo === 'JURIDICA'
      ? 'Formato: XX-XXXXXXXX-X'
      : 'DNI (7-8 dígitos) o CUIT (XX-XXXXXXXX-X)';
  });

  // Signal que se actualiza cuando cambia el control de tipoPersona
  private readonly tipoPersonaSignal = signal<TipoPersona>('FISICA');

  ngOnInit(): void {
    // Detectar modo según la ruta
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      if (Number.isFinite(id)) {
        this.comitenteId.set(id);
        this.cargarComitente(id);
      } else {
        this.errorCarga.set('El identificador del comitente no es válido.');
      }
    }

    // Re-disparar validación de dniCuit cuando cambia el tipoPersona
    // (el validator depende de ambos campos)
    this.form.controls.tipoPersona.valueChanges.subscribe((nuevoTipo) => {
      this.tipoPersonaSignal.set(nuevoTipo);
      this.form.controls.dniCuit.updateValueAndValidity();
    });
  }

  private cargarComitente(id: number): void {
    this.cargando.set(true);
    this.errorCarga.set(null);

    this.comitenteService.obtenerPorId(id).subscribe({
      next: (c) => {
        this.form.patchValue({
          tipoPersona: c.tipoPersona,
          nombreRazonSocial: c.nombreRazonSocial,
          dniCuit: c.dniCuit,
          email: c.email ?? '',
          telefono: c.telefono ?? '',
          domicilio: c.domicilio,
        });
        this.tipoPersonaSignal.set(c.tipoPersona);
        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.errorCarga.set('El comitente no existe o no te pertenece.');
        } else {
          this.errorCarga.set('No se pudo cargar el comitente. Intentá recargar.');
        }
        this.cargando.set(false);
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.errorGeneral.set(null);

    const raw = this.form.getRawValue();
    const request: ComitenteRequest = {
      tipoPersona: raw.tipoPersona,
      nombreRazonSocial: raw.nombreRazonSocial.trim(),
      dniCuit: raw.dniCuit.trim(),
      email: raw.email.trim(),
      telefono: raw.telefono.trim() || undefined,
      domicilio: raw.domicilio.trim(),
    };

    const id = this.comitenteId();
    const obs$ = id !== null
      ? this.comitenteService.actualizar(id, request)
      : this.comitenteService.crear(request);

    obs$.subscribe({
      next: (comitente) => {
        this.guardando.set(false);
        this.snackBar.open(
          id !== null
            ? `"${comitente.nombreRazonSocial}" fue actualizado.`
            : `"${comitente.nombreRazonSocial}" fue creado.`,
          'Cerrar',
          { duration: 3000, panelClass: ['snackbar-success'] }
        );
        this.router.navigate(['/comitentes']);
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        this.manejarErrorBackend(err);
      },
    });
  }

  cancelar(): void {
    this.router.navigate(['/comitentes']);
  }

  private manejarErrorBackend(err: HttpErrorResponse): void {
    if (err.status === 0) {
      this.errorGeneral.set('No se pudo conectar al servidor. Verificá tu conexión.');
      return;
    }

    const cuerpo = err.error;

    // Errores por campo (validación)
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

    // Errores generales (ej: DNI/CUIT duplicado entre los activos)
    if (cuerpo?.mensaje) {
      this.errorGeneral.set(cuerpo.mensaje);
      return;
    }

    this.errorGeneral.set('Ocurrió un error al guardar. Intentá de nuevo.');
  }
}
