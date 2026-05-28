import { Routes } from '@angular/router';

export const COMITENTES_ROUTES: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./comitente-list/comitente-list').then((m) => m.ComitenteList),
  },
  // Más adelante: 'nuevo', ':id/editar', etc.
];