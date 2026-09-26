import { Component, input } from '@angular/core';
import { EntreeNavigation } from '../../core/navigation/chemins';
import { BoutonNavigationComponent } from '../bouton-navigation/bouton-navigation.component';

/** Rangée de boutons de navigation : menus de l'en-tête, onglets de l'espace étudiant (#104). */
@Component({
  selector: 'app-barre-navigation',
  standalone: true,
  imports: [BoutonNavigationComponent],
  templateUrl: './barre-navigation.component.html',
  styleUrl: './barre-navigation.component.scss',
})
export class BarreNavigationComponent {
  readonly entrees = input.required<readonly EntreeNavigation[]>();
  /** Nom annoncé par les lecteurs d'écran (aria-label). */
  readonly libelle = input.required<string>();
}
