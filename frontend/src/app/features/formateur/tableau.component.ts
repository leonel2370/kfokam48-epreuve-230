import { DecimalPipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { ErreurApi, LigneTableau } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { AuthService } from '../../core/auth/auth.service';
import { CHEMINS, EntreeNavigation, PARAM_PROMOTION } from '../../core/navigation/chemins';
import { BoutonNavigationComponent } from '../../shared/bouton-navigation/bouton-navigation.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';

/** SF-10 / EF10 : affichage brut du tableau calculé par le serveur, aucun recalcul (F3). */
@Component({
  selector: 'app-tableau',
  standalone: true,
  imports: [DecimalPipe, ErreurComponent, BoutonNavigationComponent],
  templateUrl: './tableau.component.html',
})
export class TableauComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly auth = inject(AuthService);

  /** Paramètre de route :promotionId (withComponentInputBinding). */
  readonly promotionId = input.required<string>();
  readonly lignes = signal<LigneTableau[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);
  readonly chargement = signal(true);

  /** Retour contextuel (spécifications §1.2 bis) : l'admin revient à l'administration, le formateur à ses sessions. */
  readonly retour = computed<EntreeNavigation>(() => this.auth.profil()?.role === 'ADMIN'
    ? { chemin: CHEMINS.admin, libelle: "← Retour à l'administration" }
    : { chemin: CHEMINS.formateur, libelle: '← Retour aux sessions', parametres: { [PARAM_PROMOTION]: this.promotionId() } });

  ngOnInit(): void {
    this.api.tableau(Number(this.promotionId())).subscribe({
      next: l => { this.lignes.set(l); this.chargement.set(false); },
      error: (e: ErreurApi) => { this.erreur.set(e); this.chargement.set(false); },
    });
  }
}
