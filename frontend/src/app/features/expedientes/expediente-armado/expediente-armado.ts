import { CurrencyPipe } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { catchError, map, startWith, switchMap } from 'rxjs/operators';
import { Observable, of } from 'rxjs';
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

type Vm =
  | { status: 'loading' }
  | { status: 'error'; error: unknown }
  | { status: 'ok'; expediente: ExpedienteResponse; secciones: SeccionEstructura[] };

@Component({
  selector: 'app-expediente-armado',
  imports: [
    CurrencyPipe,
    RouterLink,
    MatProgressBarModule,
    MatIconModule,
    MatChipsModule,
    MatButtonModule,
  ],
  templateUrl: './expediente-armado.html',
  styleUrl: './expediente-armado.scss',
})
export class ExpedienteArmado {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly expedienteService = inject(ExpedienteService);
  private readonly estructuraService = inject(EstructuraService);

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

  // Documentos ya cargados al expediente. Hoy vacío; SCRUM-140 lo va a poblar.
  readonly cargados = signal<ReadonlySet<number>>(new Set());

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
  readonly seccionActivaId = computed<number | null>(() => {
    const activo = this.docActivo();
    if (!activo) return null;
    return this.secciones().find((s) => s.documentos.some((d) => d.id === activo.id))?.id ?? null;
  });

  // Progreso sobre documentos obligatorios (placeholder hasta que haya carga real).
  readonly obligatorios = computed(() => this.documentos().filter((d) => d.obligatorio));
  readonly obligatoriosCargados = computed(
    () => this.obligatorios().filter((d) => this.cargados().has(d.id)).length,
  );
  readonly progreso = computed(() => {
    const total = this.obligatorios().length;
    return total === 0 ? 0 : Math.round((this.obligatoriosCargados() / total) * 100);
  });
  readonly puedeGenerar = computed(
    () =>
      this.obligatorios().length > 0 &&
      this.obligatoriosCargados() === this.obligatorios().length,
  );

  seleccionarDoc(id: number): void {
    this.docSeleccionadoId.set(id);
  }

  // Reabre el wizard en modo edición (la ruta ':id' lo carga como "retomar").
  editarDatos(id: number): void {
    this.router.navigate(['/expedientes', id]);
  }

  generar(): void {
    // Enganche para SCRUM-11 (generación de documentos administrativos).
    // Deshabilitado hasta completar los documentos obligatorios.
  }
}
