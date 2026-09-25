import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { ErreurApi } from '../api/api.models';

/** Toute erreur devient { code, message } (ENF3) : les écrans affichent le message du serveur tel quel. */
export const erreurInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(
    catchError((e: HttpErrorResponse) => {
      const corps = e.error as Partial<ErreurApi> | null;
      const erreur: ErreurApi = corps?.code && corps?.message
        ? { code: corps.code, message: corps.message }
        : { code: e.status === 0 ? 'SERVEUR_INJOIGNABLE' : 'ERREUR_INTERNE',
            message: e.status === 0 ? 'Le serveur est injoignable.' : 'Une erreur inattendue est survenue.' };
      return throwError(() => erreur);
    }),
  );
