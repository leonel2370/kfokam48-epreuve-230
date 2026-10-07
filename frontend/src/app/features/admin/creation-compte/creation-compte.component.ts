import { Component, inject, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, FicheEtudiant, Promotion, Role, Utilisateur } from '../../../core/api/api.models';
import { ApiService } from '../../../core/api/api.service';
import { LIBELLES_ROLE } from '../../../core/navigation/libelles';
import { ErreurComponent } from '../../../shared/erreur/erreur.component';

const ROLES: readonly Role[] = ['ETUDIANT', 'FORMATEUR', 'ADMIN'];

/**
 * Création d'un compte par l'ADMIN (SF-20, EF21). #136 : pour un compte étudiant, la fiche se choisit
 * dans une liste — promotion, puis fiche active sans compte (HYP-15) — et non par son numéro interne.
 * Le mot de passe est provisoire (RG23) ; sa solidité est jugée par le serveur (RG24).
 */
@Component({
  selector: 'app-creation-compte',
  standalone: true,
  imports: [FormsModule, ErreurComponent],
  templateUrl: './creation-compte.component.html',
})
export class CreationCompteComponent {
  private readonly api = inject(ApiService);

  readonly promotions = input.required<Promotion[]>();
  /** Émis après une création réussie, pour que l'écran relise la liste des comptes. */
  readonly cree = output<Utilisateur>();

  readonly roles = ROLES;
  readonly libelles = LIBELLES_ROLE;
  readonly fiches = signal<FicheEtudiant[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);
  readonly message = signal<string | null>(null);
  readonly enCours = signal(false);

  login = '';
  nomAffiche = '';
  role: Role = 'ETUDIANT';
  motDePasseInitial = '';
  promotionId: number | null = null;
  ficheId: number | null = null;

  /** Fiches proposées : celles de la promotion choisie qui peuvent recevoir un compte. */
  choisirPromotion(promotionId: number | null): void {
    this.promotionId = promotionId;
    this.ficheId = null;
    this.fiches.set([]);
    if (promotionId === null) {
      return;
    }
    this.api.fiches(promotionId).subscribe({
      next: f => {
        if (this.promotionId === promotionId) {
          this.fiches.set(f.filter(fiche => fiche.actif && fiche.compteLogin === null));
        }
      },
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }

  /** Le nom affiché reprend celui de la fiche choisie, tant que l'administrateur n'a rien saisi. */
  choisirFiche(ficheId: number | null): void {
    this.ficheId = ficheId;
    const fiche = this.fiches().find(f => f.id === ficheId);
    if (fiche && !this.nomAffiche.trim()) {
      this.nomAffiche = fiche.nom;
    }
  }

  complet(): boolean {
    const fiche = this.role !== 'ETUDIANT' || this.ficheId !== null;
    return fiche && this.login.trim() !== '' && this.nomAffiche.trim() !== '' && this.motDePasseInitial !== '';
  }

  creer(): void {
    if (!this.complet() || this.enCours()) {
      return;
    }
    this.enCours.set(true);
    this.erreur.set(null);
    this.message.set(null);
    this.api.creerUtilisateur({
      login: this.login.trim(),
      nomAffiche: this.nomAffiche.trim(),
      role: this.role,
      motDePasseInitial: this.motDePasseInitial,
      etudiantId: this.role === 'ETUDIANT' ? this.ficheId : null,
    }).subscribe({
      next: u => {
        this.enCours.set(false);
        this.message.set(`Compte « ${u.login} » créé ; il devra changer son mot de passe à la première connexion.`);
        this.vider();
        this.cree.emit(u);
      },
      error: (e: ErreurApi) => {
        this.enCours.set(false);
        this.erreur.set(e);
      },
    });
  }

  private vider(): void {
    this.login = '';
    this.nomAffiche = '';
    this.motDePasseInitial = '';
    this.choisirPromotion(null);
  }
}
