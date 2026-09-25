import { Component, OnDestroy, OnInit, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, RelectureRelecteur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { ErreurComponent } from '../../shared/erreur.component';

/** #101 : les listes se rafraîchissent seules, une relecture ou une note peut arriver pendant que la page est ouverte. */
export const RAFRAICHISSEMENT_MS = 15_000;
const NOTE_MIN = 0;
const NOTE_MAX = 20;

/** Note entière de 0 à 20 ; un champ number vidé vaut null (Angular). */
function noteSaisieValide(note: number | null | undefined): note is number {
  return typeof note === 'number' && Number.isInteger(note) && note >= NOTE_MIN && note <= NOTE_MAX;
}

/**
 * SF-8 / SF-9 : le relecteur est un étudiant (cahier §2) ; ses relectures à faire et rendues sont dans son espace.
 * Envoi définitif après confirmation (RG10) ; l'auteur n'est jamais affiché (HYP-10).
 */
@Component({
  selector: 'app-relectures',
  standalone: true,
  imports: [FormsModule, ErreurComponent],
  templateUrl: './relectures.component.html',
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
    return noteSaisieValide(this.notes[id]) && (this.commentaires[id] ?? '').trim().length > 0;
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
