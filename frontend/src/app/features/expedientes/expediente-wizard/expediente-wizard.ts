import { Component, DestroyRef, inject, OnInit, signal, computed } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { ExpedienteService } from '../../../core/services/expediente.service';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatStepper, MatStepperModule } from '@angular/material/stepper';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { StepperSelectionEvent } from '@angular/cdk/stepper';
import { tap } from 'rxjs/operators';
import { firstValueFrom, Observable, of } from 'rxjs';
import { ExpedienteRequest, ExpedienteResponse } from '../../../core/models/expediente.model';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatRadioModule } from '@angular/material/radio';
import { MatIconModule } from '@angular/material/icon';
import { CatalogoService } from '../../../core/services/catalogo.service';
import { Especialidad, Provincia, TipoTarea } from '../../../core/models';
import { ComitenteService } from '../../../core/services/comitente.service';
import { Comitente, ComitenteRequest, TipoPersona } from '../../../core/models/comitente.model';
import { ObraService } from '../../../core/services/obra.service';
import { Obra, ObraRequest } from '../../../core/models/obra.model';
import { MatInputModule } from '@angular/material/input';
import { HttpErrorResponse } from '@angular/common/http';
import { debounceTime, distinctUntilChanged, filter } from 'rxjs/operators';
import { CurrencyPipe } from '@angular/common';
import { AportesResponse } from '../../../core/models/aportes.model';

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
    CurrencyPipe,
    MatProgressBarModule,
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
  private readonly obraService = inject(ObraService);
  private readonly destroyRef = inject(DestroyRef);

  readonly especialidades = signal<Especialidad[]>([]);
  readonly tiposTarea = signal<TipoTarea[]>([]);
  readonly cargandoTipos = signal(false);
  readonly provincias = signal<Provincia[]>([]);

  readonly nombreControl = this.fb.control<string>('');

  readonly aportes = signal<AportesResponse | null>(null);
  readonly calculandoAportes = signal(false);

  // ===== Estado del paso "Comitente" =====
  readonly dniCuitBusqueda = this.fb.control<string>('', {
    nonNullable: true,
    validators: [Validators.required, Validators.pattern(/^(\d{7,8}|\d{2}-\d{8}-\d{1})$/)],
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

  // ===== Estado del paso "Obra" =====
  readonly obrasDelComitente = signal<Obra[]>([]);
  readonly cargandoObras = signal(false);
  readonly creandoObra = signal(false);
  readonly modoNuevaObra = signal(false);

  // Form inline para dar de alta una obra nueva (no incluye comitenteId:
  // se toma del paso anterior).
  readonly nuevaObraForm = this.fb.nonNullable.group({
    designacion: ['', [Validators.required, Validators.maxLength(255)]],
    calle: ['', [Validators.required, Validators.maxLength(150)]],
    numero: ['', [Validators.required, Validators.maxLength(20)]],
    barrio: ['', [Validators.maxLength(100)]],
    localidad: ['', [Validators.required, Validators.maxLength(100)]],
    provinciaId: [null as number | null, [Validators.required]],
    codigoPostal: ['', [Validators.required, Validators.pattern(/^[A-Z0-9]{4,10}$/)]],
    circunscripcion: ['', [Validators.pattern(/^\d{1,10}$/)]],
    seccion: ['', [Validators.pattern(/^\d{1,10}$/)]],
    manzana: ['', [Validators.pattern(/^\d{1,10}$/)]],
    parcela: ['', [Validators.pattern(/^\d{1,10}$/)]],
  });

  // Estado del wizard (vive en el componente, no en el service stateless)
  readonly expedienteId = signal<number | null>(null);
  readonly expediente = signal<ExpedienteResponse | null>(null);
  readonly guardando = signal(false);
  readonly cargando = signal(false);

  // Un FormGroup por paso
  readonly tareaForm = this.fb.group({
    especialidadId: this.fb.control<number | null>(null, Validators.required),
    tipoTareaId: this.fb.control<number | null>(null, Validators.required),
  });
  readonly comitenteForm = this.fb.group({
    comitenteId: this.fb.control<number | null>(null, Validators.required),
  });
  readonly obraForm = this.fb.group({
    obraId: this.fb.control<number | null>(null, Validators.required),
  });
  readonly economicoForm = this.fb.group({
    honorariosReferenciales: this.fb.control<number | null>(null, [
      Validators.required,
      Validators.min(0),
    ]),
  });

  ngOnInit(): void {
    this.catalogoService.listarEspecialidades().subscribe({
      next: (esps) => this.especialidades.set(esps),
      error: () =>
        this.snackBar.open('No se pudieron cargar las especialidades', 'Cerrar', {
          duration: 4000,
        }),
    });

    this.catalogoService.listarProvincias().subscribe({
      next: (provs) => this.provincias.set(provs),
      error: () =>
        this.snackBar.open('No se pudieron cargar las provincias', 'Cerrar', { duration: 4000 }),
    });

    // Cuando cambia la especialidad, recargamos los tipos filtrados y limpiamos
    // el tipo previamente elegido si ya no pertenece a la nueva especialidad.
    this.tareaForm.controls.especialidadId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((especialidadId) => {
        if (especialidadId === null) {
          this.tiposTarea.set([]);
          this.tareaForm.controls.tipoTareaId.setValue(null);
          return;
        }
        this.cargarTiposTarea(especialidadId);
      });

    // Cuando cambia el comitente (selección o creación), precargamos sus obras
    // y reseteamos cualquier obra previamente elegida para evitar cruces.
    this.comitenteForm.controls.comitenteId.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((comitenteId) => {
        if (comitenteId !== null) {
          this.cargarObrasDelComitente(comitenteId);
        } else {
          this.obrasDelComitente.set([]);
        }
        // Solo limpiamos la obra si NO coincide con la del expediente cargado
        // (en modo retomar, el patchValue de obraId viene antes y queremos preservarla).
        const obraActual = this.obraForm.controls.obraId.value;
        if (obraActual !== null && !this.obrasDelComitente().some((o) => o.id === obraActual)) {
          this.obraForm.controls.obraId.setValue(null);
        }
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
        // OJO: el patchValue de especialidadId dispara cargarTiposTarea via
        // valueChanges. Seteamos tipoTareaId DESPUÉS de que termine la carga,
        // para que el select tenga el valor disponible al hacer el match.
        this.tareaForm.patchValue({ especialidadId: exp.especialidadId });
        if (exp.tipoTareaId !== null && exp.especialidadId !== null) {
          this.cargarTiposTarea(exp.especialidadId, () => {
            this.tareaForm.patchValue({ tipoTareaId: exp.tipoTareaId });
          });
        }
        if (exp.tipoTareaId != null && exp.honorariosReferenciales != null) {
          this.calcularAportes();
        }

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

    this.economicoForm.controls.honorariosReferenciales.valueChanges
      .pipe(
        debounceTime(600),
        distinctUntilChanged(),
        filter((v) => v != null && v >= 0),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => this.calcularAportes());
  }

  private calcularAportes(): void {
    const tipoTareaId = this.tareaForm.controls.tipoTareaId.value;
    const honorarios = this.economicoForm.controls.honorariosReferenciales.value;
    if (tipoTareaId == null || honorarios == null || honorarios < 0) {
      this.aportes.set(null);
      return;
    }
    this.calculandoAportes.set(true);
    this.expedienteService
      .calcularAportes({ tipoTareaId, honorariosReferenciales: honorarios })
      .subscribe({
        next: (resp) => {
          this.aportes.set(resp);
          this.calculandoAportes.set(false);
        },
        error: () => {
          this.calculandoAportes.set(false);
          this.aportes.set(null);
          this.snackBar.open('No se pudieron calcular los aportes', 'Cerrar', { duration: 4000 });
        },
      });
  }

  /** Recarga los tipos de tarea filtrados por especialidad. */
  private cargarTiposTarea(especialidadId: number, onDone?: () => void): void {
    this.cargandoTipos.set(true);
    this.catalogoService.listarTiposTarea(especialidadId).subscribe({
      next: (tipos) => {
        this.tiposTarea.set(tipos);
        this.cargandoTipos.set(false);
        // Si el tipoTarea elegido ya no pertenece a la especialidad, lo limpio.
        const tipoActual = this.tareaForm.controls.tipoTareaId.value;
        if (tipoActual !== null && !tipos.some((t) => t.id === tipoActual)) {
          this.tareaForm.controls.tipoTareaId.setValue(null);
        }
        onDone?.();
      },
      error: () => {
        this.tiposTarea.set([]);
        this.cargandoTipos.set(false);
        this.snackBar.open('No se pudieron cargar los tipos de tarea', 'Cerrar', {
          duration: 4000,
        });
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

  // ===== Métodos del paso "Obra" =====

  /** Carga las obras del comitente para mostrarlas como opciones a elegir. */
  private cargarObrasDelComitente(comitenteId: number): void {
    this.cargandoObras.set(true);
    this.obraService.listarPorComitente(comitenteId).subscribe({
      next: (obras) => {
        this.obrasDelComitente.set(obras);
        this.cargandoObras.set(false);
      },
      error: () => {
        this.obrasDelComitente.set([]);
        this.cargandoObras.set(false);
        this.snackBar.open('No se pudieron cargar las obras del comitente', 'Cerrar', {
          duration: 4000,
        });
      },
    });
  }

  /** Selecciona una obra existente como la del expediente. */
  seleccionarObraExistente(o: Obra): void {
    this.obraForm.controls.obraId.setValue(o.id);
    this.modoNuevaObra.set(false);
  }

  /** Cambia al modo "crear obra nueva" y limpia la selección actual. */
  iniciarNuevaObra(): void {
    this.modoNuevaObra.set(true);
    this.obraForm.controls.obraId.setValue(null);
    this.nuevaObraForm.reset();
  }

  /** Vuelve al modo "elegir obra existente" descartando el form. */
  cancelarNuevaObra(): void {
    this.modoNuevaObra.set(false);
    this.nuevaObraForm.reset();
  }

  /**
   * Handler del botón "Siguiente" del paso Obra. Antes de avanzar
   * asegura que haya un obraId seleccionado (creando una nueva si hace falta).
   */
  async continuarDesdeObra(stepper: MatStepper): Promise<void> {
    const ok = await this.asegurarObra();
    if (ok) stepper.next();
  }

  /** Devuelve true si ya hay (o se creó) un obraId válido para este expediente. */
  private async asegurarObra(): Promise<boolean> {
    // Caso 1: ya está seleccionada una obra existente
    if (this.obraForm.controls.obraId.value !== null) {
      return true;
    }

    // Caso 2: el usuario no eligió ni está creando una nueva
    if (!this.modoNuevaObra()) {
      this.snackBar.open('Elegí una obra o creá una nueva para continuar', 'Cerrar', {
        duration: 3000,
      });
      return false;
    }

    // Caso 3: validar el form inline de nueva obra
    if (this.nuevaObraForm.invalid) {
      this.nuevaObraForm.markAllAsTouched();
      this.snackBar.open('Completá los datos de la nueva obra', 'Cerrar', { duration: 3000 });
      return false;
    }

    const comitenteId = this.comitenteForm.controls.comitenteId.value;
    if (comitenteId === null) {
      // No debería pasar (el wizard fuerza el orden), pero por las dudas.
      this.snackBar.open('Faltó elegir el comitente en el paso anterior', 'Cerrar', {
        duration: 3000,
      });
      return false;
    }

    const raw = this.nuevaObraForm.getRawValue();
    const request: ObraRequest = {
      designacion: raw.designacion.trim(),
      calle: raw.calle.trim(),
      numero: raw.numero.trim(),
      barrio: raw.barrio.trim() || undefined,
      localidad: raw.localidad.trim(),
      provinciaId: raw.provinciaId!,
      codigoPostal: raw.codigoPostal.trim(),
      circunscripcion: raw.circunscripcion.trim() || undefined,
      seccion: raw.seccion.trim() || undefined,
      manzana: raw.manzana.trim() || undefined,
      parcela: raw.parcela.trim() || undefined,
    };

    this.creandoObra.set(true);
    try {
      const creada = await firstValueFrom(this.obraService.crear(comitenteId, request));
      this.obrasDelComitente.update((list) => [creada, ...list]);
      this.obraForm.controls.obraId.setValue(creada.id);
      this.modoNuevaObra.set(false);
      this.nuevaObraForm.reset();
      return true;
    } catch (err: unknown) {
      const httpErr = err as HttpErrorResponse | undefined;
      const msg = httpErr?.error?.mensaje ?? 'No se pudo crear la obra';
      this.snackBar.open(msg, 'Cerrar', { duration: 4000 });
      return false;
    } finally {
      this.creandoObra.set(false);
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

  // En modo edición (expediente ya generado / EN_PROCESO) el wizard NO regenera:
  // solo guarda los cambios con un PATCH. completar() se llama una sola vez,
  // al generar desde BORRADOR.
  readonly esEdicion = computed(() => this.expediente()?.estado === 'EN_PROCESO');
  readonly botonLabel = computed(() =>
    this.esEdicion() ? 'Guardar cambios' : 'Generar expediente',
  );

  async accionPrincipal(): Promise<void> {
    this.guardando.set(true);
    try {
      // Garantizo que lo último cargado quede guardado (crea el borrador si hace falta).
      const guardado = await firstValueFrom(this.guardarParcial());
      const id = guardado?.id ?? this.expedienteId();
      if (id == null) {
        this.snackBar.open('Cargá al menos un dato antes de generar', 'Cerrar', { duration: 4000 });
        return;
      }

      if (!this.esEdicion()) {
        // BORRADOR -> EN_PROCESO: generar (transición única).
        await firstValueFrom(this.expedienteService.completar(id));
      }
      // Editar post-generación es solo PATCH: no vuelve a BORRADOR ni re-genera.
      this.router.navigate(['/expedientes', id, 'armado']);
    } catch (err) {
      const msg = (err as HttpErrorResponse)?.error?.message ?? 'No se pudo generar el expediente';
      this.snackBar.open(msg, 'Cerrar', { duration: 6000 });
    } finally {
      this.guardando.set(false);
    }
  }

  /** Crea el borrador (primera vez) o lo actualiza parcial. Devuelve el expediente persistido. */
  private guardarParcial(): Observable<ExpedienteResponse | null> {
    const req = this.construirRequest();
    if (this.expedienteId() === null && this.estaVacio(req)) {
      return of(null); // nada que guardar todavia, no creo un borrador vacio
    }
    const id = this.expedienteId();
    const obs$ =
      id === null
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
    return (
      req.nombre == null &&
      req.tipoTareaId == null &&
      req.obraId == null &&
      req.honorariosReferenciales == null
    );
  }

  // === Valores reactivos de los forms usados en el resumen ===
  // computed() no rastrea form.controls.X.value (no es signal). Hay que
  // exponer los valueChanges como signals para que el resumen se recalcule.
  private readonly especialidadIdValue = toSignal(
    this.tareaForm.controls.especialidadId.valueChanges,
    { initialValue: this.tareaForm.controls.especialidadId.value },
  );
  private readonly tipoTareaIdValue = toSignal(this.tareaForm.controls.tipoTareaId.valueChanges, {
    initialValue: this.tareaForm.controls.tipoTareaId.value,
  });
  private readonly obraIdValue = toSignal(this.obraForm.controls.obraId.valueChanges, {
    initialValue: this.obraForm.controls.obraId.value,
  });
  private readonly honorariosValue = toSignal(
    this.economicoForm.controls.honorariosReferenciales.valueChanges,
    { initialValue: this.economicoForm.controls.honorariosReferenciales.value },
  );

  // nombres derivados para mostrar en el resumen (paso 5)
  readonly especialidadNombreSeleccionada = computed(() => {
    const id = this.especialidadIdValue();
    return this.especialidades().find((e) => e.id === id)?.nombre ?? null;
  });

  readonly tipoTareaNombreSeleccionado = computed(() => {
    const id = this.tipoTareaIdValue();
    return this.tiposTarea().find((t) => t.id === id)?.nombre ?? null;
  });

  // el mismo criterio que valida el back en "generar", pero para feedback en vivo
  readonly faltanDatos = computed(() => {
    const faltan: string[] = [];
    if (this.tipoTareaIdValue() == null) faltan.push('tipo de tarea');
    if (this.obraIdValue() == null) faltan.push('obra');
    if (this.honorariosValue() == null) faltan.push('honorarios referenciales');
    return faltan;
  });
}
