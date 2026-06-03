import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { forkJoin } from 'rxjs';

import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { ComitenteService } from '../../../core/services/comitente.service';
import { ObraService } from '../../../core/services/obra.service';
import { Comitente, Obra } from '../../../core/models';

@Component({
  selector: 'app-comitente-detail',
  imports: [
    CommonModule,
    DatePipe,
    MatCardModule,
    MatButtonModule,
    MatIconModule,
    MatChipsModule,
    MatDividerModule,
    MatTableModule,
    MatTooltipModule,
    MatProgressSpinnerModule,
  ],
  templateUrl: './comitente-detail.html',
  styleUrl: './comitente-detail.scss',
})
export class ComitenteDetail implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly comitenteService = inject(ComitenteService);
  private readonly obraService = inject(ObraService);

  // Estado UI
  readonly cargando = signal(true);
  readonly errorCarga = signal<string | null>(null);

  // Datos
  readonly comitente = signal<Comitente | null>(null);
  readonly obras = signal<Obra[]>([]);

  // Obras ordenadas por fecha de creación descendente (más recientes arriba)
  readonly obrasOrdenadas = computed(() =>
    [...this.obras()].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
    )
  );

  readonly columnasObras = ['designacion', 'localidad', 'fecha', 'acciones'];

  ngOnInit(): void {
    const idParam = this.route.snapshot.paramMap.get('id');
    const id = Number(idParam);
    if (!idParam || !Number.isFinite(id)) {
      this.errorCarga.set('El identificador del comitente no es válido.');
      this.cargando.set(false);
      return;
    }
    this.cargarTodo(id);
  }

  private cargarTodo(id: number): void {
    this.cargando.set(true);
    this.errorCarga.set(null);

    forkJoin({
      comitente: this.comitenteService.obtenerPorId(id),
      obras: this.obraService.listarPorComitente(id),
    }).subscribe({
      next: ({ comitente, obras }) => {
        this.comitente.set(comitente);
        this.obras.set(obras);
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

  irAEditar(): void {
    const c = this.comitente();
    if (!c) return;
    this.router.navigate(['/comitentes', c.id, 'editar']);
  }

  volver(): void {
    this.router.navigate(['/comitentes']);
  }

  nuevaObra(): void {
    const c = this.comitente();
    if (!c) return;
    // Pasamos el comitenteId como query param. Si el obra-form aún no lo lee,
    // el usuario simplemente selecciona el comitente en el select.
    this.router.navigate(['/obras/nueva'], { queryParams: { comitenteId: c.id } });
  }

  verObra(obra: Obra): void {
    this.router.navigate(['/obras', obra.id, 'editar']);
  }
}
