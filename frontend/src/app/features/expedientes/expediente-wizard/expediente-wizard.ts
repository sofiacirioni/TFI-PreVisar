import { Component, inject, OnInit, signal } from '@angular/core';
import { ExpedienteService } from '../../../core/services/expediente.service';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { switchMap, tap } from 'rxjs/operators';
import { firstValueFrom, Observable, of, throwError } from 'rxjs';
import { ExpedienteRequest, ExpedienteResponse } from '../../../core/models/expediente.model';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatIconModule } from '@angular/material/icon';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { TipoTarea } from '../../../core/models';
import { ComitenteService } from '../../../core/services/comitente.service';
import { Comitente, ComitenteRequest, TipoPersona } from '../../../core/models/comitente.model';
import { MatInputModule } from '@angular/material/input';
import { HttpErrorResponse } from '@angular/common/http';

@Component({
  selector: 'app-expediente-wizard',
  imports: [
    ReactiveFormsModule,
    MatFormFieldModule,
    MatSelectModule,
    MatRadioModule,
    MatStepperModule,
    MatButtonModule,
    MatProgressBarModule,
    MatIconModule,
    MatInputModule,
  ],
  templateUrl: './expediente-wizard.html',
  styleUrl: './expediente-wizard.scss',
})
export class ExpedienteWizard implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);
  private readonly expedienteService = inject(ExpedienteService);
  private readonly catalogoService = inject(CatalogoService);
  private readonly comitenteService = inject(ComitenteService);

  readonly tiposTarea = signal<TipoTarea[]>([]);

  readonly nombreControl = this.fb.control<string>('');

  // ===== Estado del paso "Comitente" =====
  readonly dniCuitBusqueda = this.fb.control<string>('', {
    nonNullable: true,
    validators: [
      Validators.required,
      Validators.pattern(/^(\d{7,8}|\d{2}-\d{8}-\d{1})$/),
    ],
  });
  readonly buscandoComitente = signal(false);
  readonly creandoComitente = signal(false);
  readonly comitenteEncontrado = signal<Comitente | null>(null);
  readonly modoNuevoComitente = signal(false);

  // Form inline para dar de alta un comitente nuevo (no incluye dniCuit:
  // ese se toma del campo de búsqueda).
  readonly nuevoComitenteForm = this.fb.nonNullable.group({
    tipoPersona: ['FISICA' as TipoPersona, [Validators.required]],
    nombreRazonSocial: ['', [Validators.required, Validators.maxLength(200)]],
    email: ['', [Validators.email, Validators.maxLength(255)]],
    telefono: ['', [Validators.maxLength(30)]],
    domicilio: ['', [Validators.required, Validators.maxLength(255)]],
  });

  // Estado del wizard (vive en el componente, no en el service stateless)
  readonly expedienteId = signal<number | null>(null);
  readonly expediente = signal<ExpedienteResponse | null>(null);
  readonly guardando = signal(false);
  readonly cargando = signal(false);

  // Un FormGroup por paso
  readonly tareaForm = this.fb.group({
    tipoTareaId: this.fb.control<number | null>(null, Validators.required),
  });
  readonly comitenteForm = this.fb.group({
    comitenteId: this.fb.control<number | null>(null, Validators.required),
  });
  readonly obraForm = this.fb.group({
    obraId: this.fb.control<number | null>(null, Validators.required),
  });
  readonly economicoForm = this.fb.group({
    honorariosReferenciales: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
  });

  ngOnInit(): void {
    this.catalogoService.listarTiposTarea().subscribe({
      next: (tipos) => this.tiposTarea.set(tipos),
      error: () => this.snackBar.open('No se pudieron cargar los tipos de tarea', 'Cerrar', { duration: 4000 }),
    });

    const idParam = this.route.snapshot.paramMap.get('id');
    if (!idParam) return; // modo "nuevo": el borrador se crea recien al primer guardado

    // modo "retomar": cargo el borrador existente y precargo los forms
    this.cargando.set(true);
    this.expedienteService.obtener(Number(idParam)).subscribe({
      next: (exp) => {
        this.expedienteId.set(exp.id);
        this.expediente.set(exp);
        this.nombreControl.setValue(exp.nombre ?? '');
        this.tareaForm.patchValue({ tipoTareaId: exp.tipoTareaId });
        this.obraForm.patchValue({ obraId: exp.obraId });
        this.economicoForm.patchValue({ honorariosReferenciales: exp.honorariosReferenciales });
        this.comitenteForm.patchValue({ comitenteId: exp.comitenteId });
        this.cargando.set(false);
      },
      error: () => {
        this.cargando.set(false);
        this.snackBar.open('No se encontró el expediente', 'Cerrar', { duration: 4000 });
        this.router.navigate(['/expedientes']);
      },
    });
  }

  /** Busca un comitente por DNI/CUIT en la cartera del profesional. */
  buscarComitente(): void {
    if (this.dniCuitBusqueda.invalid) {
      this.dniCuitBusqueda.markAsTouched();
      return;
    }

    const dniCuit = this.dniCuitBusqueda.value.trim();
    this.buscandoComitente.set(true);
    this.comitenteEncontrado.set(null);
    this.modoNuevoComitente.set(false);
    this.comitenteForm.controls.comitenteId.setValue(null);

    this.comitenteService.buscarPorDniCuit(dniCuit).subscribe({
      next: (c) => {
        this.buscandoComitente.set(false);
        if (c) {
          // Encontrado en la cartera: lo seleccionamos automáticamente
          this.comitenteEncontrado.set(c);
          this.comitenteForm.controls.comitenteId.setValue(c.id);
        } else {
          // No existe: habilitamos el form inline para crearlo
          this.modoNuevoComitente.set(true);
        }
      },
      error: () => {
        this.buscandoComitente.set(false);
        this.snackBar.open('No se pudo buscar el comitente', 'Cerrar', { duration: 4000 });
      },
    });
  }

  /** Resetea la búsqueda para volver a empezar con otro DNI/CUIT. */
  cambiarBusqueda(): void {
    this.comitenteEncontrado.set(null);
    this.modoNuevoComitente.set(false);
    this.comitenteForm.controls.comitenteId.setValue(null);
    this.nuevoComitenteForm.reset({ tipoPersona: 'FISICA' });
  }

  /**
   * Handler del botón "Siguiente" del paso Comitente. Antes de avanzar
   * asegura que haya un comitenteId seleccionado (creando uno nuevo si hace falta).
   */
  async continuarDesdeComitente(stepper: MatStepper): Promise<void> {
    const ok = await this.asegurarComitente();
    if (ok) stepper.next();
  }

  /** Devuelve true si ya hay (o se creó) un comitenteId válido para este expediente. */
  private async asegurarComitente(): Promise<boolean> {
    // Caso 1: ya está seleccionado (encontrado en cartera o creado previamente)
    if (this.comitenteForm.controls.comitenteId.value !== null) {
      return true;
    }

    // Caso 2: el usuario no buscó ni creó nada
    if (!this.modoNuevoComitente()) {
      this.snackBar.open('Buscá un comitente para continuar', 'Cerrar', { duration: 3000 });
      return false;
    }

    // Caso 3: hay que crear el comitente nuevo con los datos del form inline
    if (this.nuevoComitenteForm.invalid) {
      this.nuevoComitenteForm.markAllAsTouched();
      this.snackBar.open('Completá los datos del nuevo comitente', 'Cerrar', { duration: 3000 });
      return false;
    }

    const raw = this.nuevoComitenteForm.getRawValue();
    const request: ComitenteRequest = {
      tipoPersona: raw.tipoPersona,
      nombreRazonSocial: raw.nombreRazonSocial.trim(),
      dniCuit: this.dniCuitBusqueda.value.trim(),
      domicilio: raw.domicilio.trim(),
      email: raw.email.trim(),
      telefono: raw.telefono.trim() || undefined,
    };

    this.creandoComitente.set(true);
    try {
      const creado = await firstValueFrom(this.comitenteService.crear(request));
      this.comitenteForm.controls.comitenteId.setValue(creado.id);
      this.comitenteEncontrado.set(creado);
      this.modoNuevoComitente.set(false);
      return true;
    } catch (err: unknown) {
      const httpErr = err as HttpErrorResponse | undefined;
      const msg = httpErr?.error?.mensaje ?? 'No se pudo crear el comitente';
      this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
      return false;
    } finally {
      this.creandoComitente.set(false);
    }
  }

  onStepChange(_event: StepperSelectionEvent): void {
    this.guardando.set(true);
    this.guardarParcial().subscribe({
      next: () => this.guardando.set(false),
      error: () => {
        this.guardando.set(false);
        this.snackBar.open('No se pudo guardar el progreso', 'Cerrar', { duration: 4000 });
      },
    });
  }

  finalizar(): void {
    this.guardando.set(true);
    // Garantizo que lo ultimo este guardado ANTES de completar
    this.guardarParcial()
      .pipe(
        switchMap((resp) => {
          const id = resp?.id ?? this.expedienteId();
          if (id === null) return throwError(() => new Error('SIN_DATOS'));
          return this.expedienteService.completar(id);
        }),
      )
      .subscribe({
        next: () => {
          this.guardando.set(false);
          this.snackBar.open('Expediente finalizado', 'Cerrar', { duration: 3000 });
          this.router.navigate(['/expedientes']);
        },
        error: (err) => {
          this.guardando.set(false);
          if (err?.message === 'SIN_DATOS') {
            this.snackBar.open('Cargá al menos un dato antes de finalizar', 'Cerrar', { duration: 4000 });
            return;
          }
          // El back devuelve 400 con el detalle de que falta
          const msg = err?.error?.message ?? 'No se pudo finalizar el expediente';
          this.snackBar.open(msg, 'Cerrar', { duration: 6000 });
        },
      });
  }

  /** Crea el borrador (primera vez) o lo actualiza parcial. Devuelve el expediente persistido. */
  private guardarParcial(): Observable<ExpedienteResponse | null> {
    const req = this.construirRequest();
    if (this.expedienteId() === null && this.estaVacio(req)) {
      return of(null); // nada que guardar todavia, no creo un borrador vacio
    }
    const id = this.expedienteId();
    const obs$ = id === null
      ? this.expedienteService.crear(req)
      : this.expedienteService.actualizarParcial(id, req);

    return obs$.pipe(
      tap((resp) => {
        this.expedienteId.set(resp.id);
        this.expediente.set(resp);
      }),
    );
  }

  private construirRequest(): ExpedienteRequest {
    return {
      nombre: this.nombreControl.value?.trim() || null,
      tipoTareaId: this.tareaForm.controls.tipoTareaId.value,
      obraId: this.obraForm.controls.obraId.value,
      honorariosReferenciales: this.economicoForm.controls.honorariosReferenciales.value,
    };
  }

  private estaVacio(req: ExpedienteRequest): boolean {
    return req.nombre == null && req.tipoTareaId == null
      && req.obraId == null && req.honorariosReferenciales == null;
  }
}
