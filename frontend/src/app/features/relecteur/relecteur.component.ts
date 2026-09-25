import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, Profil, RelectureRelecteur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { ErreurComponent } from '../../shared/erreur.component';

/**
 * SF-8 / SF-9 : le relecteur voit ses relectures à faire (liste protégée : connexion v2) et rend
 * une note définitive (RG10) après confirmation. L'auteur n'est jamais affiché (HYP-10).
 */
@Component({
  selector: 'app-relecteur',
  standalone: true,
  imports: [FormsModule, ErreurComponent],
  template: `
    <main>
      @if (profil(); as moi) {
        <p class="ligne">Connecté : {{ moi.nomAffiche }} <button class="secondaire" (click)="deconnecter()">Se déconnecter</button></p>
        <section class="carte">
          <h2>Relectures à faire</h2>
          @if (relectures().length === 0) { <p class="discret">Aucune relecture en attente.</p> }
          @for (r of relectures(); track r.id) {
            <form class="carte" (ngSubmit)="rendre(r, moi)">
              <p><strong>{{ r.sessionTitre }}</strong> — <a [href]="r.lien" target="_blank" rel="noopener">ouvrir l'exercice</a></p>
              <div class="ligne">
                <label>Note /20 <input name="note" type="number" min="0" max="20" step="1" [(ngModel)]="notes[r.id]" required /></label>
                <label style="flex:1">Commentaire <textarea name="commentaire" [(ngModel)]="commentaires[r.id]" required></textarea></label>
              </div>
              <button type="submit" [disabled]="notes[r.id] === undefined || !commentaires[r.id]?.trim()">Envoyer (définitif)</button>
            </form>
          }
          @if (message()) { <p class="succes" role="status">{{ message() }}</p> }
          <app-erreur [erreur]="erreur()" />
        </section>
      } @else {
        <section class="carte">
          <h2>Connexion du relecteur</h2>
          <form class="ligne" (ngSubmit)="connecter()">
            <label>Identifiant <input name="login" [(ngModel)]="login" required autocomplete="username" /></label>
            <label>Mot de passe <input name="mdp" type="password" [(ngModel)]="motDePasse" required autocomplete="current-password" /></label>
            <button type="submit" [disabled]="!login || !motDePasse">Se connecter</button>
          </form>
          <app-erreur [erreur]="erreur()" />
        </section>
      }
    </main>
  `,
})
export class RelecteurComponent {
  private readonly api = inject(ApiService);

  readonly profil = signal<Profil | null>(null);
  readonly relectures = signal<RelectureRelecteur[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);
  readonly message = signal('');

  login = '';
  motDePasse = '';
  notes: Record<number, number> = {};
  commentaires: Record<number, string> = {};

  connecter(): void {
    this.erreur.set(null);
    this.api.connecter(this.login, this.motDePasse).subscribe({
      next: p => {
        this.motDePasse = '';
        if (p.role !== 'ETUDIANT' || p.etudiantId === null) {
          this.erreur.set({ code: 'ACCES_REFUSE', message: 'Cet écran est réservé aux étudiants relecteurs.' });
          return;
        }
        this.profil.set(p);
        this.charger(p.etudiantId);
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  deconnecter(): void {
    this.api.deconnecter().subscribe({ complete: () => this.reinitialiser(), error: () => this.reinitialiser() });
  }

  rendre(r: RelectureRelecteur, moi: Profil): void {
    if (moi.etudiantId === null || !confirm('Envoi définitif : la note ne pourra plus être modifiée. Continuer ?')) {
      return;
    }
    const etudiantId = moi.etudiantId;
    this.erreur.set(null);
    this.api.rendreRelecture(r.id, etudiantId, this.notes[r.id], this.commentaires[r.id].trim()).subscribe({
      next: () => { this.message.set('Relecture envoyée.'); this.charger(etudiantId); },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  private charger(etudiantId: number): void {
    this.api.relectures(etudiantId, 'A_FAIRE').subscribe({
      next: l => this.relectures.set(l),
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  private reinitialiser(): void {
    this.profil.set(null);
    this.relectures.set([]);
    this.message.set('');
  }
}
