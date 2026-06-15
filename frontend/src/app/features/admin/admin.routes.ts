import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  {
    path: 'parametros',
    loadComponent: () =>
      import('./gestion-parametros/gestion-parametros').then((m) => m.GestionParametros),
  },
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'parametros',
  },
];
