import { Injectable, signal } from '@angular/core';

/** Identité choisie à l'écran (SF-1, Q1), mémorisée dans le navigateur. Déclarative : aucune authentification. */
export interface Identite {
  promotionId: number;
  etudiantId: number;
  nom: string;
}

const CLE = 'presence48.identite';

@Injectable({ providedIn: 'root' })
export class IdentiteService {
  readonly identite = signal<Identite | null>(IdentiteService.lire());

  choisir(identite: Identite): void {
    localStorage.setItem(CLE, JSON.stringify(identite));
    this.identite.set(identite);
  }

  oublier(): void {
    localStorage.removeItem(CLE);
    this.identite.set(null);
  }

  private static lire(): Identite | null {
    try {
      const brut = localStorage.getItem(CLE);
      return brut ? (JSON.parse(brut) as Identite) : null;
    } catch {
      return null;
    }
  }
}
