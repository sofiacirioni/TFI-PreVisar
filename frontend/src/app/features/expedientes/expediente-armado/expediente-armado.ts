import { CurrencyPipe, DecimalPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toObservable, toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, startWith, switchMap } from 'rxjs/operators';
import { combineLatest, Observable, of } from 'rxjs';
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
import { GenerarContratoRequest } from '../../../core/models/generar-contrato-request.model';
import { HttpResponse } from '@angular/common/http';
import { FormsModule } from '@angular/forms';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatExpansionModule } from '@angular/material/expansion';
import { NgxExtendedPdfViewerModule } from 'ngx-extended-pdf-viewer';

type Vm =
  | { status: 'loading' }
  | { status: 'error'; error: unknown }
  | { status: 'ok'; expediente: ExpedienteResponse; secciones: SeccionEstructura[] };

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

  readonly expedienteId = toSignal(this.route.paramMap.pipe(map((pm) => Number(pm.get('id')))), {
    initialValue: 0,
  });

  private readonly refresh = signal(0);
  recargar(): void {
    this.refresh.update((n) => n + 1);
  }

  readonly documentosCargados = toSignal(
    combineLatest([this.route.paramMap, toObservable(this.refresh)]).pipe(
      switchMap(([pm]) =>
        this.documentoService.listar(Number(pm.get('id'))).pipe(catchError(() => of([]))),
      ),
    ),
    { initialValue: [] as DocumentoCargado[] },
  );

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

  // Contrato generado para previsualizar (Opción A: se regenera en el backend
  // con los campos del panel lateral). Es efímero: se limpia al cambiar de doc.
  private readonly contratoPreviewBlob = signal<Blob | null>(null);
  readonly generandoContrato = signal(false);
  readonly esContrato = computed(() => this.docActivo()?.codigo === 'CONTRATO_LOCACION');

  // Fuente del visor central: el contrato generado tiene prioridad cuando el
  // documento activo es el contrato; si no, el archivo subido del slot.
  readonly viewerSrc = computed<Blob | null>(() => {
    if (this.esContrato() && this.contratoPreviewBlob()) return this.contratoPreviewBlob();
    return this.previewBlob();
  });

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

  // Carga encadenada: expediente -> (tipoTareaId, provinciaId) -> estructura.
  // Sin <Vm> explícito: con un único type-arg se descartan los overloads que
  // aceptan initialValue y vm quedaría Signal<Vm | undefined>.
  readonly vm = toSignal(
    this.route.paramMap.pipe(
      map((pm) => Number(pm.get('id'))),
      switchMap((id) =>
        this.expedienteService.obtener(id).pipe(
          switchMap((exp) => {
            // Sin tipo de tarea o sin provincia no hay estructura que pedir.
            const estructura$: Observable<EstructuraExpediente> =
              exp.tipoTareaId == null || exp.provinciaId == null
                ? of({ tipoTareaId: 0, tipoTareaCodigo: '', secciones: [] })
                : this.estructuraService.getEstructura(exp.tipoTareaId, exp.provinciaId);
            return estructura$.pipe(
              map((est): Vm => ({ status: 'ok', expediente: exp, secciones: est.secciones })),
            );
          }),
          startWith({ status: 'loading' } as Vm),
          catchError((error) => of({ status: 'error', error } as Vm)),
        ),
      ),
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
    this.contratoPreviewBlob.set(null); // preview de contrato es por-documento
  }

  // Reabre el wizard en modo edición (la ruta ':id' lo carga como "retomar").
  editarDatos(id: number): void {
    this.router.navigate(['/expedientes', id]);
  }

  // El expediente ya existe en esta vista; el botón del header descarga todos
  // los documentos cargados (uno por archivo). TODO: endpoint backend que
  // devuelva un único ZIP en vez de N descargas.
  descargarExpediente(): void {
    for (const doc of this.documentosCargados()) {
      this.descargar(doc);
    }
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
      error: () => {
        this.subiendo.set(null);
        input.value = ''; /* snackbar de error */
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

  private contratoReq(): GenerarContratoRequest {
    return {
      honorariosPactados: this.honorariosPactados(),
      documentacionConfeccion: this.documentacionConfeccion() || null,
      tareasEspeciales: this.tareasEspeciales() || null,
      formaPago: this.formaPago() || null,
      plazoEntrega: this.plazoEntrega() || null,
      gastosEspeciales: this.gastosEspeciales() || null,
    };
  }

  // Regenera el contrato con los campos actuales y lo muestra en el visor central.
  actualizarPreviewContrato(): void {
    this.generandoContrato.set(true);
    this.documentoService.descargarContrato(this.expedienteId(), this.contratoReq()).subscribe({
      next: (resp) => {
        this.contratoPreviewBlob.set(resp.body);
        this.generandoContrato.set(false);
      },
      error: () => this.generandoContrato.set(false),
    });
  }

  // Descarga el contrato. Si ya hay una preview generada, baja ese mismo PDF;
  // si no, lo genera con los campos actuales.
  descargarContrato(): void {
    const blob = this.contratoPreviewBlob();
    if (blob) {
      this.descargarBlob(blob, 'contrato-locacion.pdf');
      return;
    }
    this.documentoService
      .descargarContrato(this.expedienteId(), this.contratoReq())
      .subscribe((resp) => this.guardarBlob(resp, 'contrato-locacion.pdf'));
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
}
