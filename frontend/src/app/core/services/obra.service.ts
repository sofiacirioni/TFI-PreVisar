import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Obra, ObraRequest } from '../models';
import { Observable } from 'rxjs/internal/Observable';
import { API } from '../constants/api.constants';

@Injectable({
  providedIn: 'root',
})
export class ObraService {
  private readonly http = inject(HttpClient);

  /** Lista TODAS las obras del profesional autenticado, de todos sus comitentes. */
  listarTodas(): Observable<Obra[]> {
    return this.http.get<Obra[]>(API.OBRAS);
  }

  /** Lista las obras de un comitente específico. */
  listarPorComitente(comitenteId: number): Observable<Obra[]> {
    return this.http.get<Obra[]>(API.COMITENTE_OBRAS(comitenteId));
  }

  obtenerPorId(id: number): Observable<Obra> {
    return this.http.get<Obra>(API.OBRA_BY_ID(id));
  }

  /**
   * Crear obra. El endpoint del backend está anidado bajo comitente:
   * POST /api/comitentes/{comitenteId}/obras
   */
  crear(comitenteId: number, request: ObraRequest): Observable<Obra> {
    return this.http.post<Obra>(API.COMITENTE_OBRAS(comitenteId), request);
  }

  /** Actualizar usa el endpoint standalone PUT /api/obras/{id}. */
  actualizar(id: number, request: ObraRequest): Observable<Obra> {
    return this.http.put<Obra>(API.OBRA_BY_ID(id), request);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(API.OBRA_BY_ID(id));
  }
}
