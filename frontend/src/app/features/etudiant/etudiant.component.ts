import { Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, ExerciceDepose, Session } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { ErreurComponent } from '../../shared/erreur.component';
import { MesNotesComponent } from './mes-notes.component';
import { RelecturesComponent } from './relectures.component';

/**
 * Espace étudiant, mobile d'abord (ENF1). Une seule connexion : l'identité vient du compte (HYP-15),
 * plus de choix du nom dans une liste. Présence (SF-3), dépôt (SF-6), notes (SF-14), relectures (SF-8, SF-9).
 */
@Component({
  selector: 'app-etudiant',
  standalone: true,
  imports: [FormsModule, ErreurComponent, MesNotesComponent, RelecturesComponent],
  styles: [':host main { max-width: 36rem; }'],
  template: `
    <main>
      @if (etudiantId(); as moi) {
        <section class="carte">
          <h2>Présence</h2>
          <form class="ligne" (ngSubmit)="marquer(moi)">
            <label>Code affiché en salle
              <input name="code" [(ngModel)]="code" required autocomplete="off" autocapitalize="characters"
                     maxlength="12" placeholder="K7MX4Q" />
            </label>
            <button type="submit" [disabled]="!code.trim()">Valider</button>
          </form>
          @if (presenceOk()) { <p class="succes" role="status">Présence enregistrée.</p> }
          <app-erreur [erreur]="erreurPresence()" />
        </section>

        <section class="carte">
          <h2>Déposer mon exercice</h2>
          <form class="ligne" (ngSubmit)="deposer(moi)">
            <label>Session
              <select name="session" [(ngModel)]="sessionId" required>
                <option [ngValue]="null" disabled>Choisir…</option>
                @for (s of sessionsOuvertes(); track s.id) { <option [ngValue]="s.id">{{ s.titre }}</option> }
              </select>
            </label>
            <label>Lien
              <input name="lien" type="url" [(ngModel)]="lien" required placeholder="https://github.com/…" />
            </label>
            <button type="submit" [disabled]="!sessionId || !lien.trim()">Déposer</button>
          </form>
          @if (depot(); as d) {
            <p class="succes" role="status">Exercice déposé — statut <span class="badge" [class]="d.statut">{{ d.statut }}</span></p>
          }
          <app-erreur [erreur]="erreurDepot()" />
        </section>

        <app-mes-notes [etudiantId]="moi" />
        <app-relectures [etudiantId]="moi" />
      } @else {
        <p class="alerte">Ce compte n'est lié à aucune fiche étudiant.</p>
      }
    </main>
  `,
})
export class EtudiantComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private readonly notes = viewChild(MesNotesComponent);

  readonly etudiantId = computed(() => this.auth.profil()?.etudiantId ?? null);
  private readonly promotionId = computed(() => this.auth.profil()?.promotionIds[0] ?? null);
  readonly sessions = signal<Session[]>([]);
  /** RG18 : une session clôturée n'accepte plus de dépôt ; le serveur le vérifie de toute façon. */
  readonly sessionsOuvertes = computed(() => this.sessions().filter(s => s.statut === 'OUVERTE'));
  readonly presenceOk = signal(false);
  readonly erreurPresence = signal<ErreurApi | null>(null);
  readonly depot = signal<ExerciceDepose | null>(null);
  readonly erreurDepot = signal<ErreurApi | null>(null);

  code = '';
  sessionId: number | null = null;
  lien = '';

  ngOnInit(): void {
    const promotionId = this.promotionId();
    if (promotionId) {
      this.api.sessions(promotionId).subscribe({
        next: s => this.sessions.set(s),
        error: (e: ErreurApi) => this.erreurDepot.set(e),
      });
    }
  }

  /** Le code est envoyé tel que saisi : normalisation et règles côté serveur (F3). */
  marquer(etudiantId: number): void {
    this.presenceOk.set(false);
    this.erreurPresence.set(null);
    this.api.marquerPresence(this.code.trim(), etudiantId).subscribe({
      next: p => { this.presenceOk.set(true); this.sessionId = p.sessionId; this.code = ''; },
      error: (e: ErreurApi) => this.erreurPresence.set(e),
    });
  }

  deposer(etudiantId: number): void {
    if (!this.sessionId) {
      return;
    }
    this.depot.set(null);
    this.erreurDepot.set(null);
    this.api.deposerExercice(this.sessionId, etudiantId, this.lien.trim()).subscribe({
      next: d => { this.depot.set(d); this.lien = ''; this.notes()?.charger(); },
      error: (e: ErreurApi) => this.erreurDepot.set(e),
    });
  }
}
