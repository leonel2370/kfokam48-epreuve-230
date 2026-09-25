import { Component, input } from '@angular/core';
import { ErreurApi } from '../core/api/api.models';

/** Affiche le message d'erreur du serveur tel quel (ENF3), avec son code pour le support. */
@Component({
  selector: 'app-erreur',
  standalone: true,
  templateUrl: './erreur.component.html',
})
export class ErreurComponent {
  readonly erreur = input<ErreurApi | null>(null);
}
