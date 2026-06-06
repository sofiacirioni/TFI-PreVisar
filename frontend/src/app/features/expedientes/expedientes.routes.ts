import { Routes } from '@angular/router';

export const EXPEDIENTES_ROUTES: Routes = [
  // Por ahora no hay listado de expedientes; mientras tanto, '/expedientes'
  // arranca el wizard en modo "nuevo". Cuando exista ExpedienteList,
  // reemplazar este redirect por su loadComponent.
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'nuevo',
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
