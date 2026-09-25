import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { EnvironmentProviders, Provider } from '@angular/core';
import { provideRouter } from '@angular/router';
import { Profil } from './core/api/api.models';
import { erreurInterceptor } from './core/interceptors/erreur.interceptor';

/** Fournisseurs communs aux tests : HttpClient simulé avec le vrai intercepteur d'erreurs, routeur vide. */
export const FOURNISSEURS_TEST: (Provider | EnvironmentProviders)[] = [
  provideHttpClient(withInterceptors([erreurInterceptor])),
  provideHttpClientTesting(),
  provideRouter([]),
];

/** Profils de démonstration (V3). */
export const PROFILS: Record<'awa' | 'formateur' | 'admin', Profil> = {
  awa: { id: 3, login: 'awa', nomAffiche: 'Awa Ndiaye', role: 'ETUDIANT', etudiantId: 1, promotionIds: [1],
    doitChangerMotDePasse: false },
  formateur: { id: 2, login: 'formateur', nomAffiche: 'Jean Fokam', role: 'FORMATEUR', etudiantId: null,
    promotionIds: [1], doitChangerMotDePasse: false },
  admin: { id: 1, login: 'admin', nomAffiche: 'Administrateur', role: 'ADMIN', etudiantId: null, promotionIds: [1, 2],
    doitChangerMotDePasse: true },
};
