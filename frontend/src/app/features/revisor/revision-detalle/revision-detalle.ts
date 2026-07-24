import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DestroyRef } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { timer } from 'rxjs/internal/observable/timer';
import { switchMap } from 'rxjs/internal/operators/switchMap';
import { takeWhile } from 'rxjs/internal/operators/takeWhile';
import { RevisionExternaService } from '../../../core/services/revision-externa.service';
import { RevisionDetalle as RevisionDetalleModel } from '../../../core/models/revision-externa.model';

/** Cada cuánto se reconsulta el detalle mientras el análisis está EN_PROGRESO. */
const POLL_MS = 3000;

@Component({
  selector: 'app-revision-detalle',
  imports: [
    CommonModule,
    MatButtonModule,
    MatIconModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './revision-detalle.html',
  styleUrl: './revision-detalle.scss',
})
export class RevisionDetalle implements OnInit {
  private readonly service = inject(RevisionExternaService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  private readonly id = Number(this.route.snapshot.paramMap.get('id'));

  readonly revision = signal<RevisionDetalleModel | null>(null);
  readonly errorCarga = signal<string | null>(null);

  readonly enProgreso = computed(() => this.revision()?.estado === 'EN_PROGRESO');
  readonly conError = computed(() => this.revision()?.estado === 'ERROR');
  readonly resumen = computed(() => this.revision()?.resultado ?? null);

  ngOnInit(): void {
    if (!this.id) {
      this.router.navigate(['/revisar']);
      return;
    }
    // Poll hasta que el análisis deje de estar EN_PROGRESO (takeWhile inclusivo:
    // emite también el estado final que corta el polling).
    timer(0, POLL_MS)
      .pipe(
        switchMap(() => this.service.obtener(this.id)),
        takeWhile((r) => r.estado === 'EN_PROGRESO', true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (r) => this.revision.set(r),
        error: (err: HttpErrorResponse) => {
          if (err.status === 404) {
            this.router.navigate(['/revisar']);
          } else {
            this.errorCarga.set('No se pudo cargar el resumen. Intentá recargar.');
          }
        },
      });
  }

  volver(): void {
    this.router.navigate(['/revisar']);
  }
}
