import { inject } from '@angular/core/primitives/di';
import { CanActivateFn, Router } from '@angular/router';
import { TokenService } from '@core/services/token.service';

/**
 * Impide que un usuario YA autenticado acceda a login/register.
 * Si tiene sesión válida, lo manda al dashboard.
 */
export const guestGuard: CanActivateFn = (route, state) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  if (tokenService.hasValidToken()) {
    // Ya logueado: no tiene sentido ver login/register
    return router.createUrlTree(['/dashboard']);
  }

  return true;
};
