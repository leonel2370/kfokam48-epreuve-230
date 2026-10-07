import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, PageUtilisateurs, Role, Utilisateur } from '../../../core/api/api.models';
import { ApiService } from '../../../core/api/api.service';
import { DialogueService } from '../../../core/dialogue/dialogue.service';
import { Ecriture } from '../../../core/etat/ecriture';
import { Lecture } from '../../../core/etat/lecture';
import { LIBELLES_ROLE } from '../../../core/libelles/libelles';
import { BadgeComponent } from '../../../shared/badge/badge.component';
import { ErreurComponent } from '../../../shared/erreur/erreur.component';
import { EtatListeComponent } from '../../../shared/etat-liste/etat-liste.component';
import { PaginationComponent } from '../../../shared/pagination/pagination.component';

const AUCUN_COMPTE: PageUtilisateurs = { contenu: [], page: 0, taille: 0, total: 0 };
/** Rôles qu'un compte sans fiche étudiant peut prendre (un compte ETUDIANT exige une fiche, HYP-15). */
const ROLES_SANS_FICHE: readonly Role[] = ['FORMATEUR', 'ADMIN'];

/**
 * Liste des comptes (SF-20, EF21), réservée à l'ADMIN : désactiver (RG28), réactiver, changer le rôle,
 * donner un mot de passe provisoire (RG23). La page affichée est celle que le serveur a rendue, et la
 * liste est relue après chaque action : le serveur reste seul juge (dernier administrateur, RG28).
 */
@Component({
  selector: 'app-liste-comptes',
  standalone: true,
  imports: [FormsModule, BadgeComponent, ErreurComponent, EtatListeComponent, PaginationComponent],
  templateUrl: './liste-comptes.component.html',
})
export class ListeComptesComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly dialogue = inject(DialogueService);

  readonly libelles = LIBELLES_ROLE;
  readonly rolesSansFiche = ROLES_SANS_FICHE;
  readonly ecriture = new Ecriture();
  readonly lecture = new Lecture<PageUtilisateurs>(AUCUN_COMPTE);
  readonly comptes = computed(() => this.lecture.donnees().contenu);
  readonly total = computed(() => this.lecture.donnees().total);
  readonly page = computed(() => this.lecture.donnees().page);
  readonly aUneSuite = computed(() => {
    const { page, taille, total } = this.lecture.donnees();
    return (page + 1) * taille < total;
  });
  readonly erreur = signal<ErreurApi | null>(null);
  readonly message = signal<string | null>(null);

  ngOnInit(): void {
    this.charger(0);
  }

  /** Relit la page affichée : appelé par l'écran quand un compte vient d'être créé. */
  recharger(): void {
    this.charger(this.page());
  }

  charger(page: number): void {
    this.lecture.charger(this.api.utilisateurs(page));
  }

  async desactiver(u: Utilisateur): Promise<void> {
    const confirme = await this.dialogue.confirmer({
      titre: `Désactiver le compte « ${u.login} » ?`,
      message: 'Il ne pourra plus se connecter. Son historique est conservé.',
      confirmer: 'Désactiver',
      danger: true,
    });
    if (confirme) {
      this.agir(this.api.desactiverUtilisateur(u.id), `Compte « ${u.login} » désactivé.`);
    }
  }

  reactiver(u: Utilisateur): void {
    this.modifier(u, { actif: true }, `Compte « ${u.login} » réactivé.`);
  }

  changerRole(u: Utilisateur, role: Role): void {
    if (role !== u.role) {
      this.modifier(u, { role }, `« ${u.login} » est maintenant ${this.libelles[role]}.`);
    }
  }

  async reinitialiser(u: Utilisateur): Promise<void> {
    const provisoire = await this.dialogue.saisir({
      titre: `Mot de passe provisoire de « ${u.login} »`,
      confirmer: 'Réinitialiser',
      champ: { libelle: 'Mot de passe (8 caractères minimum)', type: 'password',
        aide: 'Il devra le changer à sa prochaine connexion.' },
    });
    if (provisoire) {
      this.agir(this.api.reinitialiserMotDePasse(u.id, provisoire),
        `Mot de passe de « ${u.login} » réinitialisé ; à changer à la prochaine connexion.`);
    }
  }

  /** Le contrat demande le compte entier : seuls les champs modifiés changent. */
  private modifier(u: Utilisateur, changement: Partial<Pick<Utilisateur, 'role' | 'actif'>>, succes: string): void {
    this.agir(this.api.modifierUtilisateur(u.id, {
      nomAffiche: u.nomAffiche, role: u.role, actif: u.actif, etudiantId: u.etudiantId, ...changement,
    }), succes);
  }

  private agir(requete: Parameters<Ecriture['lancer']>[0], succes: string): void {
    this.message.set(null);
    this.ecriture.lancer(requete, this.erreur, () => {
      this.message.set(succes);
      this.recharger();
    });
  }
}
