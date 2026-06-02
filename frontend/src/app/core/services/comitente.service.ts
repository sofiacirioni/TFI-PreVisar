import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Comitente, ComitenteRequest } from '../models';
import { Observable } from 'rxjs/internal/Observable';
import { API } from '../constants/api.constants';
import { catchError } from 'rxjs/internal/operators/catchError';
import { of, throwError } from 'rxjs';

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
   * Busca un comitente por DNI/CUIT.
   */
  buscarPorDniCuit(dniCuit: string): Observable<Comitente | null> {
    const params = new HttpParams().set('dniCuit', dniCuit);
    return this.http.get<Comitente>(API.COMITENTE_BUSCAR, { params }).pipe(
      catchError((err: HttpErrorResponse) => {
        if (err.status === 404) {
          return of(null);
        }
        return throwError(() => err);
      })
    );
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
