import { Routes } from '@angular/router';

export const OBRAS_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./obra-list/obra-list').then((m) => m.ObraList),
  },
  // Más adelante: 'nueva', ':id/editar', etc.
];