/** Types alignés sur api/contrat.yaml (v2.0). Aucune règle métier côté client (F3). */

export interface ErreurApi {
  code: string;
  message: string;
}

export interface Promotion {
  id: number;
  nom: string;
}

export interface Etudiant {
  id: number;
  nom: string;
}

export type StatutSession = 'OUVERTE' | 'CLOTUREE';

export interface Session {
  id: number;
  titre: string;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
  statut: StatutSession;
}

export interface SessionOuverte {
  id: number;
  code: string;
  ouvertureAt: string;
  expirationAt: string;
}

export type SourcePresence = 'ETUDIANT' | 'FORMATEUR';

export interface Presence {
  id: number;
  sessionId: number;
  etudiantId: number;
  source: SourcePresence;
}

export type StatutExercice = 'DEPOSE' | 'EN_ATTENTE_RELECTURE' | 'RELU';

export interface ExerciceDepose {
  id: number;
  statut: StatutExercice;
}

export interface RelectureRelecteur {
  id: number;
  exerciceId: number;
  sessionTitre: string;
  lien: string;
  rendue: boolean;
  note: number | null;
  commentaire: string | null;
}

export interface LigneTableau {
  etudiantId: number;
  nom: string;
  presences: number;
  exercicesDeposes: number;
  moyenne: number | null;
  relecturesEnAttente: number;
}

export type Role = 'ADMIN' | 'FORMATEUR' | 'ETUDIANT';

/** v2 : utilisateur connecté (GET /api/moi, POST /api/auth/login). */
export interface Profil {
  id: number;
  login: string;
  nomAffiche: string;
  role: Role;
  etudiantId: number | null;
  promotionIds: number[];
  doitChangerMotDePasse: boolean;
}
