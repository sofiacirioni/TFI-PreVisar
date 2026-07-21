import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { ExpedienteRequest, ExpedienteResponse } from '../models/expediente.model';
import { Observable } from 'rxjs/internal/Observable';
import { API } from '../constants/api.constants';
import { AportesResponse, CalcularAportesRequest } from '../models/aportes.model';
import { EstadoArancelResponse, PreferenciaPago } from '../models/pago.model';

@Injectable({
  providedIn: 'root',
})
export class ExpedienteService {
  private readonly http = inject(HttpClient);

  crear(request: ExpedienteRequest): Observable<ExpedienteResponse> {
    return this.http.post<ExpedienteResponse>(API.EXPEDIENTES, request);
  }

  actualizarParcial(id: number, request: ExpedienteRequest): Observable<ExpedienteResponse> {
    return this.http.patch<ExpedienteResponse>(API.EXPEDIENTE_BY_ID(id), request);
  }

  completar(id: number): Observable<ExpedienteResponse> {
    return this.http.post<ExpedienteResponse>(API.EXPEDIENTE_COMPLETAR(id), {});
  }

  obtener(id: number): Observable<ExpedienteResponse> {
    return this.http.get<ExpedienteResponse>(API.EXPEDIENTE_BY_ID(id));
  }

  listarMisExpedientes(): Observable<ExpedienteResponse[]> {
    return this.http.get<ExpedienteResponse[]>(API.EXPEDIENTES);
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(API.EXPEDIENTE_BY_ID(id));
  }

  calcularAportes(request: CalcularAportesRequest): Observable<AportesResponse> {
    return this.http.post<AportesResponse>(API.EXPEDIENTES_CALCULAR_APORTES, request);
  }

  /** Crea la preferencia de pago del arancel en MP y devuelve el initPoint para redirigir al checkout. */
  /**
   * Reconcilia el arancel contra MP. Red de seguridad del webhook: se llama al
   * volver del pago, así el estado se refleja aunque la notificación se pierda.
   */
  sincronizarPago(id: number): Observable<EstadoArancelResponse> {
    return this.http.post<EstadoArancelResponse>(API.EXPEDIENTE_PAGO_SYNC(id), {});
  }

  iniciarPago(id: number): Observable<PreferenciaPago> {
    return this.http.post<PreferenciaPago>(API.EXPEDIENTE_PAGO(id), {});
  }
}
