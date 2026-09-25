import { Component, computed, inject } from '@angular/core';
import { Router, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';
import { CHEMINS, EntreeNavigation } from './core/navigation/chemins';
import { BarreNavigationComponent } from './shared/barre-navigation/barre-navigation.component';
import { BoutonNavigationComponent } from './shared/bouton-navigation/bouton-navigation.component';

/** En-tête v3 (#104) : menus du rôle, profil et déconnexion, tous en boutons ; aucun lien texte. */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, BarreNavigationComponent, BoutonNavigationComponent],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  readonly titre = 'Présence48';
  readonly chemins = CHEMINS;

  /** Menus visibles selon le rôle (matrice des droits, cahier §2 bis). */
  readonly menus = computed<EntreeNavigation[]>(() => {
    switch (this.auth.profil()?.role) {
      case 'ADMIN':
        return [{ chemin: CHEMINS.admin, libelle: 'Administration' }, { chemin: CHEMINS.formateur, libelle: 'Sessions' }];
      case 'FORMATEUR':
        return [{ chemin: CHEMINS.formateur, libelle: 'Mes sessions' }];
      case 'ETUDIANT':
        return [{ chemin: CHEMINS.etudiant, libelle: 'Mon espace' }];
      default:
        return [];
    }
  });

  deconnecter(): void {
    this.auth.deconnecter().subscribe(() => void this.router.navigate([CHEMINS.connexion]));
  }
}
