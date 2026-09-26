import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, ExerciceDepose, Session } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { CHEMINS } from '../../core/navigation/chemins';
import { LIBELLES_STATUT_EXERCICE } from '../../core/navigation/libelles';
import { BoutonNavigationComponent } from '../../shared/bouton-navigation/bouton-navigation.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';

/**
 * Écran ÉTUDIANT (F2), mobile d'abord (ENF1) : présence par code (SF-3) et dépôt de l'exercice (SF-6).
 * L'identité vient du compte (HYP-15), plus de choix du nom dans une liste. Notes et relectures ont leurs
 * propres écrans (#104).
 */
@Component({
  selector: 'app-presence',
  standalone: true,
  imports: [FormsModule, ErreurComponent, BoutonNavigationComponent],
  templateUrl: './presence.component.html',
})
export class PresenceComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  readonly chemins = CHEMINS;

  readonly etudiantId = computed(() => this.auth.profil()?.etudiantId ?? null);
  private readonly promotionId = computed(() => this.auth.profil()?.promotionIds[0] ?? null);
  readonly sessions = signal<Session[]>([]);
  /** RG18 : une session clôturée n'accepte plus de dépôt ; le serveur le vérifie de toute façon. */
  readonly sessionsOuvertes = computed(() => this.sessions().filter(s => s.statut === 'OUVERTE'));
  readonly presenceOk = signal(false);
  readonly erreurPresence = signal<ErreurApi | null>(null);
  readonly depot = signal<ExerciceDepose | null>(null);
  readonly erreurDepot = signal<ErreurApi | null>(null);
  /** #106 : les requêtes en cours désactivent leurs boutons (pas de double envoi). */
  readonly presenceEnCours = signal(false);
  readonly depotEnCours = signal(false);
  /** #107 : l'erreur de chargement des sessions a sa propre zone, distincte de celle du dépôt. */
  readonly erreurChargement = signal<ErreurApi | null>(null);

  code = '';
  sessionId: number | null = null;
  lien = '';

  /** #108 : libellés français des statuts — le code brut n'est plus affiché. */
  libelleExercice(statut: string): string {
    return LIBELLES_STATUT_EXERCICE[statut] ?? statut;
  }

  ngOnInit(): void {
    this.chargerSessions();
  }

  /** #107 : la liste est rechargée après une présence ou un dépôt — le formateur peut ouvrir une session
   *  pendant que l'écran est ouvert ; le sélecteur ne doit pas rester vide ni trompeur. */
  private chargerSessions(): void {
    const promotionId = this.promotionId();
    if (!promotionId) {
      return;
    }
    this.api.sessions(promotionId).subscribe({
      next: s => { this.sessions.set(s); this.erreurChargement.set(null); },
      error: (e: ErreurApi) => this.erreurChargement.set(e),
    });
  }

  /** Le code est envoyé tel que saisi : normalisation et règles côté serveur (F3). */
  marquer(etudiantId: number): void {
    this.presenceOk.set(false);
    this.erreurPresence.set(null);
    this.presenceEnCours.set(true);
    this.api.marquerPresence(this.code.trim(), etudiantId).subscribe({
      next: p => {
        this.presenceOk.set(true);
        this.sessionId = p.sessionId;
        this.code = '';
        this.presenceEnCours.set(false);
        this.chargerSessions();
      },
      error: (e: ErreurApi) => {
        this.presenceEnCours.set(false);
        this.erreurPresence.set(e);
      },
    });
  }

  deposer(etudiantId: number): void {
    if (!this.sessionId) {
      return;
    }
    this.depot.set(null);
    this.erreurDepot.set(null);
    this.depotEnCours.set(true);
    this.api.deposerExercice(this.sessionId, etudiantId, this.lien.trim()).subscribe({
      next: d => {
        this.depot.set(d);
        this.lien = '';
        this.depotEnCours.set(false);
        this.chargerSessions();
      },
      error: (e: ErreurApi) => {
        this.depotEnCours.set(false);
        this.erreurDepot.set(e);
      },
    });
  }
}
