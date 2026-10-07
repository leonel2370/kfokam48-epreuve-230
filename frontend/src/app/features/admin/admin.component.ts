import { Component, OnInit, inject, signal, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, Promotion } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { DialogueService } from '../../core/dialogue/dialogue.service';
import { Ecriture } from '../../core/etat/ecriture';
import { Lecture } from '../../core/etat/lecture';
import { CHEMINS, PARAM_PROMOTION, cheminTableau } from '../../core/navigation/chemins';
import { BoutonNavigationComponent } from '../../shared/bouton-navigation/bouton-navigation.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';
import { EtatListeComponent } from '../../shared/etat-liste/etat-liste.component';
import { CreationCompteComponent } from './creation-compte/creation-compte.component';
import { GestionPromotionComponent } from './gestion-promotion/gestion-promotion.component';
import { ListeComptesComponent } from './liste-comptes/liste-comptes.component';

/** Longueur maximale du nom d'une promotion (contrat PromotionEcriture). */
const LONGUEUR_NOM_PROMOTION = 100;

/**
 * Espace ADMIN (cahier §2 bis). Cet écran porte les promotions (SF-21) et assemble trois composants (#138) :
 * app-gestion-promotion (formateurs et fiches d'une promotion), app-liste-comptes et app-creation-compte
 * (SF-20). Aucune règle métier ici (F3) : tout vient de l'API ; chaque zone a son état et son erreur (#137).
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, ErreurComponent, EtatListeComponent, BoutonNavigationComponent, GestionPromotionComponent,
    ListeComptesComponent, CreationCompteComponent],
  templateUrl: './admin.component.html',
})
export class AdminComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly dialogue = inject(DialogueService);
  readonly cheminTableau = cheminTableau;
  readonly cheminSessions = CHEMINS.formateur;
  readonly ecriture = new Ecriture();
  private readonly listeComptes = viewChild.required(ListeComptesComponent);

  // --- Promotions ---
  readonly lecturePromotions = new Lecture<Promotion[]>([]);
  readonly promotions = this.lecturePromotions.donnees;
  readonly erreurPromotions = signal<ErreurApi | null>(null);
  readonly messagePromotions = signal<string | null>(null);
  readonly promotionSelectionnee = signal<Promotion | null>(null);
  readonly longueurNomPromotion = LONGUEUR_NOM_PROMOTION;
  nomPromotionCreation = '';

  ngOnInit(): void {
    this.chargerPromotions();
  }

  /** « Sessions » ouvre l'espace formateur sur la promotion cliquée, pas sur la première (#104). */
  parametresSessions(promotionId: number): Record<string, number> {
    return { [PARAM_PROMOTION]: promotionId };
  }

  /** Ouvre le volet de gestion d'une promotion (formateurs et fiches), porté par app-gestion-promotion. */
  gerer(p: Promotion): void {
    this.promotionSelectionnee.set(p);
  }

  creerPromotion(): void {
    const nom = this.nomPromotionCreation.trim();
    if (!nom) {
      return;
    }
    this.ecriture.lancer(this.api.creerPromotion(nom), this.erreurPromotions, p => {
      this.messagePromotions.set(`Promotion « ${p.nom} » créée.`);
      this.nomPromotionCreation = '';
      this.chargerPromotions();
    });
  }

  async renommerPromotion(p: Promotion): Promise<void> {
    const saisie = await this.dialogue.saisir({
      titre: `Renommer la promotion « ${p.nom} »`,
      confirmer: 'Enregistrer',
      champ: { libelle: 'Nouveau nom', type: 'text', valeur: p.nom, longueurMax: LONGUEUR_NOM_PROMOTION },
    });
    const nom = saisie?.trim();
    if (!nom || nom === p.nom) {
      return;
    }
    this.ecriture.lancer(this.api.renommerPromotion(p.id, nom), this.erreurPromotions, modifiee => {
      this.messagePromotions.set(`Promotion renommée en « ${modifiee.nom} ».`);
      if (this.promotionSelectionnee()?.id === p.id) {
        this.promotionSelectionnee.set(modifiee);
      }
      this.chargerPromotions();
    });
  }

  async supprimerPromotion(p: Promotion): Promise<void> {
    const confirme = await this.dialogue.confirmer({
      titre: `Supprimer la promotion « ${p.nom} » ?`,
      message: "Elle doit n'avoir ni étudiant ni session.",
      confirmer: 'Supprimer',
      danger: true,
    });
    if (!confirme) {
      return;
    }
    this.ecriture.lancer(this.api.supprimerPromotion(p.id), this.erreurPromotions, () => {
      this.messagePromotions.set(`Promotion « ${p.nom} » supprimée.`);
      if (this.promotionSelectionnee()?.id === p.id) {
        this.promotionSelectionnee.set(null);
      }
      this.chargerPromotions();
    });
  }

  /** #136 : un compte vient d'être créé ; la liste des comptes est relue sur le serveur. */
  compteCree(): void {
    this.listeComptes().recharger();
  }

  private chargerPromotions(): void {
    this.lecturePromotions.charger(this.api.promotions());
  }
}
