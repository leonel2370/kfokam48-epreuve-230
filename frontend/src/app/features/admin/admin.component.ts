import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, Promotion, Utilisateur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { CHEMINS, PARAM_PROMOTION, cheminTableau } from '../../core/navigation/chemins';
import { BoutonNavigationComponent } from '../../shared/bouton-navigation/bouton-navigation.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';
import { CreationCompteComponent } from './creation-compte/creation-compte.component';
import { GestionPromotionComponent } from './gestion-promotion/gestion-promotion.component';

/**
 * Espace ADMIN (cahier §2 bis) : toutes les promotions, avec un bouton vers leur tableau et vers leurs sessions.
 * #60 : gestion des comptes — liste, création (mot de passe provisoire RG23), désactivation (RG28),
 * réinitialisation de mot de passe. Aucune règle métier calculée ici (F3) : tout vient de l'API.
 * #61 : promotions (SF-21) ; le volet d'une promotion (formateurs RG26, fiches SF-22) est app-gestion-promotion.
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, ErreurComponent, BoutonNavigationComponent, GestionPromotionComponent,
    CreationCompteComponent],
  templateUrl: './admin.component.html',
})
export class AdminComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly cheminTableau = cheminTableau;
  readonly cheminSessions = CHEMINS.formateur;
  readonly promotions = signal<Promotion[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);

  // --- #60 : comptes ---
  readonly comptes = signal<Utilisateur[]>([]);
  readonly total = signal(0);
  readonly page = signal(0);

  // confirmation d'action
  readonly message = signal<string | null>(null);

  // --- #61 : référentiel ---
  readonly promotionSelectionnee = signal<Promotion | null>(null);
  nomPromotionCreation = '';

  ngOnInit(): void {
    this.api.promotions().subscribe({
      next: p => this.chargerPromotions(p),
      error: (e: ErreurApi) => this.erreur.set(e),
    });
    this.chargerComptes();
  }

  /** « Sessions » ouvre l'espace formateur sur la promotion cliquée, pas sur la première (#104). */
  parametresSessions(promotionId: number): Record<string, number> {
    return { [PARAM_PROMOTION]: promotionId };
  }

  /** #61 : ouvre le volet de gestion d'une promotion (formateurs et fiches), porté par app-gestion-promotion. */
  gerer(p: Promotion): void {
    this.promotionSelectionnee.set(p);
  }

  creerPromotion(): void {
    const nom = this.nomPromotionCreation.trim();
    if (!nom) {
      return;
    }
    this.api.creerPromotion(nom).subscribe({
      next: p => {
        this.chargerPromotions([...this.promotions(), p].sort((a, b) => a.nom.localeCompare(b.nom)));
        this.message.set(`Promotion « ${p.nom} » créée.`);
        this.nomPromotionCreation = '';
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  renommerPromotion(p: Promotion): void {
    const nom = prompt(`Nouveau nom de la promotion « ${p.nom} » :`, p.nom)?.trim();
    if (!nom || nom === p.nom) {
      return;
    }
    this.api.renommerPromotion(p.id, nom).subscribe({
      next: modifiee => {
        this.chargerPromotions(this.promotions().map(x => (x.id === p.id ? modifiee : x)));
        if (this.promotionSelectionnee()?.id === p.id) {
          this.promotionSelectionnee.set(modifiee);
        }
        this.message.set(`Promotion renommée en « ${modifiee.nom} ».`);
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  supprimerPromotion(p: Promotion): void {
    if (!confirm(`Supprimer la promotion « ${p.nom} » ? Elle doit être vide (RG28).`)) {
      return;
    }
    this.api.supprimerPromotion(p.id).subscribe({
      next: () => {
        this.chargerPromotions(this.promotions().filter(x => x.id !== p.id));
        if (this.promotionSelectionnee()?.id === p.id) {
          this.promotionSelectionnee.set(null);
        }
        this.message.set(`Promotion « ${p.nom} » supprimée.`);
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  private chargerComptes(): void {
    this.api.utilisateurs(this.page()).subscribe({
      next: p => {
        this.comptes.set(p.contenu);
        this.total.set(p.total);
        // #106 : des données à jour effacent les messages d'erreur précédents
        this.erreur.set(null);
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  pageSuivante(): void {
    this.page.set(this.page() + 1);
    this.chargerComptes();
  }

  pagePrecedente(): void {
    this.page.set(this.page() - 1);
    this.chargerComptes();
  }

  /** #136 : le formulaire de création (app-creation-compte) a réussi ; la liste est relue sur le serveur. */
  compteCree(): void {
    this.chargerComptes();
  }

  /** RG28 : désactivation, jamais de suppression physique. */
  desactiver(u: Utilisateur): void {
    if (!confirm(`Désactiver le compte « ${u.login} » ? Il ne pourra plus se connecter.`)) {
      return;
    }
    this.api.desactiverUtilisateur(u.id).subscribe({
      next: () => {
        this.message.set(`Compte « ${u.login} » désactivé.`);
        this.chargerComptes();
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  /** RG23 : nouveau mot de passe provisoire ; redemandé à la prochaine connexion. */
  reinitialiser(u: Utilisateur): void {
    const provisoire = prompt(`Mot de passe provisoire pour « ${u.login} » (8 caractères minimum) :`);
    if (!provisoire || provisoire.length < 8) {
      return;
    }
    this.api.reinitialiserMotDePasse(u.id, provisoire).subscribe({
      next: () => {
        this.message.set(`Mot de passe de « ${u.login} » réinitialisé ; à changer à la prochaine connexion.`);
        this.chargerComptes();
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  private chargerPromotions(liste: Promotion[]): void {
    this.promotions.set(liste);
    this.erreur.set(null);
  }
}
