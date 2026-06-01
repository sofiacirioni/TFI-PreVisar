import { inject, Injectable, signal } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { tap } from 'rxjs/internal/operators/tap';
import { HttpClient } from '@angular/common/http';
import { API } from '../constants/api.constants';
import { Profesional, ProfesionalUpdateRequest } from '../models/profesional.model';

@Injectable({
  providedIn: 'root',
})
export class ProfesionalService {
  private readonly http = inject(HttpClient);

  // Signal con el perfil actual cargado (cache simple).
  // null = no cargado todavía o sin sesión.
  private readonly _perfilActual = signal<Profesional | null>(null);
  readonly perfilActual = this._perfilActual.asReadonly();

  /** Carga el perfil desde el backend y lo cachea en el signal. */
  cargarPerfil(): Observable<Profesional> {
    return this.http.get<Profesional>(API.PROFESIONAL_ME).pipe(
      tap((perfil) => this._perfilActual.set(perfil))
    );
  }

  /** Actualiza el perfil y refresca el cache. */
  actualizarPerfil(datos: ProfesionalUpdateRequest): Observable<Profesional> {
    return this.http.put<Profesional>(API.PROFESIONAL_ME, datos).pipe(
      tap((perfil) => this._perfilActual.set(perfil))
    );
  }

  /** Limpia el cache (al logout). */
  limpiar(): void {
    this._perfilActual.set(null);
  }
}
