import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { ObraService } from '../../../core/services/obra.service';
import { ComitenteService } from '../../../core/services/comitente.service';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { MatSnackBar } from '@angular/material/snack-bar';
import { Comitente, Obra, ObraRequest, Provincia } from '../../../core/models';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-obra-form',
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
    MatDividerModule,
    MatTooltipModule,
  ],
  templateUrl: './obra-form.html',
  styleUrl: './obra-form.scss',
})
export class ObraForm implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly obraService = inject(ObraService);
  private readonly comitenteService = inject(ComitenteService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly snackBar = inject(MatSnackBar);

  // Estado UI
  readonly cargando = signal(true);
  readonly guardando = signal(false);
  readonly errorCarga = signal<string | null>(null);
  readonly errorGeneral = signal<string | null>(null);

  // Modo: alta o edición
  private readonly obraId = signal<number | null>(null);
  readonly modoEdicion = computed(() => this.obraId() !== null);

  // Datos para el form
  readonly comitentes = signal<Comitente[]>([]);
  readonly provincias = signal<Provincia[]>([]);

  // Comitente asociado (solo se muestra en modo edición, no se puede cambiar)
  readonly comitenteAsociado = signal<{ nombre: string; dniCuit: string } | null>(null);

  readonly form = this.fb.nonNullable.group({
    // Solo se usa en modo alta
    comitenteId: [null as number | null, [Validators.required]],

    // Identificación de la obra
    designacion: ['', [Validators.required, Validators.maxLength(255)]],

    // Dirección
    calle: ['', [Validators.required, Validators.maxLength(150)]],
    numero: ['', [Validators.required, Validators.maxLength(20)]],
    barrio: ['', [Validators.maxLength(100)]],
    localidad: ['', [Validators.required, Validators.maxLength(100)]],
    provinciaId: [null as number | null, [Validators.required]],
    codigoPostal: [
      '',
      [Validators.required, Validators.pattern(/^[A-Z0-9]{4,10}$/)],
    ],

    // Datos catastrales (opcionales)
    circunscripcion: ['', [Validators.pattern(/^\d{1,10}$/)]],
    seccion: ['', [Validators.pattern(/^\d{1,10}$/)]],
    manzana: ['', [Validators.pattern(/^\d{1,10}$/)]],
    parcela: ['', [Validators.pattern(/^\d{1,10}$/)]],
  });

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    if (idParam) {
      const id = Number(idParam);
      if (Number.isFinite(id)) {
        this.obraId.set(id);
        this.cargarObraYCatalogos(id);
      } else {
        this.errorCarga.set('El identificador de la obra no es válido.');
        this.cargando.set(false);
      }
    } else {
      this.cargarSoloCatalogos();
    }
  }

  /** Modo alta: necesitamos comitentes + provincias. */
  private cargarSoloCatalogos(): void {
    forkJoin({
      comitentes: this.comitenteService.listar(),
      provincias: this.catalogoService.listarProvincias(),
    }).subscribe({
      next: ({ comitentes, provincias }) => {
        this.comitentes.set(comitentes);
        this.provincias.set(provincias);
        this.cargando.set(false);

        if (comitentes.length === 0) {
          this.errorCarga.set('Necesitás tener al menos un comitente antes de crear una obra.');
        }
      },
      error: () => {
        this.errorCarga.set('No se pudieron cargar los datos del formulario.');
        this.cargando.set(false);
      },
    });
  }

  /** Modo edición: necesitamos la obra + provincias. El comitente viene en el response de la obra. */
  private cargarObraYCatalogos(id: number): void {
    forkJoin({
      obra: this.obraService.obtenerPorId(id),
      provincias: this.catalogoService.listarProvincias(),
    }).subscribe({
      next: ({ obra, provincias }) => {
        this.provincias.set(provincias);
        this.comitenteAsociado.set({
          nombre: obra.comitenteNombre,
          dniCuit: obra.comitenteDniCuit,
        });

        this.form.patchValue({
          comitenteId: obra.comitenteId,
          designacion: obra.designacion,
          calle: obra.calle,
          numero: obra.numero,
          barrio: obra.barrio ?? '',
          localidad: obra.localidad,
          provinciaId: obra.provinciaId,
          codigoPostal: obra.codigoPostal,
          circunscripcion: obra.circunscripcion ?? '',
          seccion: obra.seccion ?? '',
          manzana: obra.manzana ?? '',
          parcela: obra.parcela ?? '',
        });

        // En modo edición el comitente no se cambia: deshabilitamos el control
        this.form.controls.comitenteId.disable();

        this.cargando.set(false);
      },
      error: (err: HttpErrorResponse) => {
        if (err.status === 404) {
          this.errorCarga.set('La obra no existe o no te pertenece.');
        } else {
          this.errorCarga.set('No se pudo cargar la obra. Intentá recargar.');
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
    const request: ObraRequest = {
      designacion: raw.designacion.trim(),
      calle: raw.calle.trim(),
      numero: raw.numero.trim(),
      barrio: raw.barrio.trim() || undefined,
      localidad: raw.localidad.trim(),
      provinciaId: raw.provinciaId!,
      codigoPostal: raw.codigoPostal.trim().toUpperCase(),
      circunscripcion: raw.circunscripcion.trim() || undefined,
      seccion: raw.seccion.trim() || undefined,
      manzana: raw.manzana.trim() || undefined,
      parcela: raw.parcela.trim() || undefined,
    };

    const id = this.obraId();
    const obs$ = id !== null
      ? this.obraService.actualizar(id, request)
      : this.obraService.crear(raw.comitenteId!, request);

    obs$.subscribe({
      next: (obra) => {
        this.guardando.set(false);
        this.snackBar.open(
          id !== null
            ? `"${obra.designacion}" fue actualizada.`
            : `"${obra.designacion}" fue creada.`,
          'Cerrar',
          { duration: 3000, panelClass: ['snackbar-success'] }
        );
        this.router.navigate(['/obras']);
      },
      error: (err: HttpErrorResponse) => {
        this.guardando.set(false);
        this.manejarErrorBackend(err);
      },
    });
  }

  cancelar(): void {
    this.router.navigate(['/obras']);
  }

  private manejarErrorBackend(err: HttpErrorResponse): void {
    if (err.status === 0) {
      this.errorGeneral.set('No se pudo conectar al servidor. Verificá tu conexión.');
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
