import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ErreurApi } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { ErreurComponent } from '../../shared/erreur/erreur.component';

const LONGUEUR_MIN = 8;

/** SF-17 et SF-18 : profil de l'utilisateur connecté et changement de mot de passe (obligatoire si RG23). */
@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [FormsModule, ErreurComponent],
  templateUrl: './profil.component.html',
  styleUrl: './profil.component.scss',
})
export class ProfilComponent {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  readonly auth = inject(AuthService);

  readonly longueurMin = LONGUEUR_MIN;
  readonly erreur = signal<ErreurApi | null>(null);
  readonly ok = signal(false);
  ancien = '';
  nouveau = '';

  /** La règle de longueur est vérifiée par le serveur (MOT_DE_PASSE_TROP_FAIBLE) ; ici, seulement le bouton. */
  changer(): void {
    const obligatoire = this.auth.profil()?.doitChangerMotDePasse ?? false;
    this.erreur.set(null);
    this.ok.set(false);
    this.api.changerMotDePasse(this.ancien, this.nouveau).subscribe({
      next: () => {
        this.ancien = '';
        this.nouveau = '';
        this.ok.set(true);
        this.auth.rafraichir().subscribe(p => {
          if (obligatoire && p) {
            void this.router.navigateByUrl(AuthService.espace(p));
          }
        });
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }
}
