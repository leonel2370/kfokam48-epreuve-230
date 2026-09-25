import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ErreurApi, Promotion, SessionOuverte } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { ErreurComponent } from '../../shared/erreur.component';

const SECONDE = 1000;

/** SF-2 / EF2 : le formateur ouvre une session et affiche le code en grand avec son expiration. */
@Component({
  selector: 'app-formateur',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, ErreurComponent],
  template: `
    <main>
      <section class="carte">
        <h2>Ouvrir une session</h2>
        <form class="ligne" (ngSubmit)="ouvrir()">
          <label>Promotion
            <select name="promotion" [(ngModel)]="promotionId" required>
              @for (p of promotions(); track p.id) { <option [ngValue]="p.id">{{ p.nom }}</option> }
            </select>
          </label>
          <label>Titre
            <input name="titre" [(ngModel)]="titre" required maxlength="200" placeholder="TP Spring JPA" />
          </label>
          <button type="submit" [disabled]="enCours() || !titre.trim() || !promotionId">
            {{ enCours() ? 'Ouverture…' : 'Ouvrir' }}
          </button>
          @if (promotionId) { <a [routerLink]="['/formateur/tableau', promotionId]">Voir le tableau</a> }
        </form>
        <app-erreur [erreur]="erreur()" />
      </section>

      @if (session(); as s) {
        <section class="carte" aria-live="polite">
          <h2>Code de présence</h2>
          <p class="code">{{ s.code }}</p>
          <p class="discret">
            Expire à {{ s.expirationAt | date: 'HH:mm' }}
            @if (restant() > 0) { — encore {{ minutes() }} min {{ secondes() }} s }
            @else { — expiré : ajoutez les retardataires manuellement }
          </p>
        </section>
      }
    </main>
  `,
})
export class FormateurComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private minuterie?: ReturnType<typeof setInterval>;

  readonly promotions = signal<Promotion[]>([]);
  readonly session = signal<SessionOuverte | null>(null);
  readonly erreur = signal<ErreurApi | null>(null);
  readonly enCours = signal(false);
  private readonly maintenant = signal(Date.now());

  /** Compte à rebours d'affichage uniquement : l'expiration fait foi côté serveur (RG1). */
  readonly restant = computed(() => {
    const s = this.session();
    return s ? Math.max(0, Math.floor((Date.parse(s.expirationAt) - this.maintenant()) / SECONDE)) : 0;
  });
  readonly minutes = computed(() => Math.floor(this.restant() / 60));
  readonly secondes = computed(() => this.restant() % 60);

  promotionId: number | null = null;
  titre = '';

  ngOnInit(): void {
    this.api.promotions().subscribe({
      next: p => { this.promotions.set(p); this.promotionId ??= p[0]?.id ?? null; },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
    this.minuterie = setInterval(() => this.maintenant.set(Date.now()), SECONDE);
  }

  ngOnDestroy(): void {
    clearInterval(this.minuterie);
  }

  ouvrir(): void {
    if (!this.promotionId) {
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);
    this.api.ouvrirSession(this.titre.trim(), this.promotionId).subscribe({
      next: s => { this.session.set(s); this.titre = ''; this.enCours.set(false); },
      error: (e: ErreurApi) => { this.erreur.set(e); this.enCours.set(false); },
    });
  }
}
