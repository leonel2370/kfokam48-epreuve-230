import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { AuthService } from './core/auth/auth.service';

/** En-tête v2 : nom de la personne connectée, les menus de son rôle uniquement, profil et déconnexion. */
@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.component.html',
  styleUrl: './app.component.scss',
})
export class AppComponent {
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);
  readonly titre = 'Présence48';

  /** Menus visibles selon le rôle (matrice des droits, cahier §2 bis). */
  readonly menus = computed(() => {
    switch (this.auth.profil()?.role) {
      case 'ADMIN': return [{ lien: '/admin', libelle: 'Administration' }, { lien: '/formateur', libelle: 'Sessions' }];
      case 'FORMATEUR': return [{ lien: '/formateur', libelle: 'Mes sessions' }];
      case 'ETUDIANT': return [{ lien: '/etudiant', libelle: 'Mon espace' }];
      default: return [];
    }
  });

  deconnecter(): void {
    this.auth.deconnecter().subscribe(() => void this.router.navigate(['/connexion']));
  }
}
