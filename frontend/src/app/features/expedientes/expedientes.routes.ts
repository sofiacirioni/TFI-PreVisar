import { Routes } from '@angular/router';

export const EXPEDIENTES_ROUTES: Routes = [
  // '/expedientes' → listado de expedientes del usuario.
  {
    path: '',
    pathMatch: 'full',
    loadComponent: () =>
      import('./expediente-list/expediente-list').then((m) => m.ExpedienteList),
  },
  {
    path: 'nuevo',
    loadComponent: () =>
      import('./expediente-wizard/expediente-wizard').then((m) => m.ExpedienteWizard),
  },
  {
    path: ':id',
    loadComponent: () =>
      import('./expediente-wizard/expediente-wizard').then((m) => m.ExpedienteWizard),
  },
];
