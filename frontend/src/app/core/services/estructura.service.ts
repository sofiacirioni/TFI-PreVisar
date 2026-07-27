import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs/internal/Observable';
import {
  DocumentoRequerido,
  DocumentoRequeridoRequest,
  EstructuraExpediente,
} from '../models/estructura.model';
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

  /**
   * Estructura para el armado de un expediente propio: incluye las ranuras retiradas
   * de la estructura vigente que este expediente ya tiene cargadas (desactivado = true).
   */
  getEstructuraExpediente(expedienteId: number): Observable<EstructuraExpediente> {
    return this.http.get<EstructuraExpediente>(API.ESTRUCTURA_EXPEDIENTE(expedienteId));
  }

  /** Estructura de la provincia del revisor autenticado (pantalla de configuración). */
  getEstructuraMia(tipoTareaId: number): Observable<EstructuraExpediente> {
    const params = new HttpParams().set('tipoTareaId', tipoTareaId);
    return this.http.get<EstructuraExpediente>(API.ESTRUCTURA_MIA, { params });
  }

  crearDocumento(
    seccionId: number,
    body: DocumentoRequeridoRequest,
  ): Observable<DocumentoRequerido> {
    return this.http.post<DocumentoRequerido>(API.ESTRUCTURA_SECCION_DOCUMENTOS(seccionId), body);
  }

  actualizarDocumento(
    documentoId: number,
    body: DocumentoRequeridoRequest,
  ): Observable<DocumentoRequerido> {
    return this.http.put<DocumentoRequerido>(API.ESTRUCTURA_DOCUMENTO(documentoId), body);
  }

  /** Baja lógica (soft delete): el backend nunca borra el archivo físico. */
  eliminarDocumento(documentoId: number): Observable<void> {
    return this.http.delete<void>(API.ESTRUCTURA_DOCUMENTO(documentoId));
  }
}
