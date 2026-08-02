import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Comitente, ComitenteRequest } from '../models';
import { Observable } from 'rxjs';
import { API } from '../constants/api.constants';

@Injectable({
  providedIn: 'root',
})
export class ComitenteService {
  private readonly http = inject(HttpClient);

  /** Lista todos los comitentes del profesional autenticado. */
  listar(): Observable<Comitente[]> {
    return this.http.get<Comitente[]>(API.COMITENTES);
  }

  /** Obtiene un comitente por ID. */
  obtenerPorId(id: number): Observable<Comitente> {
    return this.http.get<Comitente>(API.COMITENTE_BY_ID(id));
  }

  /**
   * Búsqueda incremental por DNI/CUIT. El backend ignora guiones y demás
   * separadores, y devuelve lista vacía con menos de 3 dígitos.
   */
  buscarIncremental(fragmento: string): Observable<Comitente[]> {
    const params = new HttpParams().set('q', fragmento);
    return this.http.get<Comitente[]>(API.COMITENTE_BUSCAR_INCREMENTAL, { params });
  }

  crear(request: ComitenteRequest): Observable<Comitente> {
    return this.http.post<Comitente>(API.COMITENTES, request);
  }

  actualizar(id: number, request: ComitenteRequest): Observable<Comitente> {
    return this.http.put<Comitente>(API.COMITENTE_BY_ID(id), request);
  }

  /** Soft delete (el backend marca deleted_at). */
  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(API.COMITENTE_BY_ID(id));
  }
  
}
