import { Role, StatutExercice, StatutSession } from '../api/api.models';

/**
 * Libellés français des énumérations affichées (#108, DESIGN_SYSTEM Badge) : une seule source, typée sur
 * les énumérations du contrat (#138) — ajouter une valeur au contrat sans son libellé ne compile pas.
 * La logique reste côté serveur (F3) : ceci ne traduit que l'affichage.
 */
export const LIBELLES_ROLE: Readonly<Record<Role, string>> = {
  ADMIN: 'Administrateur',
  FORMATEUR: 'Formateur',
  ETUDIANT: 'Étudiant',
};

export const LIBELLES_STATUT_EXERCICE: Readonly<Record<StatutExercice, string>> = {
  DEPOSE: 'Déposé',
  EN_ATTENTE_RELECTURE: 'En attente de relecture',
  RELU: 'Relu',
};

export const LIBELLES_STATUT_SESSION: Readonly<Record<StatutSession, string>> = {
  OUVERTE: 'Ouverte',
  CLOTUREE: 'Clôturée',
};

export function libelleRole(role: Role): string {
  return LIBELLES_ROLE[role];
}

/** Ton d'un badge (shared/badge) : la couleur accompagne le libellé, elle ne le remplace jamais (WCAG). */
export type TonBadge = 'neutre' | 'succes' | 'attente' | 'inactif';

export const TONS_STATUT_EXERCICE: Readonly<Record<StatutExercice, TonBadge>> = {
  DEPOSE: 'neutre',
  EN_ATTENTE_RELECTURE: 'attente',
  RELU: 'succes',
};

export const TONS_STATUT_SESSION: Readonly<Record<StatutSession, TonBadge>> = {
  OUVERTE: 'neutre',
  CLOTUREE: 'inactif',
};
