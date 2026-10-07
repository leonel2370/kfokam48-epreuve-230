import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, RelectureRelecteur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { DialogueService } from '../../core/dialogue/dialogue.service';
import { Lecture } from '../../core/etat/lecture';
import { BoutonExerciceComponent } from '../../shared/bouton-exercice/bouton-exercice.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';
import { EtatListeComponent } from '../../shared/etat-liste/etat-liste.component';

/** #101 : les listes se rafraîchissent seules ; une relecture ou une note peut arriver, page ouverte. */
export const RAFRAICHISSEMENT_MS = 15_000;
/** Durée d'affichage du message de succès après un envoi. */
const DUREE_MESSAGE_MS = 5_000;
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
  imports: [FormsModule, ErreurComponent, BoutonExerciceComponent, EtatListeComponent],
  templateUrl: './relectures.component.html',
})
export class RelecturesComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly dialogue = inject(DialogueService);
  private minuterie?: ReturnType<typeof setInterval>;

  /** Écran RELECTEUR routé (F2, #104) : le relecteur est l'étudiant du compte connecté (HYP-15). */
  readonly etudiantId = computed(() => this.auth.profil()?.etudiantId ?? null);
  /** #137 : chaque liste a son état ; l'échec de l'une n'est pas effacé par la réussite de l'autre. */
  readonly aFaire = new Lecture<RelectureRelecteur[]>([]);
  readonly dejaRendues = new Lecture<RelectureRelecteur[]>([]);
  readonly relectures = this.aFaire.donnees;
  readonly rendues = this.dejaRendues.donnees;
  /** Erreur de l'envoi d'une relecture (les erreurs de lecture sont portées par chaque liste). */
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

  /** RG10 : l'envoi est définitif, il passe par une confirmation à l'écran. */
  async rendre(r: RelectureRelecteur): Promise<void> {
    const note = this.notes[r.id];
    const etudiantId = this.etudiantId();
    if (etudiantId === null || !noteSaisieValide(note)) {
      return;
    }
    const confirme = await this.dialogue.confirmer({
      titre: 'Envoyer la relecture ?',
      message: 'La note est définitive : elle ne pourra plus être modifiée.',
      confirmer: "Confirmer l'envoi",
    });
    if (!confirme) {
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
        this.minuterieMessage = setTimeout(() => this.message.set(''), DUREE_MESSAGE_MS);
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
    this.aFaire.charger(this.api.relectures(etudiantId, 'A_FAIRE'));
    this.dejaRendues.charger(this.api.relectures(etudiantId, 'RENDUE'));
  }
}
