import { Injectable, signal } from '@angular/core';

/** Champ unique d'un dialogue de saisie (renommer, mot de passe provisoire). */
export interface ChampDialogue {
  libelle: string;
  type: 'text' | 'password';
  valeur?: string;
  longueurMax?: number;
  aide?: string;
}

export interface DemandeDialogue {
  titre: string;
  message?: string;
  /** Libellé du bouton qui valide, par exemple « Désactiver ». */
  confirmer: string;
  /** Action de retrait ou définitive : le bouton prend la couleur d'alerte. */
  danger?: boolean;
  champ?: ChampDialogue;
}

/**
 * Dialogues de l'écran (DESIGN_SYSTEM Dialog, #138) : remplacent confirm, prompt et alert du navigateur,
 * qui ne suivent pas le design, sont mal annoncés aux lecteurs d'écran et affichent un mot de passe en clair.
 * Le composant app-dialogue, placé une fois à la racine, affiche la demande courante.
 */
@Injectable({ providedIn: 'root' })
export class DialogueService {
  private readonly courante = signal<DemandeDialogue | null>(null);
  readonly demande = this.courante.asReadonly();
  private repondre: ((valeur: string | null) => void) | null = null;

  /** Demande une confirmation : vrai si l'utilisateur confirme, faux s'il annule. */
  confirmer(demande: Omit<DemandeDialogue, 'champ'>): Promise<boolean> {
    return this.ouvrir(demande).then(valeur => valeur !== null);
  }

  /** Demande une saisie : la valeur saisie, ou null si l'utilisateur annule. */
  saisir(demande: DemandeDialogue & { champ: ChampDialogue }): Promise<string | null> {
    return this.ouvrir(demande);
  }

  /** Appelé par app-dialogue : null pour une annulation, la valeur (ou une chaîne vide) pour une validation. */
  fermer(valeur: string | null): void {
    const repondre = this.repondre;
    this.repondre = null;
    this.courante.set(null);
    repondre?.(valeur);
  }

  private ouvrir(demande: DemandeDialogue): Promise<string | null> {
    this.fermer(null);
    return new Promise(resolve => {
      this.repondre = resolve;
      this.courante.set(demande);
    });
  }
}
