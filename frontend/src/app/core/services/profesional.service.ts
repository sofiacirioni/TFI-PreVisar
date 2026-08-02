import { inject, Injectable, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';
import { HttpClient } from '@angular/common/http';
import { API } from '../constants/api.constants';
import { Profesional, ProfesionalUpdateRequest } from '../models/profesional.model';
import { BajaCuentaRequest, CambiarPasswordRequest } from '../models';

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

  /** Cambia la contraseña del profesional. No devuelve datos, solo éxito o error. */
  cambiarPassword(request: CambiarPasswordRequest): Observable<void> {
    return this.http.post<void>(API.PROFESIONAL_CAMBIAR_PASSWORD, request);
  }

  /** Da de baja la cuenta (soft-delete). Requiere la contraseña como confirmación. */
  darDeBaja(request: BajaCuentaRequest): Observable<void> {
    return this.http.post<void>(API.PROFESIONAL_BAJA, request);
  }

  /**
   * Solicita el rol de revisor: el backend notifica a la institución por correo.
   * El mensaje es opcional. El alta efectiva del rol es manual.
   */
  solicitarRolRevisor(mensaje?: string): Observable<void> {
    return this.http.post<void>(API.PROFESIONAL_SOLICITAR_REVISOR, { mensaje });
  }

  /** Limpia el cache (al logout). */
  limpiar(): void {
    this._perfilActual.set(null);
  }
}
