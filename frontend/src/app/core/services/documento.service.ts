import { inject, Injectable } from '@angular/core';
import { HttpClient, HttpParams, HttpResponse } from '@angular/common/http';
import { DocumentoCargado } from '../models/documento-cargado.model';
import { Observable } from 'rxjs';
import { API } from '../constants/api.constants';
import { ValidacionResultado } from '../models/validacion.model';

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
    return this.http.get(API.EXPEDIENTE_DOCUMENTO(expId, docId), {
      responseType: 'blob',
      observe: 'response',
    });
  }

  descargarCompilado(expId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(API.EXPEDIENTE_COMPILADO(expId), {
      responseType: 'blob',
      observe: 'response',
    });
  }

  /** El contrato se arma con los datos ya guardados en el expediente: es un GET sin body. */
  descargarContrato(expId: number): Observable<HttpResponse<Blob>> {
    return this.http.get(API.EXPEDIENTE_CONTRATO(expId), {
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

  validar(expId: number): Observable<ValidacionResultado> {
    return this.http.get<ValidacionResultado>(API.EXPEDIENTE_VALIDACION(expId));
  }

  analizarIa(expId: number, docReqId: number): Observable<void> {
    return this.http.post<void>(API.EXPEDIENTE_IA(expId, docReqId), {});
  }

  estadoIa(expId: number, docReqId: number): Observable<{ estado: string; detalle?: string }> {
    return this.http.get<{ estado: string; detalle?: string }>(
      API.EXPEDIENTE_IA_ESTADO(expId, docReqId),
    );
  }
}
