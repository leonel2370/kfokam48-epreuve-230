/**
 * Libellés français des énumérations affichées (#108, DESIGN_SYSTEM Badge) :
 * une seule source, comme les chemins — plus aucun code brut (CLOTUREE, EN_ATTENTE_RELECTURE…)
 * n'est montré à l'écran. La logique reste côté serveur (F3) : ceci ne traduit que l'affichage.
 */
export const LIBELLES_ROLE: Record<string, string> = {
  ADMIN: 'Administrateur',
  FORMATEUR: 'Formateur',
  ETUDIANT: 'Étudiant',
};

export const LIBELLES_STATUT_EXERCICE: Record<string, string> = {
  DEPOSE: 'Déposé',
  EN_ATTENTE_RELECTURE: 'En attente de relecture',
  RELU: 'Relu',
};

export const LIBELLES_STATUT_SESSION: Record<string, string> = {
  OUVERTE: 'Ouverte',
  CLOTUREE: 'Clôturée',
};

export function libelleRole(role: string): string {
  return LIBELLES_ROLE[role] ?? role;
}
