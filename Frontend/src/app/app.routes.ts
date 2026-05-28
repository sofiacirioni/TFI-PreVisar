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

  // --- Zona pública (solo si NO estás logueado) ---
  {
    path: 'auth',
    canActivate: [guestGuard],
    loadChildren: () =>
      import('@features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },

  // --- Zona privada (requiere sesión) ---
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@features/dashboard/dashboard').then((m) => m.Dashboard),
  },
  {
    path: 'profile',
    canActivate: [authGuard],
    loadComponent: () =>
      import('@features/profile/profile').then((m) => m.Profile),
  },
  {
    path: 'comitentes',
    canActivate: [authGuard],
    loadChildren: () =>
      import('@features/comitentes/comitentes.routes').then((m) => m.COMITENTES_ROUTES),
  },
  {
    path: 'obras',
    canActivate: [authGuard],
    loadChildren: () =>
      import('@features/obras/obras.routes').then((m) => m.OBRAS_ROUTES),
  },

  // --- Fallback ---
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
