import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, DestroyRef, effect, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed, toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, filter, map, startWith, switchMap, take } from 'rxjs/operators';
import { combineLatest, fromEvent, merge, Observable, of, Subscription, timer } from 'rxjs';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatButtonModule } from '@angular/material/button';
import {
  DocumentoRequerido,
  EstructuraExpediente,
  SeccionEstructura,
} from '../../../core/models/estructura.model';
import { ExpedienteResponse } from '../../../core/models/expediente.model';
import { ExpedienteService } from '../../../core/services/expediente.service';
import { EstructuraService } from '../../../core/services/estructura.service';
import { DocumentoService } from '../../../core/services/documento.service';
import { DocumentoCargado } from '../../../core/models/documento-cargado.model';
import { DatosContrato } from '../../../core/models/datos-contrato.model';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { MatSnackBar } from '@angular/material/snack-bar';
import { PagarArancelDialog } from '../../../shared/components/pagar-arancel-dialog/pagar-arancel-dialog';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';
import { Observacion, OrigenObservacion, ValidacionResultado } from '../../../core/models/validacion.model';

type Vm =
  | { status: 'loading' }
  | { status: 'error'; error: unknown }
  | { status: 'ok'; expediente: ExpedienteResponse; secciones: SeccionEstructura[] };

type EstadoIA =
  | 'IDLE'
  | 'EN_PROGRESO'
  | 'COMPLETADO'
  | 'COMPLETADO_CON_ERRORES'
  | 'ERROR'
  | 'TIMEOUT';

@Component({
  selector: 'app-expediente-armado',
  imports: [
    CurrencyPipe,
    DecimalPipe,
    RouterLink,
    MatProgressBarModule,
    MatIconModule,
    MatChipsModule,
    MatButtonModule,
    FormsModule,
    MatProgressSpinnerModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule,
    MatExpansionModule,
    NgxExtendedPdfViewerModule,
  ],
  templateUrl: './expediente-armado.html',
  styleUrl: './expediente-armado.scss',
})
export class ExpedienteArmado {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly expedienteService = inject(ExpedienteService);
  private readonly estructuraService = inject(EstructuraService);

