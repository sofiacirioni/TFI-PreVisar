import { Routes } from '@angular/router';

export const OBRAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./obra-list/obra-list').then((m) => m.ObraList),
  },
  {
    path: 'nueva',
    loadComponent: () =>
      import('./obra-form/obra-form').then((m) => m.ObraForm),
  },
  {
    path: ':id/editar',
    loadComponent: () =>
      import('./obra-form/obra-form').then((m) => m.ObraForm),
  },
];