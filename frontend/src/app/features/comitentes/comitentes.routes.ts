import { Routes } from '@angular/router';

export const COMITENTES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./comitente-list/comitente-list').then((m) => m.ComitenteList),
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./comitente-form/comitente-form').then((m) => m.ComitenteForm),
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('./comitente-form/comitente-form').then((m) => m.ComitenteForm),
  },
];