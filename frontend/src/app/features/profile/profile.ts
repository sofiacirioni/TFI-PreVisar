import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { ProfesionalService } from '../../core/services/profesional.service';
import { CondicionIva, Regional } from '../../core/models/catalogos.model';
import { forkJoin } from 'rxjs/internal/observable/forkJoin';
import { ProfesionalUpdateRequest } from '../../core/models/profesional.model';
import { HttpErrorResponse } from '@angular/common/http';
import { CatalogoService } from '../../core/services/catalogo.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-profile',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    MatCheckboxModule,
    MatDividerModule,
    MatChipsModule,
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly profesionalService = inject(ProfesionalService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly snackBar = inject(MatSnackBar);

  // Estado UI
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly editando = signal(false);
  readonly errorCarga = signal<string | null>(null);
  readonly errorGeneral = signal<string | null>(null);

  // Datos
  readonly perfil = this.profesionalService.perfilActual;
  readonly regionales = signal<Regional[]>([]);
  readonly condicionesIva = signal<CondicionIva[]>([]);

  // Derivados para mostrar nombres en modo lectura
  readonly regionalNombre = computed(() => this.perfil()?.regionalNombre ?? '—');
  readonly provinciaNombre = computed(() => this.perfil()?.provinciaNombre ?? '—');
  readonly condicionIvaNombre = computed(
    () => this.perfil()?.condicionIvaDescripcion ?? '—'
  );

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    apellido: ['', [Validators.required, Validators.maxLength(100)]],
    titulo: ['', [Validators.required, Validators.maxLength(150)]],
    domicilio: ['', [Validators.required, Validators.maxLength(255)]],
    telefono: ['', [Validators.maxLength(30)]],
    regionalId: [null as number | null, [Validators.required]],
    condicionIvaId: [null as number | null, [Validators.required]],
    afiliadoCaja8470: [false, [Validators.required]],
  });

  ngOnInit(): void {
    this.cargarTodo();
  }

  private cargarTodo(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);

    forkJoin({
      perfil: this.profesionalService.cargarPerfil(),
      regionales: this.catalogoService.listarRegionales(),
      condicionesIva: this.catalogoService.listarCondicionesIva(),
    }).subscribe({
      next: ({ regionales, condicionesIva }) => {
        this.regionales.set(regionales);
        this.condicionesIva.set(condicionesIva);
        this.cargando.set(false);
      },
      error: () => {
        this.errorCarga.set('No se pudo cargar el perfil. Recargá la página.');
        this.cargando.set(false);
      },
    });
  }

  comenzarEdicion(): void {
    const p = this.perfil();
    if (!p) return;

    // Cargamos los datos actuales en el form
    this.form.patchValue({
      nombre: p.nombre,
      apellido: p.apellido,
      titulo: p.titulo,
      domicilio: p.domicilio,
      telefono: p.telefono ?? '',
      regionalId: p.regionalId,
      condicionIvaId: p.condicionIvaId,
      afiliadoCaja8470: p.afiliadoCaja8470,
    });

    this.errorGeneral.set(null);
    this.editando.set(true);
  }

  cancelarEdicion(): void {
    this.form.reset();
    this.errorGeneral.set(null);
    this.editando.set(false);
  }

  guardar(): void {
    if (this.form.invalid || this.guardando()) {
      this.form.markAllAsTouched();
      return;
    }

    this.guardando.set(true);
    this.errorGeneral.set(null);

    const raw = this.form.getRawValue();
    const datos: ProfesionalUpdateRequest = {
      nombre: raw.nombre,
      apellido: raw.apellido,
      titulo: raw.titulo,
      domicilio: raw.domicilio,
      telefono: raw.telefono || undefined,
      regionalId: raw.regionalId!,
      condicionIvaId: raw.condicionIvaId!,
      afiliadoCaja8470: raw.afiliadoCaja8470,
    };

    this.profesionalService.actualizarPerfil(datos).subscribe({
      next: () => {
        this.guardando.set(false);
        this.editando.set(false);
        this.snackBar.open('Perfil actualizado correctamente', 'Cerrar', {
          duration: 3000,
          panelClass: ['snackbar-success'],
        });
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

    if (cuerpo?.mensaje) {
      this.errorGeneral.set(cuerpo.mensaje);
      return;
    }

    this.errorGeneral.set('Ocurrió un error al guardar. Intentá de nuevo.');
  }

}
