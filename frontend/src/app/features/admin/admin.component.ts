import { Component, OnInit, inject, signal } from '@angular/core';
import { ErreurApi, Promotion } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { CHEMINS, PARAM_PROMOTION, cheminTableau } from '../../core/navigation/chemins';
import { BoutonNavigationComponent } from '../../shared/bouton-navigation/bouton-navigation.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';

/**
 * Espace ADMIN (cahier §2 bis) : toutes les promotions, avec un bouton vers leur tableau et vers leurs sessions.
 * Les écrans de gestion (comptes, promotions, étudiants : #60–#62) ne sont pas livrés : c'est affiché.
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [ErreurComponent, BoutonNavigationComponent],
  templateUrl: './admin.component.html',
})
export class AdminComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly cheminTableau = cheminTableau;
  readonly cheminSessions = CHEMINS.formateur;
  readonly promotions = signal<Promotion[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);

  /** « Sessions » ouvre l'espace formateur sur la promotion cliquée, pas sur la première (#104). */
  parametresSessions(promotionId: number): Record<string, number> {
    return { [PARAM_PROMOTION]: promotionId };
  }

  ngOnInit(): void {
    this.api.promotions().subscribe({ next: p => this.promotions.set(p), error: (e: ErreurApi) => this.erreur.set(e) });
  }
}
