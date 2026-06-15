import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { API } from '@core/constants/api.constants';
import { ActualizarArancelRequest, ArancelVigente } from '@core/models/aportes.model';

@Injectable({
  providedIn: 'root',
})
export class ParametroAporteService {
  private readonly http = inject(HttpClient);

  /** Valor vigente del arancel administrativo. */
  obtenerArancelVigente(): Observable<ArancelVigente> {
    return this.http.get<ArancelVigente>(API.APORTE_ARANCEL);
  }

  /** Actualiza el arancel (solo revisor). Internamente versiona, no pisa el valor. */
  actualizarArancel(request: ActualizarArancelRequest): Observable<void> {
    return this.http.put<void>(API.APORTE_ARANCEL, request);
  }
}
