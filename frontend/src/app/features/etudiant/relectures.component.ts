import { Component, OnDestroy, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, RelectureRelecteur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { ErreurComponent } from '../../shared/erreur.component';

/** #101 : les listes se rafraîchissent seules, une relecture ou une note peut arriver pendant que la page est ouverte. */
export const RAFRAICHISSEMENT_MS = 15_000;
const NOTE_MIN = 0;
const NOTE_MAX = 20;

/**
 * SF-8 / SF-9 : le relecteur est un étudiant (cahier §2) ; ses relectures à faire et rendues sont dans son espace.
 * Envoi définitif après confirmation (RG10) ; l'auteur n'est jamais affiché (HYP-10).
 */
@Component({
  selector: 'app-relectures',
  standalone: true,
  imports: [FormsModule, ErreurComponent],
  template: `
    <section class="carte">
      <div class="ligne" style="justify-content: space-between">
        <h2>Mes relectures à faire</h2>
        <button type="button" class="secondaire" (click)="charger()">Actualiser</button>
      </div>
      @for (r of relectures(); track r.id) {
        <form class="carte" (ngSubmit)="rendre(r)">
          <p><strong>{{ r.sessionTitre }}</strong> — <a [href]="r.lien" target="_blank" rel="noopener">ouvrir l'exercice</a></p>
          <div class="ligne">
            <label>Note /20 <input name="note" type="number" min="0" max="20" step="1" [(ngModel)]="notes[r.id]" required /></label>
            <label style="flex:1">Commentaire <textarea name="commentaire" [(ngModel)]="commentaires[r.id]" required></textarea></label>
          </div>
          <button type="submit" [disabled]="!peutEnvoyer(r.id)">Envoyer (définitif)</button>
        </form>
      } @empty { <p class="discret">Aucune relecture en attente.</p> }
      @if (message()) { <p class="succes" role="status">{{ message() }}</p> }
      <app-erreur [erreur]="erreur()" />
    </section>

    <section class="carte">
      <h2>Relectures rendues</h2>
      @for (r of rendues(); track r.id) {
        <p class="ligne">
          <strong>{{ r.sessionTitre }}</strong>
          <span>{{ r.note }} / 20</span>
          <span class="discret">« {{ r.commentaire }} »</span>
        </p>
      } @empty { <p class="discret">Aucune relecture rendue.</p> }
    </section>
  `,
})
export class RelecturesComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private minuterie?: ReturnType<typeof setInterval>;

  readonly etudiantId = input.required<number>();
  readonly relectures = signal<RelectureRelecteur[]>([]);
  readonly rendues = signal<RelectureRelecteur[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);
  readonly message = signal('');

  /** Un champ number vidé vaut null (Angular) : il ne doit pas activer l'envoi (#101). */
  notes: Partial<Record<number, number | null>> = {};
  commentaires: Partial<Record<number, string>> = {};

  ngOnInit(): void {
    this.charger();
    this.minuterie = setInterval(() => this.charger(), RAFRAICHISSEMENT_MS);
  }

  ngOnDestroy(): void {
    clearInterval(this.minuterie);
  }

  /** Aide à la saisie seulement : la règle RG9 est vérifiée par le serveur (NOTE_INVALIDE). */
  peutEnvoyer(id: number): boolean {
    const note = this.notes[id];
    return typeof note === 'number' && Number.isInteger(note) && note >= NOTE_MIN && note <= NOTE_MAX
      && (this.commentaires[id] ?? '').trim().length > 0;
  }

  rendre(r: RelectureRelecteur): void {
    const note = this.notes[r.id];
    if (typeof note !== 'number' || !confirm('Envoi définitif : la note ne pourra plus être modifiée. Continuer ?')) {
      return;
    }
    this.erreur.set(null);
    this.api.rendreRelecture(r.id, this.etudiantId(), note, (this.commentaires[r.id] ?? '').trim()).subscribe({
      next: () => { this.message.set('Relecture envoyée.'); this.charger(); },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  charger(): void {
    this.api.relectures(this.etudiantId(), 'A_FAIRE').subscribe({
      next: l => this.relectures.set(l),
      error: (e: ErreurApi) => this.erreur.set(e),
    });
    this.api.relectures(this.etudiantId(), 'RENDUE').subscribe({
      next: l => this.rendues.set(l),
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }
}
