import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, PageUtilisateurs, Promotion, Utilisateur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { DialogueService } from '../../core/dialogue/dialogue.service';
import { Ecriture } from '../../core/etat/ecriture';
import { Lecture } from '../../core/etat/lecture';
import { CHEMINS, PARAM_PROMOTION, cheminTableau } from '../../core/navigation/chemins';
import { LIBELLES_ROLE } from '../../core/libelles/libelles';
import { BoutonNavigationComponent } from '../../shared/bouton-navigation/bouton-navigation.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';
import { CreationCompteComponent } from './creation-compte/creation-compte.component';
import { GestionPromotionComponent } from './gestion-promotion/gestion-promotion.component';

/** Longueur maximale du nom d'une promotion (contrat PromotionEcriture). */
const LONGUEUR_NOM_PROMOTION = 100;
const AUCUN_COMPTE: PageUtilisateurs = { contenu: [], page: 0, taille: 0, total: 0 };

/**
 * Espace ADMIN (cahier §2 bis) : les promotions (SF-21) avec un bouton vers leur tableau, leurs sessions et
 * leur volet de gestion (app-gestion-promotion), puis les comptes (SF-20) et leur création
 * (app-creation-compte). Aucune règle métier ici (F3) : tout vient de l'API.
 * #137 : promotions et comptes ont chacun leur état de lecture, leur zone d'erreur et leur message ;
 * une seule écriture à la fois.
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
  private readonly dialogue = inject(DialogueService);
  readonly cheminTableau = cheminTableau;
  readonly cheminSessions = CHEMINS.formateur;
  readonly ecriture = new Ecriture();
  readonly libelles = LIBELLES_ROLE;

  // --- Promotions ---
  readonly lecturePromotions = new Lecture<Promotion[]>([]);
  readonly promotions = this.lecturePromotions.donnees;
  readonly erreurPromotions = signal<ErreurApi | null>(null);
  readonly messagePromotions = signal<string | null>(null);
  readonly promotionSelectionnee = signal<Promotion | null>(null);
  readonly longueurNomPromotion = LONGUEUR_NOM_PROMOTION;
  nomPromotionCreation = '';

  // --- Comptes : la page affichée est celle que le serveur a rendue ---
  readonly lectureComptes = new Lecture<PageUtilisateurs>(AUCUN_COMPTE);
  readonly comptes = computed(() => this.lectureComptes.donnees().contenu);
  readonly total = computed(() => this.lectureComptes.donnees().total);
  readonly page = computed(() => this.lectureComptes.donnees().page);
  readonly aUnePageSuivante = computed(() => {
    const { page, taille, total } = this.lectureComptes.donnees();
    return (page + 1) * taille < total;
  });
  readonly erreurComptes = signal<ErreurApi | null>(null);
  readonly messageComptes = signal<string | null>(null);

  ngOnInit(): void {
    this.chargerPromotions();
    this.chargerComptes(0);
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

  pageSuivante(): void {
    this.chargerComptes(this.page() + 1);
  }

  pagePrecedente(): void {
    this.chargerComptes(this.page() - 1);
  }

  /** #136 : le formulaire de création (app-creation-compte) a réussi ; la liste est relue sur le serveur. */
  compteCree(): void {
    this.chargerComptes(this.page());
  }

  /** RG28 : désactivation, jamais de suppression physique. */
  async desactiver(u: Utilisateur): Promise<void> {
    const confirme = await this.dialogue.confirmer({
      titre: `Désactiver le compte « ${u.login} » ?`,
      message: 'Il ne pourra plus se connecter. Son historique est conservé.',
      confirmer: 'Désactiver',
      danger: true,
    });
    if (!confirme) {
      return;
    }
    this.ecriture.lancer(this.api.desactiverUtilisateur(u.id), this.erreurComptes, () => {
      this.messageComptes.set(`Compte « ${u.login} » désactivé.`);
      this.chargerComptes(this.page());
    });
  }

  /** RG23 : nouveau mot de passe provisoire ; sa solidité est jugée par le serveur (RG24). */
  async reinitialiser(u: Utilisateur): Promise<void> {
    const provisoire = await this.dialogue.saisir({
      titre: `Mot de passe provisoire de « ${u.login} »`,
      confirmer: 'Réinitialiser',
      champ: { libelle: 'Mot de passe (8 caractères minimum)', type: 'password',
        aide: 'Il devra le changer à sa prochaine connexion.' },
    });
    if (!provisoire) {
      return;
    }
    this.ecriture.lancer(this.api.reinitialiserMotDePasse(u.id, provisoire), this.erreurComptes, () => {
      this.messageComptes.set(`Mot de passe de « ${u.login} » réinitialisé ; à changer à la prochaine connexion.`);
      this.chargerComptes(this.page());
    });
  }

  private chargerPromotions(): void {
    this.lecturePromotions.charger(this.api.promotions());
  }

  private chargerComptes(page: number): void {
    this.lectureComptes.charger(this.api.utilisateurs(page));
  }
}
