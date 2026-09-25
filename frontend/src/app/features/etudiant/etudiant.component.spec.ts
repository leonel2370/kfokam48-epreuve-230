import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { FOURNISSEURS_TEST, PROFILS } from '../../testing';
import { EtudiantComponent } from './etudiant.component';

const SESSION = { id: 9, titre: 'TP JPA', promotionId: 1, code: null, ouvertureAt: '2026-09-25T15:00:00Z',
  expirationAt: '2026-09-25T15:15:00Z', statut: 'OUVERTE', clotureAt: null };
const CLOTUREE = { ...SESSION, id: 4, titre: 'TP Flyway', statut: 'CLOTUREE' };

describe('EtudiantComponent (HYP-15, SF-3, SF-6)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [EtudiantComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).profil.set(PROFILS.awa);
  });

  function ouvrir() {
    const fixture = TestBed.createComponent(EtudiantComponent);
    fixture.detectChanges();
    http.expectOne('/api/sessions?promotionId=1').flush([SESSION, CLOTUREE]);
    http.expectOne('/api/etudiants/1/exercices').flush([]);
    http.expectOne('/api/etudiants/1/relectures?statut=A_FAIRE').flush([]);
    return fixture;
  }

  it("prend l'identité du compte connecté, sans liste de noms, et ne propose que les sessions ouvertes", () => {
    const fixture = ouvrir();
    expect(fixture.componentInstance.etudiantId()).toBe(1);
    expect(fixture.componentInstance.sessionsOuvertes().map(s => s.id)).toEqual([9]);
    expect((fixture.nativeElement as HTMLElement).querySelector('select[name="etudiant"]')).toBeNull();
  });

  it('marque la présence puis dépose pour la session du code', () => {
    const c = ouvrir().componentInstance;
    c.code = ' k7mx4q ';
    c.marquer(1);
    const presence = http.expectOne('/api/presences');
    expect(presence.request.body).toEqual({ code: 'k7mx4q', etudiantId: 1 });
    presence.flush({ id: 1, sessionId: 9, etudiantId: 1, source: 'ETUDIANT' });
    expect(c.sessionId).toBe(9);
    c.lien = 'https://github.com/awa/tp';
    c.deposer(1);
    http.expectOne('/api/exercices').flush({ id: 4, statut: 'EN_ATTENTE_RELECTURE' });
    http.expectOne('/api/etudiants/1/exercices').flush([]);
    expect(c.depot()?.statut).toBe('EN_ATTENTE_RELECTURE');
  });

  it('affiche le message du serveur pour un code expiré', () => {
    const fixture = ouvrir();
    fixture.componentInstance.code = 'OLD001';
    fixture.componentInstance.marquer(1);
    http.expectOne('/api/presences').flush({ code: 'CODE_EXPIRE', message: 'Le code a expiré.' },
      { status: 410, statusText: 'Gone' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.alerte')?.textContent).toContain('Le code a expiré.');
  });
});
