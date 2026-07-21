import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { ProfesionalService } from '../../core/services/profesional.service';
import { ExpedienteService } from '../../core/services/expediente.service';
import { ExpedienteResponse, rutaRetomarExpediente } from '../../core/models/expediente.model';

const MAX_EXPEDIENTES_RECIENTES = 4;

@Component({
  selector: 'app-dashboard',
  imports: [
    CommonModule,
    DatePipe,
    RouterLink,
    MatIconModule,
    MatTooltipModule,
  ],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class Dashboard implements OnInit {
  private readonly profesionalService = inject(ProfesionalService);
  private readonly expedienteService = inject(ExpedienteService);

  readonly perfil = this.profesionalService.perfilActual;

  // Listado de los más recientes (el back ya devuelve ordenado por updatedAt desc)
  readonly expedientesRecientes = signal<ExpedienteResponse[]>([]);
  readonly cargandoExpedientes = signal(true);

  /** Retoma donde quedó: armado si ya está EN_PROCESO, wizard si sigue en BORRADOR. */
  readonly rutaRetomar = rutaRetomarExpediente;

  readonly saludo = computed(() => {
    const hora = new Date().getHours();
    if (hora < 12) return 'Buen día';
    if (hora < 19) return 'Buenas tardes';
    return 'Buenas noches';
  });

  ngOnInit(): void {
    this.expedienteService.listarMisExpedientes().subscribe({
      next: (lista) => {
        this.expedientesRecientes.set(lista.slice(0, MAX_EXPEDIENTES_RECIENTES));
        this.cargandoExpedientes.set(false);
      },
      error: () => {
        // El dashboard no debe romperse si falla esta query: dejamos lista vacía
        this.expedientesRecientes.set([]);
        this.cargandoExpedientes.set(false);
      },
    });
  }
}
