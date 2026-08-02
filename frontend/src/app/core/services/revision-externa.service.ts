import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { API } from '../constants/api.constants';
import {
  RevisionDetalle,
  RevisionMetricas,
  RevisionResumen,
} from '../models/revision-externa.model';

/**
 * Funciones del rol revisor: subir un expediente completo (PDF) para análisis
 * con IA y consultar el historial de revisiones.
 */
@Injectable({
  providedIn: 'root',
})
export class RevisionExternaService {
  private readonly http = inject(HttpClient);

  /** Sube el PDF. El backend responde 202 con el id y analiza en segundo plano. */
  crear(archivo: File): Observable<{ id: number }> {
    const form = new FormData();
    form.append('archivo', archivo);
    return this.http.post<{ id: number }>(API.REVISOR_REVISIONES, form);
  }

  /** Historial del revisor, más nueva primero. */
  listar(): Observable<RevisionResumen[]> {
    return this.http.get<RevisionResumen[]>(API.REVISOR_REVISIONES);
  }

  /**
   * Métricas agregadas del historial. Van por endpoint aparte porque los
   * problemas detectados están dentro del `resultado` de cada revisión y el
   * listado no lo trae.
   */
  metricas(): Observable<RevisionMetricas> {
    return this.http.get<RevisionMetricas>(API.REVISOR_METRICAS);
  }

  /** Detalle con el resumen. El front lo consulta hasta que estado deje de ser EN_PROGRESO. */
  obtener(id: number): Observable<RevisionDetalle> {
    return this.http.get<RevisionDetalle>(API.REVISOR_REVISION_BY_ID(id));
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(API.REVISOR_REVISION_BY_ID(id));
  }
}
