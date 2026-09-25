import { HttpErrorResponse, HttpInterceptorFn, HttpStatusCode } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';

/** Session expirée ou déconnectée ailleurs : tout 401 renvoie à l'écran de connexion (SF-16). */
export const sessionInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return next(req).pipe(catchError((e: HttpErrorResponse) => {
    const verificationSilencieuse = req.url.endsWith('/moi') || req.url.endsWith('/auth/login');
    if (e.status === HttpStatusCode.Unauthorized && !verificationSilencieuse) {
      auth.oublier();
      void router.navigate(['/connexion']);
    }
    return throwError(() => e);
  }));
};
