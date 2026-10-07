import { WritableSignal, signal } from '@angular/core';
import { Observable } from 'rxjs';
import { ErreurApi } from '../api/api.models';

/**
 * Une écriture à la fois (#137) : tant qu'elle est en cours, les boutons d'action sont inactifs, ce qui
 * évite le double envoi. L'erreur va dans la zone que l'appelant désigne.
 */
export class Ecriture {
  readonly enCours = signal(false);

  lancer<T>(requete: Observable<T>, erreur: WritableSignal<ErreurApi | null>, apres: (reponse: T) => void): void {
    if (this.enCours()) {
      return;
    }
    this.enCours.set(true);
    erreur.set(null);
    requete.subscribe({
      next: reponse => {
        this.enCours.set(false);
        apres(reponse);
      },
      error: (e: ErreurApi) => {
        this.enCours.set(false);
        erreur.set(e);
      },
    });
  }
}
