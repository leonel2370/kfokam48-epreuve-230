import { DecimalPipe } from '@angular/common';
import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, ExerciceAuteur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { ErreurComponent } from '../../shared/erreur.component';

/**
 * SF-14 / #88 : l'étudiant voit la note retenue de ses exercices (moyenne des deux pairs, RG16 v3),
 * marquée « provisoire » tant qu'un seul a rendu (RG31). Route protégée : connexion demandée.
 */
@Component({
  selector: 'app-mes-notes',
  standalone: true,
  imports: [FormsModule, DecimalPipe, ErreurComponent],
  template: `
    <section class="carte">
      <h2>Mes notes</h2>
      @if (exercices(); as liste) {
        @if (liste.length === 0) { <p class="discret">Aucun exercice déposé.</p> }
        @for (x of liste; track x.id) {
          <div class="ligne">
            <strong>{{ x.sessionTitre }}</strong>
            @if (x.noteRetenue === null) {
              <span class="discret">en attente de relecture</span>
            } @else {
              <span>{{ x.noteRetenue | number: '1.0-2' }} / 20</span>
              @if (x.provisoire) { <span class="badge EN_ATTENTE_RELECTURE">provisoire</span> }
              @else { <span class="badge RELU">définitive</span> }
            }
          </div>
          @for (c of x.commentaires; track $index) { <p class="discret">« {{ c }} »</p> }
        }
      } @else {
        <form class="ligne" (ngSubmit)="connecter()">
          <label>Identifiant <input name="login" [(ngModel)]="login" required autocomplete="username" /></label>
          <label>Mot de passe
            <input name="mdp" type="password" [(ngModel)]="motDePasse" required autocomplete="current-password" />
          </label>
          <button type="submit" [disabled]="!login || !motDePasse">Voir mes notes</button>
        </form>
      }
      <app-erreur [erreur]="erreur()" />
    </section>
  `,
})
export class MesNotesComponent {
  private readonly api = inject(ApiService);

  readonly etudiantId = input.required<number>();
  readonly exercices = signal<ExerciceAuteur[] | null>(null);
  readonly erreur = signal<ErreurApi | null>(null);

  login = '';
  motDePasse = '';

  connecter(): void {
    this.erreur.set(null);
    this.api.connecter(this.login, this.motDePasse).subscribe({
      next: () => {
        this.motDePasse = '';
        this.api.mesExercices(this.etudiantId()).subscribe({
          next: l => this.exercices.set(l),
          error: (e: ErreurApi) => this.erreur.set(e),
        });
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }
}
