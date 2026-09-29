import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, PageUtilisateurs, Promotion, Role, Utilisateur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { CHEMINS, PARAM_PROMOTION, cheminTableau } from '../../core/navigation/chemins';
import { BoutonNavigationComponent } from '../../shared/bouton-navigation/bouton-navigation.component';
import { ErreurComponent } from '../../shared/erreur/erreur.component';

/**
 * Espace ADMIN (cahier §2 bis) : toutes les promotions, avec un bouton vers leur tableau et vers leurs sessions.
 * #60 : gestion des comptes — liste, création (mot de passe provisoire RG23), désactivation (RG28),
 * réinitialisation de mot de passe. Aucune règle métier calculée ici (F3) : tout vient de l'API.
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

  ngOnInit(): void {
    this.api.promotions().subscribe({ next: p => this.promotions.set(p), error: (e: ErreurApi) => this.erreur.set(e) });
    this.chargerComptes();
  }

  /** « Sessions » ouvre l'espace formateur sur la promotion cliquée, pas sur la première (#104). */
  parametresSessions(promotionId: number): Record<string, number> {
    return { [PARAM_PROMOTION]: promotionId };
  }

  chargerComptes(): void {
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

  private reinitialiserFormulaire(): void {
    this.login = '';
    this.nomAffiche = '';
    this.motDePasseInitial = '';
    this.etudiantIdCreation = null;
  }
}
