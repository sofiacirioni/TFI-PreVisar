import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import { EstructuraExpediente } from '../models/estructura.model';
import { HttpClient, HttpParams } from '@angular/common/http';
import { API } from '../constants/api.constants';

@Injectable({
  providedIn: 'root',
})
export class EstructuraService {
  private readonly http = inject(HttpClient);

  getEstructura(tipoTareaId: number, provinciaId: number): Observable<EstructuraExpediente> {
    const params = new HttpParams()
      .set('tipoTareaId', tipoTareaId)
      .set('provinciaId', provinciaId);
    return this.http.get<EstructuraExpediente>(API.ESTRUCTURA, { params });
  }
}
