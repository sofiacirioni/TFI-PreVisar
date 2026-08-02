import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { CondicionIva, Especialidad, Provincia, Regional, TipoTarea, Titulo } from '../models';
import { Observable } from 'rxjs';
import { API } from '../constants/api.constants';

@Injectable({
  providedIn: 'root',
})
export class CatalogoService {
  private readonly http = inject(HttpClient);

  listarProvincias(): Observable<Provincia[]> {
    return this.http.get<Provincia[]>(API.CATALOGO_PROVINCIAS);
  }

  listarRegionales(): Observable<Regional[]> {
    return this.http.get<Regional[]>(API.CATALOGO_REGIONALES);
  }

  listarCondicionesIva(): Observable<CondicionIva[]> {
    return this.http.get<CondicionIva[]>(API.CATALOGO_CONDICIONES_IVA);
  }

  listarTitulos(): Observable<Titulo[]> {
    return this.http.get<Titulo[]>(API.CATALOGO_TITULOS);
  }

  listarEspecialidades(): Observable<Especialidad[]> {
    return this.http.get<Especialidad[]>(API.CATALOGO_ESPECIALIDADES);
  }

  /** Si especialidadId viene, el back devuelve solo los tipos de esa especialidad. */
  listarTiposTarea(especialidadId?: number | null): Observable<TipoTarea[]> {
    const params = especialidadId != null
      ? new HttpParams().set('especialidadId', String(especialidadId))
      : undefined;
    return this.http.get<TipoTarea[]>(API.CATALOGO_TIPOS_TAREA, { params });
  }
}
