import { Routes } from '@angular/router';
import { authGuard } from '@core/guards/auth.guard';
import { guestGuard } from '@core/guards/guest.guard';
import { revisorGuard } from '@core/guards/revisor.guard';

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

  // Retorno del checkout de MP (SCRUM-182). Público y sin layout: lo ve el comitente,
  // que paga desde MP y NO tiene acceso a la app. :estado = exito | pendiente | error.
  {
    path: 'pago/:estado',
    loadComponent: () =>
      import('@features/pago/pago-retorno/pago-retorno').then((m) => m.PagoRetorno),
  },

  // Términos y Condiciones. Público y sin guard: lo enlaza la casilla del registro
  // (usuario sin sesión) y también debe poder consultarse ya logueado.
  {
    path: 'terminos',
    loadComponent: () =>
      import('@features/legal/terminos/terminos').then((m) => m.Terminos),
  },

  // Política de Privacidad. Pública: forma parte de los T&C y la enlaza el registro.
  {
    path: 'privacidad',
    loadComponent: () =>
      import('@features/legal/privacidad/privacidad').then((m) => m.Privacidad),
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
        path: 'ayuda',
        loadComponent: () =>
          import('@features/ayuda/ayuda').then((m) => m.Ayuda),
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
      {
        path: 'expedientes',
        loadChildren: () =>
          import('@features/expedientes/expedientes.routes').then((m) => m.EXPEDIENTES_ROUTES),
      },
      {
        path: 'revisar',
        canActivate: [revisorGuard],
        loadChildren: () =>
          import('@features/revisor/revisor.routes').then((m) => m.REVISOR_ROUTES),
      },
      {
        path: 'admin',
        canActivate: [revisorGuard],
        loadChildren: () =>
          import('@features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
      },
    ],
  },

  // Fallback
  {
    path: '**',
    redirectTo: 'dashboard',
  },
];
