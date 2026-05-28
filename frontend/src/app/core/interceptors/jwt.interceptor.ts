import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '@core/services/token.service';
import { environment } from '@env/environment.development';

/**
 * Agrega el header Authorization: Bearer <token> a las requests dirigidas
 * a nuestra API. No toca requests a otros dominios (ej. catálogos externos).
 */
export const jwtInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenService = inject(TokenService);
  const token = tokenService.getToken();

  // Solo adjuntamos el token a requests a nuestro backend
  const esRequestALaApi =
    req.url.startsWith(environment.apiBaseUrl) ||
    req.url.startsWith(environment.authBaseUrl);

  if (token && esRequestALaApi) {
    const reqConToken = req.clone({
      setHeaders: {
        Authorization: `Bearer ${token}`,
      },
    });
    return next(reqConToken);
  }

  return next(req);
};
