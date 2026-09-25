import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { map } from 'rxjs';
import { Role } from '../api/api.models';
import { CHEMINS } from '../navigation/chemins';
import { AuthService } from './auth.service';

/**
 * SF-19 côté écran : non connecté → /connexion ; mot de passe à changer → /profil (RG23) ;
 * rôle non autorisé → l'espace de son propre rôle. Le serveur reste seul juge (401/403).
 */
export function accesGuard(...roles: Role[]): CanActivateFn {
  return (_route, state) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    return auth.charger().pipe(map(p => {
      if (!p) {
        return router.createUrlTree([CHEMINS.connexion]);
      }
      if (p.doitChangerMotDePasse && state.url !== CHEMINS.profil) {
        return router.createUrlTree([CHEMINS.profil]);
      }
      if (roles.length > 0 && !roles.includes(p.role)) {
        return router.createUrlTree([AuthService.espace(p)]);
      }
      return true;
    }));
  };
}

/** Page d'accueil « / » et page de connexion : un utilisateur déjà connecté va directement dans son espace. */
export const redirectionGuard: CanActivateFn = (_route, state) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  return auth.charger().pipe(map(p => {
    if (p) {
      return router.createUrlTree([AuthService.espace(p)]);
    }
    return state.url === CHEMINS.connexion ? true : router.createUrlTree([CHEMINS.connexion]);
  }));
};
