import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Etudiant, ErreurApi, ExerciceDepose, Promotion } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { IdentiteService } from '../../core/identite/identite.service';
import { ErreurComponent } from '../../shared/erreur.component';

/** Écran étudiant, mobile d'abord (ENF1) : identité (SF-1), présence par code (SF-3), dépôt (SF-6). */
@Component({
  selector: 'app-etudiant',
  standalone: true,
  imports: [FormsModule, ErreurComponent],
  styles: [':host main { max-width: 28rem; }'],
  template: `
    <main>
      @if (identite.identite(); as moi) {
        <p class="ligne">Bonjour {{ moi.nom }} <button class="secondaire" (click)="identite.oublier()">Changer</button></p>

        <section class="carte">
          <h2>Présence</h2>
          <form class="ligne" (ngSubmit)="marquer(moi.etudiantId)">
            <label>Code
              <input name="code" [(ngModel)]="code" required autocomplete="off" autocapitalize="characters"
                     inputmode="text" maxlength="12" placeholder="K7MX4Q" />
            </label>
            <button type="submit" [disabled]="!code.trim()">Valider</button>
          </form>
          @if (presenceOk()) { <p class="succes" role="status">Présence enregistrée.</p> }
          <app-erreur [erreur]="erreurPresence()" />
        </section>

        <section class="carte">
          <h2>Mon exercice</h2>
          <form class="ligne" (ngSubmit)="deposer(moi.etudiantId)">
            <label>Session n°
              <input name="session" type="number" [(ngModel)]="sessionId" required min="1" />
            </label>
            <label>Lien
              <input name="lien" type="url" [(ngModel)]="lien" required placeholder="https://github.com/…" />
            </label>
            <button type="submit" [disabled]="!sessionId || !lien.trim()">Déposer</button>
          </form>
          <p class="discret">La session est renseignée automatiquement après la validation du code.</p>
          @if (depot(); as d) {
            <p class="succes" role="status">Exercice déposé — statut <span class="badge" [class]="d.statut">{{ d.statut }}</span></p>
          }
          <app-erreur [erreur]="erreurDepot()" />
        </section>
      } @else {
        <section class="carte">
          <h2>Qui êtes-vous ?</h2>
          <label>Promotion
            <select name="promotion" [ngModel]="promotionId" (ngModelChange)="choisirPromotion($event)">
              <option [ngValue]="null" disabled>Choisir…</option>
              @for (p of promotions(); track p.id) { <option [ngValue]="p.id">{{ p.nom }}</option> }
            </select>
          </label>
          @if (etudiants().length > 0) {
            <label>Votre nom
              <select name="etudiant" [ngModel]="null" (ngModelChange)="choisirEtudiant($event)">
                <option [ngValue]="null" disabled>Choisir…</option>
                @for (e of etudiants(); track e.id) { <option [ngValue]="e">{{ e.nom }}</option> }
              </select>
            </label>
          }
          <app-erreur [erreur]="erreurIdentite()" />
        </section>
      }
    </main>
  `,
})
export class EtudiantComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly identite = inject(IdentiteService);

  readonly promotions = signal<Promotion[]>([]);
  readonly etudiants = signal<Etudiant[]>([]);
  readonly erreurIdentite = signal<ErreurApi | null>(null);
  readonly presenceOk = signal(false);
  readonly erreurPresence = signal<ErreurApi | null>(null);
  readonly depot = signal<ExerciceDepose | null>(null);
  readonly erreurDepot = signal<ErreurApi | null>(null);

  promotionId: number | null = null;
  code = '';
  sessionId: number | null = null;
  lien = '';

  ngOnInit(): void {
    this.api.promotions().subscribe({ next: p => this.promotions.set(p), error: (e: ErreurApi) => this.erreurIdentite.set(e) });
  }

  choisirPromotion(promotionId: number): void {
    this.promotionId = promotionId;
    this.api.etudiants(promotionId).subscribe({
      next: e => this.etudiants.set(e),
      error: (e: ErreurApi) => this.erreurIdentite.set(e),
    });
  }

  choisirEtudiant(etudiant: Etudiant): void {
    if (this.promotionId) {
      this.identite.choisir({ promotionId: this.promotionId, etudiantId: etudiant.id, nom: etudiant.nom });
    }
  }

  /** Le code est envoyé tel que saisi : la normalisation et toutes les règles sont côté serveur. */
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
      next: d => this.depot.set(d),
      error: (e: ErreurApi) => this.erreurDepot.set(e),
    });
  }
}
