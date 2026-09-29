import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import {
  ErreurApi,
  EtudiantAdmin,
  PageUtilisateurs,
  Promotion,
  Role,
  Utilisateur,
} from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { CHEMINS, PARAM_PROMOTION, cheminTableau } from '../../core/navigation/chemins';
import { BoutonNavigationComponent } from '../../shared/bouton-navigation/bouton-navigation.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';

/**
 * Espace ADMIN (cahier §2 bis) : toutes les promotions, avec un bouton vers leur tableau et vers leurs sessions.
 * #60 : gestion des comptes — liste, création (mot de passe provisoire RG23), désactivation (RG28),
 * réinitialisation de mot de passe. Aucune règle métier calculée ici (F3) : tout vient de l'API.
 * #61 : gestion du référentiel — promotions (SF-21), rattachement des formateurs (RG26), fiches étudiants (SF-22).
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, ErreurComponent, BoutonNavigationComponent],
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
  readonly ROLES: Role[] = ['ADMIN', 'FORMATEUR', 'ETUDIANT'];

  // formulaire de création (ngModel, comme les autres écrans)
  login = '';
  nomAffiche = '';
  role: Role = 'ETUDIANT';
  motDePasseInitial = '';
  etudiantIdCreation: number | null = null;

  // confirmation d'action
  readonly message = signal<string | null>(null);
  readonly enCours = signal(false);

  // --- #61 : référentiel ---
  readonly promotionSelectionnee = signal<Promotion | null>(null);
  readonly formateursRattaches = signal<Utilisateur[]>([]);
  readonly fiches = signal<EtudiantAdmin[]>([]);
  nomPromotionCreation = '';
  nomFicheCreation = '';

  ngOnInit(): void {
    this.api.promotions().subscribe({ next: p => this.chargerPromotions(p), error: (e: ErreurApi) => this.erreur.set(e) });
    this.chargerComptes();
  }

  /** « Sessions » ouvre l'espace formateur sur la promotion cliquée, pas sur la première (#104). */
  parametresSessions(promotionId: number): Record<string, number> {
    return { [PARAM_PROMOTION]: promotionId };
  }

  /** #61 : ouvre le volet de gestion d'une promotion (formateurs + fiches étudiants). */
  gerer(p: Promotion): void {
    this.promotionSelectionnee.set(p);
    this.nomFicheCreation = '';
    this.api.etudiantsAdmin(p.id).subscribe({
      next: f => {
        this.fiches.set(f);
        this.erreur.set(null);
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  /** #61 : les formateurs rattachables sont les comptes FORMATEUR actifs (RG26). */
  formateurs(): Utilisateur[] {
    return this.comptes().filter(u => u.role === 'FORMATEUR' && u.actif);
  }

  estRattache(u: Utilisateur): boolean {
    return this.formateursRattaches().some(f => f.id === u.id);
  }

  basculerRattachement(u: Utilisateur): void {
    const p = this.promotionSelectionnee();
    if (!p) {
      return;
    }
    const ids = this.estRattache(u)
      ? this.formateursRattaches().filter(f => f.id !== u.id).map(f => f.id)
      : [...this.formateursRattaches().map(f => f.id), u.id];
    this.api.rattacherFormateurs(p.id, ids).subscribe({
      next: () => {
        this.formateursRattaches.set(
          this.formateurs().filter(f => ids.includes(f.id)),
        );
        this.message.set(`Formateurs de « ${p.nom} » enregistrés.`);
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
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
    const nom = prompt(`Nouveau nom de la promotion « ${p.nom} » :`, p.nom);
    if (!nom || !nom.trim() || nom.trim() === p.nom) {
      return;
    }
    this.api.renommerPromotion(p.id, nom.trim()).subscribe({
      next: modifiee => {
        this.chargerPromotions(this.promotions().map(x => (x.id === p.id ? modifiee : x)));
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

  creerFiche(): void {
    const p = this.promotionSelectionnee();
    const nom = this.nomFicheCreation.trim();
    if (!p || !nom) {
      return;
    }
    this.api.creerEtudiant({ nom, promotionId: p.id }).subscribe({
      next: f => {
        // la réponse du contrat (Etudiant) ne porte pas actif : une fiche créée est active
        this.fiches.set([...this.fiches(), { ...f, actif: true }].sort((a, b) => a.nom.localeCompare(b.nom)));
        this.message.set(`Fiche « ${f.nom} » créée dans « ${p.nom} ».`);
        this.nomFicheCreation = '';
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  renommerFiche(f: EtudiantAdmin): void {
    const nom = prompt(`Nouveau nom de l'étudiant « ${f.nom} » :`, f.nom);
    const p = this.promotionSelectionnee();
    if (!p || !nom || !nom.trim() || nom.trim() === f.nom) {
      return;
    }
    this.api.modifierEtudiant(f.id, { nom: nom.trim(), promotionId: p.id }).subscribe({
      next: modifiee => {
        this.fiches.set(this.fiches().map(x => (x.id === f.id ? { ...modifiee, actif: f.actif } : x)));
        this.message.set(`Fiche renommée en « ${modifiee.nom} ».`);
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  supprimerFiche(f: EtudiantAdmin): void {
    if (!confirm(
      `Supprimer la fiche de « ${f.nom} » ? Sans historique elle sera supprimée ; sinon elle sera ` +
      'désactivée (RG28) et restera au tableau.',
    )) {
      return;
    }
    this.api.supprimerEtudiant(f.id).subscribe({
      next: () => this.gerer(this.promotionSelectionnee()!),
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

  /** #60 : la fiche étudiant n'est demandée que pour le rôle ETUDIANT (HYP-15). */
  creer(): void {
    if (!this.login.trim() || !this.nomAffiche.trim() || this.motDePasseInitial.length < 8) {
      return; // le formulaire exige déjà required/minlength
    }
    this.enCours.set(true);
    this.api.creerUtilisateur({
      login: this.login.trim(),
      nomAffiche: this.nomAffiche.trim(),
      role: this.role,
      motDePasseInitial: this.motDePasseInitial,
      etudiantId: this.role === 'ETUDIANT' ? this.etudiantIdCreation : null,
    }).subscribe({
      next: u => {
        this.enCours.set(false);
        this.message.set(`Compte « ${u.login} » créé ; il devra changer son mot de passe à la première connexion.`);
        this.reinitialiserFormulaire();
        this.chargerComptes();
      },
      error: (e: ErreurApi) => {
        this.enCours.set(false);
        this.erreur.set(e);
      },
    });
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

  private reinitialiserFormulaire(): void {
    this.login = '';
    this.nomAffiche = '';
    this.motDePasseInitial = '';
    this.etudiantIdCreation = null;
  }
}
