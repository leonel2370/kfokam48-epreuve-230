import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { ErreurApi } from '../../core/api/api.models';
import { AuthService } from '../../core/auth/auth.service';
import { ErreurComponent } from '../../shared/erreur.component';

/** SF-15 : seule page publique ; après connexion, chacun va dans l'espace de son rôle. */
@Component({
  selector: 'app-connexion',
  standalone: true,
  imports: [FormsModule, ErreurComponent],
  styles: [':host main { max-width: 24rem; }'],
  template: `
    <main>
      <section class="carte">
        <h2>Connexion</h2>
        <form (ngSubmit)="connecter()" style="display:grid; gap:.75rem">
          <label>Identifiant <input name="login" [(ngModel)]="login" required autocomplete="username" /></label>
          <label>Mot de passe
            <input name="mdp" type="password" [(ngModel)]="motDePasse" required autocomplete="current-password" />
          </label>
          <button type="submit" [disabled]="enCours() || !login || !motDePasse">
            {{ enCours() ? 'Connexion…' : 'Se connecter' }}
          </button>
        </form>
        <app-erreur [erreur]="erreur()" />
        <p class="discret">Comptes de démonstration : voir le README.</p>
      </section>
    </main>
  `,
})
export class ConnexionComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly erreur = signal<ErreurApi | null>(null);
  readonly enCours = signal(false);
  login = '';
  motDePasse = '';

  connecter(): void {
    this.enCours.set(true);
    this.erreur.set(null);
    this.auth.connecter(this.login.trim(), this.motDePasse).subscribe({
      next: p => { this.enCours.set(false); void this.router.navigateByUrl(AuthService.espace(p)); },
      error: (e: ErreurApi) => { this.enCours.set(false); this.motDePasse = ''; this.erreur.set(e); },
    });
  }
}
