import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
import { guestGuard } from '@core/guards/guest.guard';

export const routes: Routes = [
  // Raíz → dashboard
  {
    path: '',
    pathMatch: 'full',
    redirectTo: 'dashboard',
  },

  // ===== Zona pública (con AuthLayout) =====
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadComponent: () =>
      import('@layouts/auth-layout/auth-layout').then((m) => m.AuthLayout),
    loadChildren: () =>
      import('@features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },

  // ===== Zona privada (con MainLayout) =====
  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@layouts/main-layout/main-layout').then((m) => m.MainLayout),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('@features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('@features/profile/profile').then((m) => m.Profile),
      },
      {
        path: 'comitentes',
        loadChildren: () =>
          import('@features/comitentes/comitentes.routes').then((m) => m.COMITENTES_ROUTES),
      },
      {
        path: 'obras',
        loadChildren: () =>
          import('@features/obras/obras.routes').then((m) => m.OBRAS_ROUTES),
      },
    ],
  },

  // Fallback
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
