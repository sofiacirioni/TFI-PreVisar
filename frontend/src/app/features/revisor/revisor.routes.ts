import { Routes } from '@angular/router';

export const REVISOR_ROUTES: Routes = [
  // '/revisar' → historial + subida de un expediente completo.
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./revision-list/revision-list').then((m) => m.RevisionList),
  },
  // '/revisar/:id' → resumen del expediente analizado.
  {
    path: ':id',
    loadComponent: () =>
      import('./revision-detalle/revision-detalle').then((m) => m.RevisionDetalle),
  },
];
