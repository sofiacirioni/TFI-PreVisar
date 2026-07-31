import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners, provideZoneChangeDetection } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeEsAr from '@angular/common/locales/es-AR';
import { provideRouter } from '@angular/router';

import { routes } from './app.routes';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { errorInterceptor } from '@core/interceptors/error.interceptor';
import { jwtInterceptor } from '@core/interceptors/jwt.interceptor';
import { provideLucideIcons } from '@core/icons/provide-lucide-icons';

// Sin esto Angular usa en-US: `currency: 'ARS' : 'symbol'` rendía "ARS 1,234.56"
// en vez de "$ 1.234,56", y las fechas salían en formato estadounidense.
registerLocaleData(localeEsAr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideZoneChangeDetection({ eventCoalescing: true }),
    { provide: LOCALE_ID, useValue: 'es-AR' },
    provideRouter(routes),
    provideHttpClient(
      withInterceptors([jwtInterceptor, errorInterceptor])
    ),
    provideAnimations(),
    provideLucideIcons()
  ]
};
