import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '@core/services/token.service';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/internal/operators/catchError';
import { throwError } from 'rxjs/internal/observable/throwError';

/**
 * Maneja errores HTTP de forma centralizada.
 * - 401: token inválido/vencido → limpia sesión y redirige a login.
 * - El resto se reenvía para que cada feature lo maneje según contexto.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401) {
        // Token vencido o inválido: la sesión ya no sirve
        tokenService.clear();
        // Evitamos loop si el 401 vino del propio login
        const esEndpointAuth = req.url.includes('/auth/');
        if (!esEndpointAuth) {
          router.navigate(['/auth/login'], {
            queryParams: { sesionExpirada: true },
          });
        }
      }
      // Reenviamos el error para que el componente que hizo la llamada lo maneje
      return throwError(() => error);
    })
  );
};
