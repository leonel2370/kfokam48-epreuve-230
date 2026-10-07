import { Component, computed, effect, inject, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ErreurApi, FicheEtudiant, Promotion, Utilisateur } from '../../../core/api/api.models';
import { ApiService } from '../../../core/api/api.service';
import { Ecriture } from '../../../core/etat/ecriture';
import { ErreurComponent } from '../../../shared/erreur/erreur.component';

/** Taille maximale d'une page côté serveur : tous les formateurs tiennent dans une seule lecture. */
const TAILLE_MAX = 100;

/** Un compte FORMATEUR et son lien avec la promotion affichée. */
interface LigneFormateur {
  compte: Utilisateur;
  rattache: boolean;
}

/**
 * Volet de gestion d'une promotion (SF-21, SF-22) : formateurs rattachés (RG26) et fiches étudiants (RG28).
 * #135 : tout ce qui est affiché vient d'une lecture du serveur (contrat 2.8) et chaque écriture est suivie
 * d'une relecture ; l'écran ne devine ni l'état d'une fiche ni la liste des rattachements.
 */
@Component({
  selector: 'app-gestion-promotion',
  standalone: true,
  imports: [FormsModule, ErreurComponent],
  templateUrl: './gestion-promotion.component.html',
})
export class GestionPromotionComponent {
  private readonly api = inject(ApiService);

  readonly promotion = input.required<Promotion>();

  readonly fiches = signal<FicheEtudiant[]>([]);
  readonly erreurFiches = signal<ErreurApi | null>(null);
  private readonly rattaches = signal<Utilisateur[]>([]);
  private readonly comptesFormateurs = signal<Utilisateur[]>([]);
  readonly erreurFormateurs = signal<ErreurApi | null>(null);
  readonly message = signal<string | null>(null);
  private readonly ecriture = new Ecriture();
  readonly enCours = this.ecriture.enCours;

  /** Formateurs actifs, plus tout formateur encore rattaché même désactivé, pour pouvoir le détacher. */
  readonly formateurs = computed<LigneFormateur[]>(() => {
    const ids = new Set(this.rattaches().map(f => f.id));
    return this.comptesFormateurs()
      .filter(c => c.actif || ids.has(c.id))
      .map(compte => ({ compte, rattache: ids.has(compte.id) }));
  });

  nomFiche = '';

  constructor() {
    // Changer de promotion repart de listes vides : rien ne fuit d'une promotion à l'autre.
    effect(() => {
      const { id } = this.promotion();
      untracked(() => this.charger(id));
    }, { allowSignalWrites: true });
  }

  basculer(ligne: LigneFormateur): void {
    const { id, nom } = this.promotion();
    const actuels = this.rattaches().map(f => f.id);
    const ids = ligne.rattache ? actuels.filter(x => x !== ligne.compte.id) : [...actuels, ligne.compte.id];
    this.ecriture.lancer(this.api.rattacherFormateurs(id, ids), this.erreurFormateurs, () => {
      this.message.set(`Formateurs de « ${nom} » enregistrés.`);
      this.chargerRattaches(id);
    });
  }

  creerFiche(): void {
    const { id, nom } = this.promotion();
    const nomFiche = this.nomFiche.trim();
    if (!nomFiche) {
      return;
    }
    this.ecriture.lancer(this.api.creerEtudiant({ nom: nomFiche, promotionId: id }), this.erreurFiches, () => {
      this.message.set(`Fiche « ${nomFiche} » créée dans « ${nom} ».`);
      this.nomFiche = '';
      this.chargerFiches(id);
    });
  }

  renommerFiche(fiche: FicheEtudiant): void {
    const nom = prompt(`Nouveau nom de l'étudiant « ${fiche.nom} » :`, fiche.nom)?.trim();
    if (!nom || nom === fiche.nom) {
      return;
    }
    const { id } = this.promotion();
    this.ecriture.lancer(this.api.modifierEtudiant(fiche.id, { nom, promotionId: id }), this.erreurFiches, () => {
      this.message.set(`Fiche renommée en « ${nom} ».`);
      this.chargerFiches(id);
    });
  }

  supprimerFiche(fiche: FicheEtudiant): void {
    if (!confirm(`Retirer la fiche de « ${fiche.nom} » ? Sans aucune trace elle est supprimée ; `
      + 'sinon elle est désactivée, avec son compte, et reste au tableau.')) {
      return;
    }
    const { id } = this.promotion();
    this.ecriture.lancer(this.api.supprimerEtudiant(fiche.id), this.erreurFiches, () => {
      this.message.set(`Fiche de « ${fiche.nom} » retirée.`);
      this.chargerFiches(id);
    });
  }

  private charger(promotionId: number): void {
    this.fiches.set([]);
    this.rattaches.set([]);
    this.message.set(null);
    this.chargerFiches(promotionId);
    this.chargerRattaches(promotionId);
    this.api.utilisateurs(0, TAILLE_MAX, 'FORMATEUR').subscribe({
      next: p => { this.comptesFormateurs.set(p.contenu); },
      error: (e: ErreurApi) => this.erreurFormateurs.set(e),
    });
  }

  private chargerFiches(promotionId: number): void {
    this.api.fiches(promotionId).subscribe({
      next: f => this.siToujoursAffichee(promotionId, () => { this.fiches.set(f); this.erreurFiches.set(null); }),
      error: (e: ErreurApi) => this.siToujoursAffichee(promotionId, () => this.erreurFiches.set(e)),
    });
  }

  private chargerRattaches(promotionId: number): void {
    this.api.formateursDe(promotionId).subscribe({
      next: f => this.siToujoursAffichee(promotionId, () => {
        this.rattaches.set(f);
        this.erreurFormateurs.set(null);
      }),
      error: (e: ErreurApi) => this.siToujoursAffichee(promotionId, () => this.erreurFormateurs.set(e)),
    });
  }

  /** Une réponse arrivée après un changement de promotion est ignorée. */
  private siToujoursAffichee(promotionId: number, action: () => void): void {
    if (this.promotion().id === promotionId) {
      action();
    }
  }
}
