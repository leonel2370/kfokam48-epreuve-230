import { Component, input } from '@angular/core';
import { Lecture } from '../../core/etat/lecture';
import { ErreurComponent } from '../erreur/erreur.component';

/**
 * États obligatoires d'une liste (DESIGN_SYSTEM, #137, #138) : « Chargement… » avant la première réponse,
 * le texte de liste vide quand la lecture a réussi sans élément, et l'erreur de CETTE lecture.
 */
@Component({
  selector: 'app-etat-liste',
  standalone: true,
  imports: [ErreurComponent],
  templateUrl: './etat-liste.component.html',
})
export class EtatListeComponent {
  readonly lecture = input.required<Lecture<unknown>>();
  /** Nombre d'éléments affichés par l'appelant. */
  readonly nombre = input.required<number>();
  /** Texte affiché quand la liste est vraiment vide. */
  readonly vide = input.required<string>();
}
