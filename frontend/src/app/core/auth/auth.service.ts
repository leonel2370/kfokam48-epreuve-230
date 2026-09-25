import { Injectable, inject, signal } from '@angular/core';
import { Observable, catchError, map, of, tap } from 'rxjs';
import { Profil, Role } from '../api/api.models';
import { ApiService } from '../api/api.service';
import { CHEMINS } from '../navigation/chemins';

/** Espace d'arrivée de chaque rôle après connexion (spécifications §1.2). */
const ESPACES: Record<Role, string> = {
  ADMIN: CHEMINS.admin,
  FORMATEUR: CHEMINS.formateur,
  ETUDIANT: CHEMINS.etudiant,
};

/**
 * SF-15 à SF-17 : qui est connecté. La session vit côté serveur (cookie HttpOnly) : le frontend ne garde
 * aucun jeton, il redemande /api/moi au chargement de la page.
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);

  readonly profil = signal<Profil | null>(null);
  private verifie = false;

  /** Profil courant, en interrogeant le serveur une seule fois par chargement de page. */
  charger(): Observable<Profil | null> {
    if (this.verifie) {
      return of(this.profil());
    }
    return this.api.moi().pipe(
      catchError(() => of(null)),
      tap(p => { this.profil.set(p); this.verifie = true; }),
    );
  }

  connecter(login: string, motDePasse: string): Observable<Profil> {
    return this.api.connecter(login, motDePasse).pipe(tap(p => { this.profil.set(p); this.verifie = true; }));
  }

  /** Après un changement de mot de passe : relit le profil (doitChangerMotDePasse repasse à false). */
  rafraichir(): Observable<Profil | null> {
    this.verifie = false;
    return this.charger();
  }

  deconnecter(): Observable<void> {
    return this.api.deconnecter().pipe(
      catchError(() => of(undefined)),
      map(() => undefined),
      tap(() => this.oublier()),
    );
  }

  /** Session perdue (401) ou déconnexion. */
  oublier(): void {
    this.profil.set(null);
    this.verifie = true;
  }

  /** Où envoyer l'utilisateur : changement de mot de passe d'abord (RG23), puis l'espace de son rôle. */
  static espace(p: Profil): string {
    return p.doitChangerMotDePasse ? CHEMINS.profil : ESPACES[p.role];
  }
}
