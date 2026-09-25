import { Component, computed, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { CHEMINS, EntreeNavigation } from '../../core/navigation/chemins';
import { BarreNavigationComponent } from '../../shared/barre-navigation/barre-navigation.component';

/** Onglets de l'espace étudiant (spécifications §1.2 bis) ; « Mes relectures » est l'écran relecteur (F2). */
export const ONGLETS_ETUDIANT: readonly EntreeNavigation[] = [
  { chemin: CHEMINS.etudiantPresence, libelle: 'Présence et dépôt' },
  { chemin: CHEMINS.etudiantNotes, libelle: 'Mes notes' },
  { chemin: CHEMINS.etudiantRelectures, libelle: 'Mes relectures' },
];

/**
 * Espace étudiant (#104) : barre d'onglets commune aux trois écrans et contrôle unique du lien compte → fiche
 * étudiant (HYP-15). Mobile d'abord (ENF1).
 */
@Component({
  selector: 'app-espace-etudiant',
  standalone: true,
  imports: [RouterOutlet, BarreNavigationComponent],
  templateUrl: './espace-etudiant.component.html',
  styleUrl: './espace-etudiant.component.scss',
})
export class EspaceEtudiantComponent {
  private readonly auth = inject(AuthService);

  readonly onglets = ONGLETS_ETUDIANT;
  readonly lie = computed(() => this.auth.profil()?.etudiantId != null);
}