  //signals de documentos para descarga y subida.
  private readonly documentoService = inject(DocumentoService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  private readonly destroyRef = inject(DestroyRef);

  readonly generando = signal(false);

  constructor() {
    this.observarVueltaDePago();
    this.reconciliarSiFiguraImpago();
    this.previsualizarGenerables();
    this.invalidarPreviewsAlEditarDatos();
    this.hidratarDatosContrato();
  }

  /** Expedientes ya reconciliados en esta sesión, para no repetir la consulta a MP. */
  private readonly yaReconciliados = new Set<number>();

  /**
   * Si el expediente figura impago, se le pregunta a MP una vez por sesión. Cubre
   * el caso en que el webhook se perdió en otra sesión: sin esto el expediente
   * quedaba desactualizado para siempre, porque el reintento por foco solo aplica
   * cuando el pago se inició en esta pantalla.
   */
  private reconciliarSiFiguraImpago(): void {
    effect(() => {
      const exp = this.expediente();
      if (!exp || exp.estadoArancel !== 'NINGUNO' || this.yaReconciliados.has(exp.id)) return;
      this.yaReconciliados.add(exp.id);
      this.expedienteService
        .sincronizarPago(exp.id)
        .pipe(catchError(() => of(null)))
        .subscribe((r) => {
          if (r && r.estadoArancel !== 'NINGUNO') this.recargar();
        });
    });
  }

  readonly expedienteId = toSignal(this.route.paramMap.pipe(map((pm) => Number(pm.get('id')))), {
    initialValue: 0,
  });

  private readonly refresh = signal(0);
  recargar(): void {
    this.refresh.update((n) => n + 1);
  }

  /** Último expediente que llegó a cargarse: distingue "primera carga" de "refresco". */
  private ultimoIdCargado: number | null = null;

  // Id del expediente que reemite ante cada recarga, para refrescar documentos
  // cargados y validación a la vez (subida/baja/revalidación).
  private readonly expedienteId$ = combineLatest([
    this.route.paramMap,
    toObservable(this.refresh),
  ]).pipe(map(([pm]) => Number(pm.get('id'))));

  readonly validacion = toSignal(
    this.expedienteId$.pipe(
      switchMap((id) =>
        this.documentoService
          .validar(id)
          .pipe(catchError(() => of({ documentos: [], generales: [] } as ValidacionResultado))),
      ),
    ),
    { initialValue: { documentos: [], generales: [] } as ValidacionResultado },
  );

  // Observaciones agrupadas por ranura (un slot multi-archivo junta las de todos sus archivos)
  private readonly obsPorSlot = computed(() => {
    const map = new Map<number, Observacion[]>();
    for (const d of this.validacion().documentos) {
      if (!d.observaciones.length) continue;
      const arr = map.get(d.documentoRequeridoId) ?? [];
      arr.push(...d.observaciones);
      map.set(d.documentoRequeridoId, arr);
    }
    return map;
  });
  conObservacion(docReqId: number): boolean {
    return (this.obsPorSlot().get(docReqId)?.length ?? 0) > 0;
  }

  readonly generales = computed(() => this.validacion().generales);

  // null mientras viaja el primer pedido: hace falta poder distinguir "todavía no sé"
  // de "la ranura está vacía", si no se generaría un PDF al aire en las ranuras que
  // ya tienen archivo subido (ver previsualizarGenerables).
  private readonly documentosCargadosCargando = toSignal(
    this.expedienteId$.pipe(
      switchMap((id) =>
        this.documentoService.listar(id).pipe(catchError(() => of<DocumentoCargado[] | null>([]))),
      ),
    ),
    { initialValue: null },
  );
  readonly documentosCargados = computed(() => this.documentosCargadosCargando() ?? []);

  private readonly archivosPorSlot = computed(() => {
    const map = new Map<number, DocumentoCargado[]>();
    for (const d of this.documentosCargados()) {
      const arr = map.get(d.documentoRequeridoId) ?? [];
      arr.push(d);
      map.set(d.documentoRequeridoId, arr);
    }
    return map;
  });
  archivosDe(docReqId: number): DocumentoCargado[] {
    return this.archivosPorSlot().get(docReqId) ?? [];
  }

  // Archivo que se previsualiza en la hoja central. Por defecto el primero del
  // slot activo; en slots con varios archivos el usuario puede elegir otro.
  private readonly archivoSeleccionadoId = signal<number | null>(null);
  seleccionarArchivo(a: DocumentoCargado): void {
    this.archivoSeleccionadoId.set(a.id);
  }
  readonly archivoActivo = computed<DocumentoCargado | null>(() => {
    const doc = this.docActivo();
    if (!doc) return null;
    const archivos = this.archivosDe(doc.id);
    return archivos.find((a) => a.id === this.archivoSeleccionadoId()) ?? archivos[0] ?? null;
  });

  // Blob del archivo activo para el visor PDF.js (acepta Blob directo, sin
  // object URLs). switchMap cancela la descarga previa al cambiar de archivo.
  readonly previewBlob = toSignal(
    toObservable(this.archivoActivo).pipe(
      switchMap((archivo) => {
        if (!archivo) return of<Blob | null>(null);
        return this.documentoService.descargar(this.expedienteId(), archivo.id).pipe(
          map((resp) => resp.body),
          startWith(null as Blob | null),
          catchError(() => of<Blob | null>(null)),
        );
      }),
    ),
    { initialValue: null as Blob | null },
  );

  // PDFs que produce el sistema (carátula, contrato), cacheados por ranura para no
  // volver a pedirlos cada vez que se abre el slot. No se guardan en el servidor: son
  // estado derivado de los datos del expediente, y generarlos cuesta milisegundos.
  private readonly previewsGeneradas = signal<ReadonlyMap<number, Blob>>(new Map());
  // Ranuras con una generación en curso: evita pedir dos veces el mismo PDF.
  private readonly generandoIds = signal<ReadonlySet<number>>(new Set());

  // El slot admite generación de PDF por el sistema (dato: documento_requerido.generable).
  readonly esGenerable = computed(() => this.docActivo()?.generable ?? false);
  // El contrato es el único generable con campos editables (panel lateral).
  readonly esContrato = computed(() => this.docActivo()?.codigo === 'CONTRATO_LOCACION');
  readonly generandoPreview = computed(() => {
    const id = this.docActivo()?.id;
    return id != null && this.generandoIds().has(id);
  });

  // Fuente del visor central: el archivo subido manda. Si el profesional cargó su
  // versión —el contrato ya firmado, por ejemplo— es la que vale, igual que en el
  // compilado; solo cuando la ranura está vacía se muestra el PDF del sistema.
  readonly viewerSrc = computed<Blob | null>(() => {
    const doc = this.docActivo();
    if (!doc) return null;
    if (this.archivosDe(doc.id).length) return this.previewBlob();
    return this.previewsGeneradas().get(doc.id) ?? null;
  });

  /**
   * Genera la previsualización de la ranura activa cuando hace falta. Al entrar al
   * armado la carátula ya aparece sola, y cada ranura se genera una única vez: después
   * queda en caché hasta que cambien los datos del expediente.
   */
  private previsualizarGenerables(): void {
    effect(() => {
      const doc = this.docActivo();
      if (!doc?.generable) return;
      if (this.documentosCargadosCargando() === null) return; // aún no sé si hay archivo
      if (this.archivosDe(doc.id).length) return; // subió su propia versión
      if (this.previewsGeneradas().has(doc.id)) return; // ya está en caché
      if (this.generandoIds().has(doc.id)) return; // ya se está pidiendo
      this.generarPreview(doc);
    });
  }

  /**
   * Los PDFs generados son función de los datos del expediente: si el profesional los
   * editó (updatedAt cambia), la caché quedó vieja y se descarta para que se regeneren.
   */
  private invalidarPreviewsAlEditarDatos(): void {
    let ultimoUpdatedAt: string | null = null;
    effect(() => {
      const exp = this.expediente();
      if (!exp) return;
      if (ultimoUpdatedAt !== null && ultimoUpdatedAt !== exp.updatedAt) {
        this.previewsGeneradas.set(new Map());
      }
      ultimoUpdatedAt = exp.updatedAt;
    });
  }

  private generarPreview(doc: DocumentoRequerido): void {
    const pdf$ = this.pdfGenerado(doc.codigo);
    if (!pdf$) return;
    this.generandoIds.update((s) => new Set(s).add(doc.id));
    pdf$.subscribe({
      next: (resp) => {
        const blob = resp.body;
        if (blob) this.previewsGeneradas.update((m) => new Map(m).set(doc.id, blob));
        this.finGeneracion(doc.id);
      },
      error: () => this.finGeneracion(doc.id),
    });
  }

  private finGeneracion(docReqId: number): void {
    this.generandoIds.update((s) => {
      const resto = new Set(s);
      resto.delete(docReqId);
      return resto;
    });
  }

  /** Descarta la preview cacheada de la ranura activa; el effect la vuelve a generar. */
  regenerarPreview(): void {
    const doc = this.docActivo();
    if (!doc) return;
    this.previewsGeneradas.update((m) => {
      const resto = new Map(m);
      resto.delete(doc.id);
      return resto;
    });
  }

  // un slot está "cargado" si tiene al menos un archivo activo
  readonly cargados = computed(
    () => new Set(this.documentosCargados().map((d) => d.documentoRequeridoId)),
  );

  readonly subiendo = signal<number | null>(null);

  // Campos editables del contrato (panel lateral). El backend cae a puntos
  // suspensivos si vienen vacíos.
  readonly honorariosPactados = signal<number | null>(null);
  readonly documentacionConfeccion = signal('');
  readonly tareasEspeciales = signal('');
  readonly formaPago = signal('');
  readonly plazoEntrega = signal('');
  readonly gastosEspeciales = signal('');
  readonly guardandoContrato = signal(false);

  /** Expediente ya volcado al panel del contrato: se precarga una sola vez. */
  private hidratadoDe: number | null = null;

  /**
   * Precarga el panel con los campos guardados del contrato. Una sola vez por
   * expediente: recargar() se dispara también al volver a la pestaña, y re-hidratar
   * pisaría lo que el profesional está escribiendo sin haber guardado todavía.
   */
  private hidratarDatosContrato(): void {
    effect(() => {
      const exp = this.expediente();
      if (!exp || this.hidratadoDe === exp.id) return;
      this.hidratadoDe = exp.id;
      const datos = exp.datosContrato;
      this.honorariosPactados.set(datos?.honorariosPactados ?? null);
      this.documentacionConfeccion.set(datos?.documentacionConfeccion ?? '');
      this.tareasEspeciales.set(datos?.tareasEspeciales ?? '');
      this.formaPago.set(datos?.formaPago ?? '');
      this.plazoEntrega.set(datos?.plazoEntrega ?? '');
      this.gastosEspeciales.set(datos?.gastosEspeciales ?? '');
    });
  }

  /**
   * Guarda los campos del contrato en el expediente. Al recargar cambia updatedAt, lo
   * que invalida la caché de previews y hace que el contrato se regenere solo.
   */
  guardarDatosContrato(): void {
    if (this.guardandoContrato()) return;
    this.guardandoContrato.set(true);
    const datos: DatosContrato = {
      honorariosPactados: this.honorariosPactados(),
      documentacionConfeccion: this.documentacionConfeccion() || null,
      tareasEspeciales: this.tareasEspeciales() || null,
      formaPago: this.formaPago() || null,
      plazoEntrega: this.plazoEntrega() || null,
      gastosEspeciales: this.gastosEspeciales() || null,
    };
    this.expedienteService.guardarDatosContrato(this.expedienteId(), datos).subscribe({
      next: () => {
        this.guardandoContrato.set(false);
        this.recargar();
      },
      error: (err: HttpErrorResponse) => {
        this.guardandoContrato.set(false);
        const mensaje = err.error?.mensaje ?? 'No se pudieron guardar los campos del contrato';
        this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
      },
    });
  }

  // Carga encadenada: expediente -> (tipoTareaId, provinciaId) -> estructura.
  // Sin <Vm> explícito: con un único type-arg se descartan los overloads que
  // aceptan initialValue y vm quedaría Signal<Vm | undefined>.
  readonly vm = toSignal(
    // Usa expedienteId$ (paramMap + refresh) y no paramMap solo: si no, recargar()
    // refrescaba documentos y validación pero NO el expediente, y el estado del
    // arancel (el chip) quedaba viejo hasta recargar la página entera.
    this.expedienteId$.pipe(
      switchMap((id) => {
        // Solo la primera carga de un expediente muestra el esqueleto. Los
        // refrescos (resync del pago cada 2,5s, vuelta a la pestaña, subida de
        // archivo) mantienen la vista montada y actualizan en silencio: con el
        // startWith incondicional que había antes, la pantalla parpadeaba
        // entera en cada tick.
        const esOtroExpediente = this.ultimoIdCargado !== id;
        this.ultimoIdCargado = id;
        return this.expedienteService.obtener(id).pipe(
          switchMap((exp) => {
            // Sin tipo de tarea o sin provincia no hay estructura que pedir.
            // Se usa la estructura scopeada al expediente: incluye ranuras retiradas de
            // la estructura vigente que este expediente ya tiene cargadas (desactivado=true).
            const estructura$: Observable<EstructuraExpediente> =
              exp.tipoTareaId == null || exp.provinciaId == null
                ? of({ tipoTareaId: 0, tipoTareaCodigo: '', secciones: [] })
                : this.estructuraService.getEstructuraExpediente(exp.id);
            return estructura$.pipe(
              map((est): Vm => ({ status: 'ok', expediente: exp, secciones: est.secciones })),
            );
          }),
          // El esqueleto solo en la primera carga de este expediente.
          esOtroExpediente ? startWith({ status: 'loading' } as Vm) : (o$: Observable<Vm>) => o$,
          catchError((error) => of({ status: 'error', error } as Vm)),
        );
      }),
    ),
    { initialValue: { status: 'loading' } as Vm },
  );

  // Expediente cargado (para el panel de datos); null mientras carga o si falla.
  readonly expediente = computed<ExpedienteResponse | null>(() => {
    const v = this.vm();
    return v.status === 'ok' ? v.expediente : null;
  });

  readonly secciones = computed(() =>
    this.vm().status === 'ok' ? (this.vm() as Extract<Vm, { status: 'ok' }>).secciones : [],
  );

  // Lista plana de documentos en orden (sección, doc) para la navegación.
  private readonly documentos = computed(() => this.secciones().flatMap((s) => s.documentos));

  // Documento elegido para la "hoja" central; por defecto el primero.
  private readonly docSeleccionadoId = signal<number | null>(null);
  readonly docActivo = computed<DocumentoRequerido | null>(() => {
    const docs = this.documentos();
    if (!docs.length) return null;
    return docs.find((d) => d.id === this.docSeleccionadoId()) ?? docs[0];
  });

  // Sección del documento activo (para resaltar el grupo en el índice).
  readonly seccionActiva = computed<SeccionEstructura | null>(() => {
    const activo = this.docActivo();
    if (!activo) return null;
    return this.secciones().find((s) => s.documentos.some((d) => d.id === activo.id)) ?? null;
  });
  readonly seccionActivaId = computed<number | null>(() => this.seccionActiva()?.id ?? null);

  // Progreso sobre documentos obligatorios (placeholder hasta que haya carga real).
  readonly obligatorios = computed(() => this.documentos().filter((d) => d.obligatorio));
  readonly obligatoriosCargados = computed(
    () => this.obligatorios().filter((d) => this.cargados().has(d.id)).length,
  );
  readonly progreso = computed(() => {
    const total = this.obligatorios().length;
    return total === 0 ? 0 : Math.round((this.obligatoriosCargados() / total) * 100);
  });
  // Hay al menos un archivo cargado: habilita la descarga del expediente.
  readonly hayDocumentos = computed(() => this.documentosCargados().length > 0);

  seleccionarDoc(id: number): void {
    this.docSeleccionadoId.set(id);
    this.archivoSeleccionadoId.set(null);
  }

  // Reabre el wizard en modo edición (la ruta ':id' lo carga como "retomar").
  editarDatos(id: number): void {
    this.router.navigate(['/expedientes', id]);
  }

  // Descarga y subida de archivos para slot de documento.
  onArchivoSeleccionado(docReqId: number, event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.subiendo.set(docReqId);
    this.documentoService.subir(this.expedienteId(), docReqId, file).subscribe({
      next: () => {
        this.subiendo.set(null);
        input.value = '';
        this.recargar();
      },
      error: (err: HttpErrorResponse) => {
        this.subiendo.set(null);
        input.value = '';
        // Sin esto la subida fallaba en silencio y parecía que "no pasaba nada".
        const mensaje = err.error?.mensaje ?? 'No se pudo subir el documento';
        this.snackBar.open(mensaje, 'Cerrar', { duration: 4000 });
      },
    });
  }

  descargar(doc: DocumentoCargado): void {
    this.documentoService
      .descargar(this.expedienteId(), doc.id)
      .subscribe((resp) => this.guardarBlob(resp, doc.nombreOriginal));
  }

  eliminar(doc: DocumentoCargado): void {
    this.documentoService.eliminar(this.expedienteId(), doc.id).subscribe(() => this.recargar());
  }

  // Despacha al endpoint según el código del slot (el flag `generable` decide si la
  // ranura tiene PDF del sistema; el código, cuál de ellos).
  private pdfGenerado(codigo: string): Observable<HttpResponse<Blob>> | null {
    switch (codigo) {
      case 'CONTRATO_LOCACION':
        return this.documentoService.descargarContrato(this.expedienteId());
      case 'CARATULA':
        return this.documentoService.descargarCaratula(this.expedienteId());
      default:
        return null;
    }
  }

  private nombreGenerado(codigo: string): string {
    return codigo === 'CARATULA' ? 'caratula.pdf' : 'contrato-locacion.pdf';
  }

  // Descarga el PDF del sistema: si está en caché baja ese mismo, si no lo pide.
  descargarGenerada(): void {
    const doc = this.docActivo();
    if (!doc) return;
    const enCache = this.previewsGeneradas().get(doc.id);
    if (enCache) {
      this.descargarBlob(enCache, this.nombreGenerado(doc.codigo));
      return;
    }
    this.pdfGenerado(doc.codigo)?.subscribe((resp) =>
      this.guardarBlob(resp, this.nombreGenerado(doc.codigo)),
    );
  }

  private guardarBlob(resp: HttpResponse<Blob>, fallback: string): void {
    const cd = resp.headers.get('content-disposition') ?? '';
    const nombre = /filename="?([^"]+)"?/.exec(cd)?.[1] ?? fallback;
    this.descargarBlob(resp.body!, nombre);
  }

  private descargarBlob(blob: Blob, nombre: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = nombre;
    a.click();
    URL.revokeObjectURL(url);
  }

  // Descarga un PDF compilado con todos los documentos cargados.
  compilar(): void {
    if (this.generando()) return;
    this.generando.set(true);
    this.documentoService.descargarCompilado(this.expedienteId()).subscribe({
      next: (resp) => {
        this.guardarBlob(resp, `expediente-${this.expedienteId()}.pdf`);
        this.generando.set(false);
      },
      error: () => {
        this.generando.set(false); /* snackbar de error */
      },
    });
  }

  // ── Pago del arancel (SCRUM-182) ─────────────────────────────────────────
  // Un solo botón abre el diálogo, que resuelve "pagar ahora" vs "compartir link".
  abrirPago(): void {
    const vm = this.vm();
    this.dialog.open(PagarArancelDialog, {
      width: '480px',
      data: {
        expedienteId: this.expedienteId(),
        expedienteNombre: vm.status === 'ok' ? vm.expediente.nombre : undefined,
        comitenteEmail: vm.status === 'ok' ? (vm.expediente.comitenteEmail ?? undefined) : undefined,
      },
    });
    // Pagar saca al usuario de la app (o abre MP en otra pestaña). Al volver,
    // el foco dispara la resincronización que actualiza el chip.
    this.esperandoPago = true;
  }

  /** Hubo un intento de pago: justifica reintentar al recuperar el foco. */
  private esperandoPago = false;

  // El pago se confirma por webhook, que es asíncrono: cuando el usuario vuelve
  // de MP el estado puede tardar unos segundos en estar acreditado. Se reintenta
  // un puñado de veces en vez de una sola, y se corta apenas queda pagado.
  private static readonly PAGO_RESYNC_MS = 2500;
  private static readonly PAGO_RESYNC_MAX = 4;

  private observarVueltaDePago(): void {
    const visible$ = fromEvent(document, 'visibilitychange').pipe(
      filter(() => document.visibilityState === 'visible'),
    );
    // Refresco barato al volver a la PESTAÑA: se escucha solo 'visibilitychange'
    // (cambio de pestaña real), NUNCA el 'focus' de window. Al cerrar el diálogo
    // nativo de "elegir archivo", el window recupera el foco y dispararía este
    // refresco: recargar() reinicia el vm a 'loading' y desmonta el <input file>
    // justo antes de que llegue su evento 'change', y la subida se perdía en
    // silencio (ni se ejecutaba el handler). visibilitychange NO se dispara por
    // el diálogo de archivo, así que es seguro.
    visible$
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.esperandoPago) this.recargar();
      });
    // Resync del pago tras volver de MP (foco de window o pestaña). Solo actúa
    // mientras se espera la acreditación, para que el foco del diálogo de archivo
    // no gatille nada cuando no hay un pago en curso.
    merge(fromEvent(window, 'focus'), visible$)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.resincronizarPago());
  }

  private resincronizarPago(): void {
    if (!this.esperandoPago) return;
    timer(0, ExpedienteArmado.PAGO_RESYNC_MS)
      .pipe(take(ExpedienteArmado.PAGO_RESYNC_MAX), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.expediente()?.estadoArancel !== 'NINGUNO') {
          this.esperandoPago = false; // ya se reflejó: no seguir insistiendo
          return;
        }
        // No alcanza con recargar: si el webhook se perdió, el backend tampoco
        // sabe del pago. Se le pide que reconcilie contra MP y recién ahí se
        // refresca. Si falla (red, MP caído), se reintenta en el próximo tick.
        this.expedienteService
          .sincronizarPago(this.expedienteId())
          .pipe(catchError(() => of(null)))
          .subscribe((r) => {
            if (r && r.estadoArancel !== 'NINGUNO') {
              this.esperandoPago = false;
            }
            this.recargar();
          });
      });
  }

  // ── Validación visual con IA (nivel 3) ──────────────────────────────────
  // Disparo on-demand del análisis y polling del estado efímero (en memoria del
  // backend). Al completar, se recarga el panel para que aparezcan las observaciones.
  private static readonly IA_POLL_MS = 3000;
  private static readonly IA_POLL_MAX = 100; // ~5 min; el análisis corre en background, no apura al usuario

  // Estado por ranura (slot): cada ranura se dispara y se sigue de forma independiente.
  // Antes la clave era la sección; analizar de a una ranura tarda segundos en vez de
  // minutos, y permite seguir dos ranuras distintas a la vez.
  private readonly iaEstados = signal<Map<number, EstadoIA>>(new Map());
  private readonly iaDetalles = signal<Map<number, string>>(new Map());
  private readonly iaPolls = new Map<number, Subscription>();

  // Estado/detalle de la ranura activa (la que se está mirando en el panel).
  readonly iaEstadoActivo = computed<EstadoIA>(() => {
    const id = this.docActivo()?.id;
    return id != null ? (this.iaEstados().get(id) ?? 'IDLE') : 'IDLE';
  });
  readonly iaDetalleActivo = computed<string>(() => {
    const id = this.docActivo()?.id;
    return id != null ? (this.iaDetalles().get(id) ?? '') : '';
  });

  analizarRanura(docReqId: number): void {
    if (this.iaEstados().get(docReqId) === 'EN_PROGRESO') return; // guarda de doble disparo (par del back)
    this.setEstadoIa(docReqId, 'EN_PROGRESO');
    this.documentoService.analizarIa(this.expedienteId(), docReqId).subscribe({
      next: () => this.pollRanura(docReqId),
      // 409 = ya hay un análisis en curso para esa ranura: nos enganchamos al polling igual.
      error: (e: HttpErrorResponse) =>
        e.status === 409 ? this.pollRanura(docReqId) : this.setEstadoIa(docReqId, 'ERROR'),
    });
  }

  private pollRanura(docReqId: number): void {
    this.iaPolls.get(docReqId)?.unsubscribe();
    const sub = timer(ExpedienteArmado.IA_POLL_MS, ExpedienteArmado.IA_POLL_MS)
      .pipe(
        switchMap(() => this.documentoService.estadoIa(this.expedienteId(), docReqId)),
        take(ExpedienteArmado.IA_POLL_MAX),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (r) => {
          if (r.estado === 'COMPLETADO') this.finRanura(docReqId, 'COMPLETADO');
          else if (r.estado === 'COMPLETADO_CON_ERRORES')
            this.finRanura(docReqId, 'COMPLETADO_CON_ERRORES', r.detalle);
          else if (r.estado === 'ERROR') this.finRanura(docReqId, 'ERROR', r.detalle);
          // EN_PROGRESO / SIN_INICIAR → seguir esperando
        },
        complete: () => {
          if (this.iaEstados().get(docReqId) === 'EN_PROGRESO')
            this.finRanura(docReqId, 'TIMEOUT');
        },
      });
    this.iaPolls.set(docReqId, sub);
  }

  private finRanura(
    docReqId: number,
    estado: 'COMPLETADO' | 'COMPLETADO_CON_ERRORES' | 'ERROR' | 'TIMEOUT',
    detalle?: string,
  ): void {
    this.iaPolls.get(docReqId)?.unsubscribe();
    this.iaPolls.delete(docReqId);
    this.setEstadoIa(docReqId, estado, detalle ?? '');
    // Los "completados" refrescan el panel: aparecen las obs de los docs que sí se analizaron.
    if (estado === 'COMPLETADO' || estado === 'COMPLETADO_CON_ERRORES') this.recargar();
  }

  private setEstadoIa(docReqId: number, estado: EstadoIA, detalle = ''): void {
    this.iaEstados.update((m) => new Map(m).set(docReqId, estado));
    this.iaDetalles.update((m) => new Map(m).set(docReqId, detalle));
  }

  readonly ORIGENES: { id: OrigenObservacion; label: string; icon: string }[] = [
    { id: 'DETERMINISTICO', label: 'Automática', icon: 'rule' },
    { id: 'COHERENCIA', label: 'Coherencia', icon: 'fact_check' },
    { id: 'IA_VISUAL', label: 'IA visual', icon: 'auto_awesome' },
  ];

  readonly filtroOrigen = signal<Set<OrigenObservacion>>(
    new Set(['DETERMINISTICO', 'COHERENCIA', 'IA_VISUAL']),
  );

  toggleOrigen(o: OrigenObservacion): void {
    const s = new Set(this.filtroOrigen());
    s.has(o) ? s.delete(o) : s.add(o);
    this.filtroOrigen.set(s);
  }
  origenInfo(o: OrigenObservacion) {
    return this.ORIGENES.find((x) => x.id === o) ?? this.ORIGENES[0];
  }

  private readonly pasaFiltro = (obs: Observacion) => this.filtroOrigen().has(obs.origen);

  readonly generalesFiltradas = computed(() => this.generales().filter(this.pasaFiltro));
  readonly documentosConObsFiltrados = computed(() =>
    this.validacion()
      .documentos.map((d) => ({ ...d, observaciones: d.observaciones.filter(this.pasaFiltro) }))
      .filter((d) => d.observaciones.length > 0),
  );
}
