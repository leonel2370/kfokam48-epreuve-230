import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ErreurApi, Promotion, Session, SessionOuverte } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { ErreurComponent } from '../../shared/erreur/erreur.component';

const SECONDE = 1000;
const MINUTE = 60;

/**
 * Espace formateur (SF-2, EF2) : ses promotions uniquement (RG26 ; toutes pour l'admin), ouverture d'une
 * session avec le code en grand, liste des sessions de la promotion et accès au tableau.
 */
@Component({
  selector: 'app-formateur',
  standalone: true,
  imports: [FormsModule, DatePipe, RouterLink, ErreurComponent],
  templateUrl: './formateur.component.html',
})
export class FormateurComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private minuterie?: ReturnType<typeof setInterval>;

  readonly promotions = signal<Promotion[]>([]);
  readonly sessions = signal<Session[]>([]);
  readonly session = signal<SessionOuverte | null>(null);
  readonly erreur = signal<ErreurApi | null>(null);
  readonly enCours = signal(false);
  private readonly maintenant = signal(Date.now());

  /** Compte à rebours d'affichage uniquement : l'expiration fait foi côté serveur (RG1). */
  readonly restant = computed(() => {
    const s = this.session();
    return s ? Math.max(0, Math.floor((Date.parse(s.expirationAt) - this.maintenant()) / SECONDE)) : 0;
  });
  readonly minutes = computed(() => Math.floor(this.restant() / MINUTE));
  readonly secondes = computed(() => this.restant() % MINUTE);

  promotionId: number | null = null;
  titre = '';

  ngOnInit(): void {
    const moi = this.auth.profil();
    this.api.promotions().subscribe({
      next: toutes => {
        // Le formateur ne voit que ses promotions (RG26) ; l'admin les voit toutes.
        const visibles = moi?.role === 'FORMATEUR' ? toutes.filter(p => moi.promotionIds.includes(p.id)) : toutes;
        this.promotions.set(visibles);
        if (visibles.length > 0) {
          this.choisir(visibles[0].id);
        }
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
    this.minuterie = setInterval(() => this.maintenant.set(Date.now()), SECONDE);
  }

  ngOnDestroy(): void {
    clearInterval(this.minuterie);
  }

  choisir(promotionId: number): void {
    this.promotionId = promotionId;
    this.chargerSessions();
  }

  ouvrir(): void {
    if (!this.promotionId) {
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);
    this.api.ouvrirSession(this.titre.trim(), this.promotionId).subscribe({
      next: s => { this.session.set(s); this.titre = ''; this.enCours.set(false); this.chargerSessions(); },
      error: (e: ErreurApi) => { this.erreur.set(e); this.enCours.set(false); },
    });
  }

  private chargerSessions(): void {
    if (!this.promotionId) {
      return;
    }
    this.api.sessions(this.promotionId).subscribe({
      next: s => this.sessions.set(s),
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }
}
