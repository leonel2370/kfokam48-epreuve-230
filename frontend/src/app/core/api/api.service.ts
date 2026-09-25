import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  Etudiant,
  ExerciceDepose,
  LigneTableau,
  Presence,
  Promotion,
  RelectureRelecteur,
  Session,
  SessionOuverte,
} from './api.models';

/** Seul point d'accès HTTP de l'application : une méthode par opération du contrat. */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly url = environment.apiUrl;

  promotions(): Observable<Promotion[]> {
    return this.http.get<Promotion[]>(`${this.url}/promotions`);
  }

  etudiants(promotionId: number): Observable<Etudiant[]> {
    return this.http.get<Etudiant[]>(`${this.url}/promotions/${promotionId}/etudiants`);
  }

  /** Imposé (EF2). */
  ouvrirSession(titre: string, promotionId: number): Observable<SessionOuverte> {
    return this.http.post<SessionOuverte>(`${this.url}/sessions`, { titre, promotionId });
  }

  sessions(promotionId: number): Observable<Session[]> {
    return this.http.get<Session[]>(`${this.url}/sessions`, { params: { promotionId } });
  }

  /** Imposé (EF3). */
  marquerPresence(code: string, etudiantId: number): Observable<Presence> {
    return this.http.post<Presence>(`${this.url}/presences`, { code, etudiantId });
  }

  /** Imposé (EF6). */
  deposerExercice(sessionId: number, etudiantId: number, lien: string): Observable<ExerciceDepose> {
    return this.http.post<ExerciceDepose>(`${this.url}/exercices`, { sessionId, etudiantId, lien });
  }

  relectures(etudiantId: number, statut?: 'A_FAIRE' | 'RENDUE'): Observable<RelectureRelecteur[]> {
    const params: Record<string, string> = statut ? { statut } : {};
    return this.http.get<RelectureRelecteur[]>(`${this.url}/etudiants/${etudiantId}/relectures`, { params });
  }

  /** Imposé (EF9) : l'identité passe par l'en-tête X-Etudiant-Id (HYP-2). */
  rendreRelecture(id: number, etudiantId: number, note: number, commentaire: string): Observable<void> {
    const headers = new HttpHeaders({ 'X-Etudiant-Id': String(etudiantId) });
    return this.http.post<void>(`${this.url}/relectures/${id}`, { note, commentaire }, { headers });
  }

  /** Imposé (EF10) : les agrégats viennent du serveur, jamais recalculés ici (F3). */
  tableau(promotionId: number): Observable<LigneTableau[]> {
    return this.http.get<LigneTableau[]>(`${this.url}/tableau`, { params: { promotionId } });
  }
}
