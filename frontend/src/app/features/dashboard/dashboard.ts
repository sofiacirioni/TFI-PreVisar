import { CommonModule, DatePipe } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { MatIconModule } from '@angular/material/icon';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RouterLink } from '@angular/router';
import { ProfesionalService } from '../../core/services/profesional.service';
import { ExpedienteService } from '../../core/services/expediente.service';
import { ExpedienteResponse, rutaRetomarExpediente } from '../../core/models/expediente.model';

const MAX_EXPEDIENTES_RECIENTES = 4;

/** Meses que muestra el gráfico de actividad, incluido el actual. */
const MESES_ACTIVIDAD = 6;

/**
 * Debajo de este total no se dibujan gráficos: con uno o dos expedientes una
 * barra no dice nada y una "frecuencia" es ruido. Se muestran igual las
 * tarjetas, que sí tienen sentido desde el primer registro.
 */
const MINIMO_PARA_GRAFICOS = 3;

/** Una porción de una distribución, ya normalizada para dibujarla. */
export interface Porcion {
  etiqueta: string;
  cantidad: number;
  /** 0–100, relativo al valor más alto del conjunto. */
  porcentaje: number;
}

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

  /**
   * Lista completa. Antes se guardaba solo el recorte de recientes, pero las
   * métricas se calculan sobre todos los expedientes y ya vienen en la misma
   * respuesta: sirve tenerla entera para no pedir nada extra.
   */
  private readonly expedientes = signal<ExpedienteResponse[]>([]);
  readonly cargandoExpedientes = signal(true);

  // El back ya devuelve ordenado por updatedAt desc.
  readonly expedientesRecientes = computed(() =>
    this.expedientes().slice(0, MAX_EXPEDIENTES_RECIENTES),
  );

  // ── Métricas ────────────────────────────────────────────────────────────
  // Todo se deriva de la lista que ya está en memoria: sin requests nuevos.

  readonly totalExpedientes = computed(() => this.expedientes().length);

  readonly hayMetricas = computed(() => this.totalExpedientes() > 0);

  /** Con pocos expedientes un gráfico engaña más de lo que informa. */
  readonly hayDatosParaGraficos = computed(
    () => this.totalExpedientes() >= MINIMO_PARA_GRAFICOS,
  );

  /** Borradores sin terminar: es el dato accionable, no una estadística. */
  readonly borradores = computed(
    () => this.expedientes().filter((e) => e.estado === 'BORRADOR').length,
  );

  /**
   * Comitente que más veces aparece. Null si ninguno se repite: con una sola
   * aparición "el más frecuente" no significa nada y el dato confunde.
   */
  readonly comitenteFrecuente = computed<{ nombre: string; cantidad: number } | null>(() => {
    const conteo = this.contarPor(this.expedientes(), (e) => e.comitenteNombre);
    const top = [...conteo.entries()].sort((a, b) => b[1] - a[1])[0];
    return top && top[1] > 1 ? { nombre: top[0], cantidad: top[1] } : null;
  });

  /** Distribución por tipo de tarea, de mayor a menor. */
  readonly porTipoTarea = computed<Porcion[]>(() => {
    const conteo = this.contarPor(this.expedientes(), (e) => e.tipoTareaNombre);
    return this.aPorciones([...conteo.entries()].sort((a, b) => b[1] - a[1]));
  });

  /** Expedientes creados en cada uno de los últimos meses. */
  readonly porMes = computed<Porcion[]>(() => {
    const hoy = new Date();
    const baldes: { clave: string; etiqueta: string; cantidad: number }[] = [];

    for (let i = MESES_ACTIVIDAD - 1; i >= 0; i--) {
      const d = new Date(hoy.getFullYear(), hoy.getMonth() - i, 1);
      baldes.push({
        clave: `${d.getFullYear()}-${d.getMonth()}`,
        etiqueta: d.toLocaleDateString('es-AR', { month: 'short' }).replace('.', ''),
        cantidad: 0,
      });
    }

    const indice = new Map(baldes.map((b, i) => [b.clave, i]));
    for (const e of this.expedientes()) {
      const d = new Date(e.createdAt);
      if (Number.isNaN(d.getTime())) continue;
      const i = indice.get(`${d.getFullYear()}-${d.getMonth()}`);
      if (i !== undefined) baldes[i].cantidad++;
    }

    return this.aPorciones(baldes.map((b) => [b.etiqueta, b.cantidad]));
  });

  /** Cuenta ocurrencias de una propiedad, ignorando las vacías. */
  private contarPor(
    lista: ExpedienteResponse[],
    obtener: (e: ExpedienteResponse) => string | null,
  ): Map<string, number> {
    const conteo = new Map<string, number>();
    for (const e of lista) {
      const clave = obtener(e);
      if (!clave) continue;
      conteo.set(clave, (conteo.get(clave) ?? 0) + 1);
    }
    return conteo;
  }

  /** Normaliza contra el valor más alto, que es lo que define el alto de barra. */
  private aPorciones(pares: [string, number][]): Porcion[] {
    const max = Math.max(...pares.map(([, n]) => n), 0);
    return pares.map(([etiqueta, cantidad]) => ({
      etiqueta,
      cantidad,
      porcentaje: max > 0 ? Math.round((cantidad / max) * 100) : 0,
    }));
  }

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
        this.expedientes.set(lista);
        this.cargandoExpedientes.set(false);
      },
      error: () => {
        // El dashboard no debe romperse si falla esta query: dejamos lista vacía
        this.expedientes.set([]);
        this.cargandoExpedientes.set(false);
      },
    });
  }
}
