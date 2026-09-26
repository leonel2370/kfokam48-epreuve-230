/**
 * Seule source des adresses de l'application (spécifications §1.2, §1.2 bis — #104).
 * Les routes, les gardes, les intercepteurs et les boutons de navigation les lisent ici : aucune adresse
 * n'est écrite ailleurs dans le code.
 */
export const SEGMENTS = {
  connexion: 'connexion',
  profil: 'profil',
  admin: 'admin',
  formateur: 'formateur',
  tableau: 'tableau',
  etudiant: 'etudiant',
  presence: 'presence',
  notes: 'notes',
  relectures: 'relectures',
} as const;

export const CHEMINS = {
  connexion: `/${SEGMENTS.connexion}`,
  profil: `/${SEGMENTS.profil}`,
  admin: `/${SEGMENTS.admin}`,
  formateur: `/${SEGMENTS.formateur}`,
  etudiant: `/${SEGMENTS.etudiant}`,
  etudiantPresence: `/${SEGMENTS.etudiant}/${SEGMENTS.presence}`,
  etudiantNotes: `/${SEGMENTS.etudiant}/${SEGMENTS.notes}`,
  etudiantRelectures: `/${SEGMENTS.etudiant}/${SEGMENTS.relectures}`,
} as const;

/** Paramètre de route du tableau et paramètre d'adresse qui présélectionne une promotion côté formateur. */
export const PARAM_PROMOTION = 'promotionId';

/** Tableau d'une promotion (SF-10). */
export function cheminTableau(promotionId: number): string {
  return `${CHEMINS.formateur}/${SEGMENTS.tableau}/${promotionId}`;
}

/** Une entrée de menu : un bouton qui mène à un chemin, avec d'éventuels paramètres d'adresse. */
export interface EntreeNavigation {
  chemin: string;
  libelle: string;
  parametres?: Record<string, string | number>;
}
