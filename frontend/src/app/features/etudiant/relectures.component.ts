import { Component, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, RelectureRelecteur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { ErreurComponent } from '../../shared/erreur.component';

/**
 * SF-8 / SF-9 : le relecteur est un étudiant (cahier §2) ; ses relectures à faire sont dans son espace.
 * Envoi définitif après confirmation (RG10) ; l'auteur n'est jamais affiché (HYP-10).
 */
@Component({
  selector: 'app-relectures',
  standalone: true,
  imports: [FormsModule, ErreurComponent],
  template: `
    <section class="carte">
      <h2>Mes relectures à faire</h2>
      @for (r of relectures(); track r.id) {
        <form class="carte" (ngSubmit)="rendre(r)">
          <p><strong>{{ r.sessionTitre }}</strong> — <a [href]="r.lien" target="_blank" rel="noopener">ouvrir l'exercice</a></p>
          <div class="ligne">
            <label>Note /20 <input name="note" type="number" min="0" max="20" step="1" [(ngModel)]="notes[r.id]" required /></label>
            <label style="flex:1">Commentaire <textarea name="commentaire" [(ngModel)]="commentaires[r.id]" required></textarea></label>
          </div>
          <button type="submit" [disabled]="notes[r.id] === undefined || !commentaires[r.id]?.trim()">Envoyer (définitif)</button>
        </form>
      } @empty { <p class="discret">Aucune relecture en attente.</p> }
      @if (message()) { <p class="succes" role="status">{{ message() }}</p> }
      <app-erreur [erreur]="erreur()" />
    </section>
  `,
})
export class RelecturesComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly etudiantId = input.required<number>();
  readonly relectures = signal<RelectureRelecteur[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);
  readonly message = signal('');

  notes: Record<number, number> = {};
  commentaires: Partial<Record<number, string>> = {};

  ngOnInit(): void {
    this.charger();
  }

  rendre(r: RelectureRelecteur): void {
    if (!confirm('Envoi définitif : la note ne pourra plus être modifiée. Continuer ?')) {
      return;
    }
    this.erreur.set(null);
    this.api.rendreRelecture(r.id, this.etudiantId(), this.notes[r.id], (this.commentaires[r.id] ?? '').trim()).subscribe({
      next: () => { this.message.set('Relecture envoyée.'); this.charger(); },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  charger(): void {
    this.api.relectures(this.etudiantId(), 'A_FAIRE').subscribe({
      next: l => this.relectures.set(l),
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }
}
