import { provideHttpClient, withInterceptors, withXsrfConfiguration } from '@angular/common/http';
import { ApplicationConfig } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';

import { routes } from './app.routes';
import { erreurInterceptor } from './core/interceptors/erreur.interceptor';
import { sessionInterceptor } from './core/interceptors/session.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes, withComponentInputBinding()),
    // Ordre : sessionInterceptor voit d'abord le 401 brut, erreurInterceptor le convertit ensuite en {code, message}.
    // v2 : cookie XSRF-TOKEN recopié dans l'en-tête X-XSRF-TOKEN sur les écritures (même origine).
    provideHttpClient(withInterceptors([erreurInterceptor, sessionInterceptor]),
      withXsrfConfiguration({ cookieName: 'XSRF-TOKEN', headerName: 'X-XSRF-TOKEN' })),
  ],
};
