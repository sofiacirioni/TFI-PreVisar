import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { ProfesionalService } from '@core/services/profesional.service';
import { of } from 'rxjs/internal/observable/of';
import { map } from 'rxjs/internal/operators/map';
import { catchError } from 'rxjs/internal/operators/catchError';

/**
 * Permite el acceso solo a profesionales con rol de revisor.
 * Usa el perfil cacheado si está disponible; si no, lo carga del backend.
 * Cualquier no-revisor (o fallo de carga) se redirige al dashboard.
 */
export const revisorGuard: CanActivateFn = () => {
  const profesionalService = inject(ProfesionalService);
  const router = inject(Router);

  const perfilCacheado = profesionalService.perfilActual();
  const perfil$ = perfilCacheado
    ? of(perfilCacheado)
    : profesionalService.cargarPerfil();

  return perfil$.pipe(
    map((perfil) =>
      perfil.esRevisor ? true : router.createUrlTree(['/dashboard'])
    ),
    catchError(() => of(router.createUrlTree(['/dashboard'])))
  );
};
