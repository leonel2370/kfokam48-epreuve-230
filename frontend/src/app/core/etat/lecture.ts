import { WritableSignal, computed, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { ErreurApi } from '../api/api.models';

/**
 * État d'une lecture du serveur (#137) : ses données, son erreur et son premier chargement.
 * L'erreur n'est effacée que par la réussite de CETTE lecture : une autre requête qui réussit ne la masque pas.
 * Tant qu'aucune réponse n'est arrivée, l'écran affiche « Chargement… » et non une liste vide.
 */
export class Lecture<T> {
  readonly donnees: WritableSignal<T>;
  readonly erreur = signal<ErreurApi | null>(null);
  private readonly recue = signal(false);

  /** Vrai quand la lecture a réussi au moins une fois : une liste vide est alors vraiment vide. */
  readonly lue = this.recue.asReadonly();
  /** Vrai tant qu'aucune réponse, réussite ou échec, n'est arrivée. */
  readonly enAttente = computed(() => !this.recue() && this.erreur() === null);

  constructor(initial: T) {
    this.donnees = signal(initial);
  }

  charger(requete: Observable<T>): void {
    requete.subscribe({
      next: valeur => {
        this.donnees.set(valeur);
        this.erreur.set(null);
        this.recue.set(true);
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }
}
