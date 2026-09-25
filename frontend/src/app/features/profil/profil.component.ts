import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ErreurApi } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { ErreurComponent } from '../../shared/erreur.component';

const LONGUEUR_MIN = 8;

/** SF-17 et SF-18 : profil de l'utilisateur connecté et changement de mot de passe (obligatoire si RG23). */
@Component({
  selector: 'app-profil',
  standalone: true,
  imports: [FormsModule, ErreurComponent],
  styles: [':host main { max-width: 28rem; }'],
  template: `
    <main>
      @if (auth.profil(); as moi) {
        @if (moi.doitChangerMotDePasse) {
          <p class="alerte" role="alert">Premier accès : choisissez un nouveau mot de passe pour continuer.</p>
        }
        <section class="carte">
          <h2>Mon profil</h2>
          <p><strong>{{ moi.nomAffiche }}</strong> · {{ moi.role }} · identifiant <code>{{ moi.login }}</code></p>
        </section>
        <section class="carte">
          <h2>Changer mon mot de passe</h2>
          <form (ngSubmit)="changer()" style="display:grid; gap:.75rem">
            <label>Mot de passe actuel
              <input name="ancien" type="password" [(ngModel)]="ancien" required autocomplete="current-password" />
            </label>
            <label>Nouveau mot de passe (8 caractères minimum)
              <input name="nouveau" type="password" [(ngModel)]="nouveau" required minlength="8" autocomplete="new-password" />
            </label>
            <button type="submit" [disabled]="!ancien || nouveau.length < longueurMin">Enregistrer</button>
          </form>
          @if (ok()) { <p class="succes" role="status">Mot de passe changé.</p> }
          <app-erreur [erreur]="erreur()" />
        </section>
      }
    </main>
  `,
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
