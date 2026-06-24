import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { DocumentoCargado } from '../models/documento-cargado.model';
import { GenerarContratoRequest } from '../models/generar-contrato-request.model';
import { Observable } from 'rxjs';
import { API } from '../constants/api.constants';

@Injectable({
  providedIn: 'root',
})
export class DocumentoService {
  private readonly http = inject(HttpClient);

  listar(expId: number): Observable<DocumentoCargado[]> {
    return this.http.get<DocumentoCargado[]>(API.EXPEDIENTE_DOCUMENTOS(expId));
  }

  subir(expId: number, documentoRequeridoId: number, archivo: File): Observable<DocumentoCargado> {
    const form = new FormData();
    form.append('archivo', archivo);
    const params = new HttpParams().set('documentoRequeridoId', documentoRequeridoId);
    return this.http.post<DocumentoCargado>(API.EXPEDIENTE_DOCUMENTOS(expId), form, { params });
  }

  eliminar(expId: number, docId: number): Observable<void> {
    return this.http.delete<void>(API.EXPEDIENTE_DOCUMENTO(expId, docId));
  }

  descargar(expId: number, docId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(API.EXPEDIENTE_DOCUMENTO(expId, docId), { responseType: 'blob', observe: 'response' });
  }

  descargarContrato(expId: number, req: GenerarContratoRequest): Observable<HttpResponse<Blob>> {
    return this.http.post(API.EXPEDIENTE_CONTRATO(expId), req, {
      responseType: 'blob',
      observe: 'response',
    });
  }

  descargarCaratula(expId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(API.EXPEDIENTE_CARATULA(expId), {
      responseType: 'blob',
      observe: 'response',
    });
  }
}
