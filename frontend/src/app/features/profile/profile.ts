import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
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
import { NgxMatSelectSearchModule } from 'ngx-mat-select-search';
import { ProfesionalService } from '../../core/services/profesional.service';
import { CondicionIva, Regional, Titulo } from '../../core/models/catalogos.model';
import { forkJoin } from 'rxjs/internal/observable/forkJoin';
import { ProfesionalUpdateRequest } from '../../core/models/profesional.model';
import { HttpErrorResponse } from '@angular/common/http';
import { CatalogoService } from '../../core/services/catalogo.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { toSignal } from '@angular/core/rxjs-interop';
import { MatDialog } from '@angular/material/dialog';
import { CambiarPasswordDialog } from '../../shared/components/cambiar-password-dialog/cambiar-password-dialog';
import { DarDeBajaDialog } from '../../shared/components/dar-de-baja-dialog/dar-de-baja-dialog';
import { AuthService } from '../../core/services/auth.service';
import { firstValueFrom } from 'rxjs/internal/firstValueFrom';

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
    NgxMatSelectSearchModule,
  ],
  templateUrl: './profile.html',
  styleUrl: './profile.scss',
})
export class Profile implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly profesionalService = inject(ProfesionalService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly snackBar = inject(MatSnackBar);
  private readonly dialog = inject(MatDialog);
  private readonly authService = inject(AuthService);

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
  readonly titulos = signal<Titulo[]>([]);

  // Derivados para mostrar nombres en modo lectura
  readonly regionalNombre = computed(() => this.perfil()?.regionalNombre ?? '—');
  readonly provinciaNombre = computed(() => this.perfil()?.provinciaNombre ?? '—');
  readonly condicionIvaNombre = computed(() => this.perfil()?.condicionIvaDescripcion ?? '—');

  readonly tituloMostrar = computed(() => {
    const p = this.perfil();
    if (!p) return '—';
    return p.tituloOtroDescripcion || p.tituloNombre;
  });

  readonly matriculaCompleta = computed(() => {
    const p = this.perfil();
    if (!p) return '';
    return p.numeroOrden ? `${p.matricula}/${p.numeroOrden}` : p.matricula;
  });

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100)]],
    apellido: ['', [Validators.required, Validators.maxLength(100)]],
    tituloId: [null as number | null, [Validators.required]],
    tituloOtroDescripcion: ['', [Validators.maxLength(150)]],
    domicilio: ['', [Validators.required, Validators.maxLength(255)]],
    telefono: ['', [Validators.maxLength(30)]],
    regionalId: [null as number | null, [Validators.required]],
    condicionIvaId: [null as number | null, [Validators.required]],
    afiliadoCaja8470: [false, [Validators.required]],
  });

  ngOnInit(): void {
    this.cargarTodo();

    this.form.controls.tituloId.valueChanges.subscribe(() => {
      if (!this.tituloRequiereDescripcion()) {
        this.form.controls.tituloOtroDescripcion.setValue('');
        this.form.controls.tituloOtroDescripcion.setErrors(null);
      }
    });
  }

  private cargarTodo(): void {
    this.cargando.set(true);
    this.errorCarga.set(null);

    forkJoin({
      perfil: this.profesionalService.cargarPerfil(),
      regionales: this.catalogoService.listarRegionales(),
      condicionesIva: this.catalogoService.listarCondicionesIva(),
      titulos: this.catalogoService.listarTitulos(),
    }).subscribe({
      next: ({ regionales, condicionesIva, titulos }) => {
        this.regionales.set(regionales);
        this.condicionesIva.set(condicionesIva);
        this.titulos.set(titulos);
        this.cargando.set(false);
      },
      error: () => {
        this.errorCarga.set('No se pudo cargar el perfil. Recargá la página.');
        this.cargando.set(false);
      },
    });
  }

  private readonly tituloIdSignal = toSignal(this.form.controls.tituloId.valueChanges, {
    initialValue: this.form.controls.tituloId.value,
  });

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

  comenzarEdicion(): void {
    const p = this.perfil();
    if (!p) return;

    // Cargamos los datos actuales en el form
    this.form.patchValue({
      nombre: p.nombre,
      apellido: p.apellido,
      tituloId: p.tituloId,
      tituloOtroDescripcion: p.tituloOtroDescripcion ?? '',
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

    const raw = this.form.getRawValue();
    if (this.tituloRequiereDescripcion() && !raw.tituloOtroDescripcion.trim()) {
      this.form.controls.tituloOtroDescripcion.setErrors({ requeridoSiOtro: true });
      this.form.controls.tituloOtroDescripcion.markAsTouched();
      return;
    }

    this.guardando.set(true);
    this.errorGeneral.set(null);

    const datos: ProfesionalUpdateRequest = {
      nombre: raw.nombre,
      apellido: raw.apellido,
      tituloId: raw.tituloId!,
      tituloOtroDescripcion: this.tituloRequiereDescripcion()
        ? raw.tituloOtroDescripcion.trim()
        : undefined,
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

  async abrirCambiarPassword(): Promise<void> {
    const ref = this.dialog.open<CambiarPasswordDialog, void, boolean>(CambiarPasswordDialog, {
      width: '460px',
      disableClose: false,
    });

    const exito = await firstValueFrom(ref.afterClosed());
    if (exito) {
      this.snackBar.open('Contraseña cambiada correctamente', 'Cerrar', {
        duration: 3000,
        panelClass: ['snackbar-success'],
      });
    }
  }

  async abrirDarDeBaja(): Promise<void> {
    const ref = this.dialog.open<DarDeBajaDialog, void, boolean>(DarDeBajaDialog, {
      width: '480px',
      disableClose: false,
    });

    const dadoDeBaja = await firstValueFrom(ref.afterClosed());
    if (dadoDeBaja) {
      this.snackBar.open('Tu cuenta fue dada de baja', 'Cerrar', {
        duration: 4000,
      });
      // La cuenta quedó deshabilitada: cerramos sesión y volvemos al login.
      this.authService.logout();
    }
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
