import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { TokenService } from '@core/services/token.service';
// Sin alias de entorno: '@env/environment' es el archivo que `fileReplacements`
// sustituye por environment.development.ts en los builds de desarrollo. Importar
// environment.development directamente saltea ese reemplazo y en el build de
// producción deja el guard comparando contra http://localhost:8080, con lo que
// NINGUNA request lleva el header Authorization.
import { environment } from '@env/environment';

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
