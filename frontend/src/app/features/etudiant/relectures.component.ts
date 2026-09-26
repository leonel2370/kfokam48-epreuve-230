import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, RelectureRelecteur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { BoutonExerciceComponent } from '../../shared/bouton-exercice/bouton-exercice.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';

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
  imports: [FormsModule, ErreurComponent, BoutonExerciceComponent],
  templateUrl: './relectures.component.html',
})
export class RelecturesComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private minuterie?: ReturnType<typeof setInterval>;

  /** Écran RELECTEUR routé (F2, #104) : le relecteur est l'étudiant du compte connecté (HYP-15). */
  readonly etudiantId = computed(() => this.auth.profil()?.etudiantId ?? null);
  readonly relectures = signal<RelectureRelecteur[]>([]);
  readonly rendues = signal<RelectureRelecteur[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);
  readonly message = signal('');
  /** #106 : l'envoi en cours désactive le bouton (pas de double envoi définitif). */
  readonly envoi = signal(false);
  private minuterieMessage?: ReturnType<typeof setTimeout>;

  /** Un champ number vidé vaut null (Angular) : il ne doit pas activer l'envoi (#101). */
  notes: Partial<Record<number, number | null>> = {};
  commentaires: Partial<Record<number, string>> = {};

  ngOnInit(): void {
    this.charger();
    this.minuterie = setInterval(() => this.charger(), RAFRAICHISSEMENT_MS);
  }

  ngOnDestroy(): void {
    clearInterval(this.minuterie);
    clearTimeout(this.minuterieMessage);
  }

  /** Aide à la saisie seulement : la règle RG9 est vérifiée par le serveur (NOTE_INVALIDE). */
  peutEnvoyer(id: number): boolean {
    return noteSaisieValide(this.notes[id]) && (this.commentaires[id] ?? '').trim().length > 0;
  }

  rendre(r: RelectureRelecteur): void {
    const note = this.notes[r.id];
    const etudiantId = this.etudiantId();
    if (etudiantId === null || !noteSaisieValide(note)
      || !confirm('Envoi définitif : la note ne pourra plus être modifiée. Continuer ?')) {
      return;
    }
    this.erreur.set(null);
    this.envoi.set(true);
    this.api.rendreRelecture(r.id, etudiantId, note, (this.commentaires[r.id] ?? '').trim()).subscribe({
      next: () => {
        this.envoi.set(false);
        this.message.set('Relecture envoyée.');
        // #106 : le message s'efface tout seul, il ne doit pas rester au-dessus des listes rafraîchies.
        clearTimeout(this.minuterieMessage);
        this.minuterieMessage = setTimeout(() => this.message.set(''), 5000);
        this.charger();
      },
      error: (e: ErreurApi) => { this.envoi.set(false); this.erreur.set(e); },
    });
  }

  charger(): void {
    const etudiantId = this.etudiantId();
    if (etudiantId === null) {
      return;
    }
    this.api.relectures(etudiantId, 'A_FAIRE').subscribe({
      // #106 : un rafraîchissement réussi efface l'erreur précédente.
      next: l => { this.relectures.set(l); this.erreur.set(null); },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
    this.api.relectures(etudiantId, 'RENDUE').subscribe({
      next: l => { this.rendues.set(l); this.erreur.set(null); },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }
}
