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
  templateUrl: './connexion.component.html',
  styleUrl: './connexion.component.scss',
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
