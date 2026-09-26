import { DecimalPipe } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ErreurApi, ExerciceAuteur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { BoutonExerciceComponent } from '../../shared/bouton-exercice/bouton-exercice.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';
import { RAFRAICHISSEMENT_MS } from './relectures.component';

/**
 * SF-14 / #88 : note retenue de chaque exercice (moyenne des deux pairs, RG16 v3), marquée « provisoire »
 * tant qu'un seul a rendu (RG31), avec les commentaires reçus, sans les relecteurs (RG8).
 */
@Component({
  selector: 'app-mes-notes',
  standalone: true,
  imports: [DecimalPipe, ErreurComponent, BoutonExerciceComponent],
  templateUrl: './mes-notes.component.html',
})
export class MesNotesComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);
  private minuterie?: ReturnType<typeof setInterval>;

  /** Écran routé (#104) : l'étudiant est celui du compte connecté (HYP-15). */
  readonly etudiantId = computed(() => this.auth.profil()?.etudiantId ?? null);
  readonly exercices = signal<ExerciceAuteur[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);

  /** #101 : une note rendue pendant que la page est ouverte apparaît sans recharger. */
  ngOnInit(): void {
    this.charger();
    this.minuterie = setInterval(() => this.charger(), RAFRAICHISSEMENT_MS);
  }

  ngOnDestroy(): void {
    clearInterval(this.minuterie);
  }

  charger(): void {
    const etudiantId = this.etudiantId();
    if (etudiantId === null) {
      return;
    }
    this.api.mesExercices(etudiantId).subscribe({
      // #106 : des données à jour ne doivent pas cohabiter avec une vieille erreur.
      next: l => { this.exercices.set(l); this.erreur.set(null); },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }
}
