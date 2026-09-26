import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { FOURNISSEURS_TEST, PROFILS } from '../../testing';
import { PresenceComponent } from './presence.component';

const SESSION = { id: 9, titre: 'TP JPA', promotionId: 1, code: null, ouvertureAt: '2026-09-25T15:00:00Z',
  expirationAt: '2026-09-25T15:15:00Z', statut: 'OUVERTE', clotureAt: null };
const CLOTUREE = { ...SESSION, id: 4, titre: 'TP Flyway', statut: 'CLOTUREE' };

describe('PresenceComponent — écran étudiant (HYP-15, SF-3, SF-6)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [PresenceComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).profil.set(PROFILS.awa);
  });

  function ouvrir() {
    const fixture = TestBed.createComponent(PresenceComponent);
    fixture.detectChanges();
    http.expectOne('/api/sessions?promotionId=1').flush([SESSION, CLOTUREE]);
    return fixture;
  }

  it("prend l'identité du compte connecté, sans liste de noms, et ne propose que les sessions ouvertes", () => {
    const fixture = ouvrir();
    expect(fixture.componentInstance.etudiantId()).toBe(1);
    expect(fixture.componentInstance.sessionsOuvertes().map(s => s.id)).toEqual([9]);
    expect((fixture.nativeElement as HTMLElement).querySelector('select[name="etudiant"]')).toBeNull();
  });

  it('#107 recharge la liste des sessions après une présence réussie et sélectionne la session du code', () => {
    const c = ouvrir().componentInstance;
    c.code = 'k7mx4q';
    c.marquer(1);
    http.expectOne('/api/presences').flush({ id: 1, sessionId: 9, etudiantId: 1, source: 'ETUDIANT' });
    // La liste est rechargée après le succès : le formateur a ouvert la session pendant que la page était ouverte.
    http.expectOne('/api/sessions?promotionId=1').flush([SESSION]);
    expect(c.sessionsOuvertes().map(s => s.id)).toContain(9);
    expect(c.sessionId).toBe(9);
  });

  it('#107 recharge aussi la liste des sessions après un dépôt réussi', () => {
    const c = ouvrir().componentInstance;
    c.sessionId = 9;
    c.lien = 'https://github.com/awa/tp';
    c.deposer(1);
    http.expectOne('/api/exercices').flush({ id: 4, statut: 'EN_ATTENTE_RELECTURE' });
    http.expectOne('/api/sessions?promotionId=1').flush([SESSION]);
    expect(c.depot()?.statut).toBe('EN_ATTENTE_RELECTURE');
  });

  it("#107 l'erreur de chargement des sessions a sa propre zone, distincte de celle du dépôt", () => {
    const fixture = TestBed.createComponent(PresenceComponent);
    fixture.detectChanges();
    http.expectOne('/api/sessions?promotionId=1').flush({ code: 'ERREUR_INTERNE', message: 'Base injoignable.' },
      { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    // Trois zones d'erreur distinctes : chargement des sessions, présence, dépôt.
    expect(el.querySelectorAll('app-erreur').length).toBe(3);
    expect(el.textContent).toContain('Base injoignable.');
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

  it('#106 — le bouton Valider est désactivé pendant la requête de présence (pas de double envoi)', () => {
    const fixture = ouvrir();
    const c = fixture.componentInstance;
    c.code = 'k7mx4q';
    c.marquer(1);
    fixture.detectChanges();
    const bouton = (fixture.nativeElement as HTMLElement).querySelector('form button[type="submit"]') as HTMLButtonElement;
    expect(bouton.disabled).withContext('pendant la requête').toBeTrue();
    http.expectOne('/api/presences').flush({ id: 1, sessionId: 9, etudiantId: 1, source: 'ETUDIANT' });
    fixture.detectChanges();
    expect(bouton.disabled).withContext('après la requête').toBeFalse();
  });

  it('#106 — le bouton Déposer est désactivé pendant la requête de dépôt (pas de double envoi)', () => {
    const fixture = ouvrir();
    const c = fixture.componentInstance;
    c.sessionId = 9;
    c.lien = 'https://github.com/awa/tp';
    c.deposer(1);
    fixture.detectChanges();
    const boutons = (fixture.nativeElement as HTMLElement).querySelectorAll('form button[type="submit"]');
    expect((boutons[1] as HTMLButtonElement).disabled).withContext('pendant la requête').toBeTrue();
    http.expectOne('/api/exercices').flush({ id: 4, statut: 'DEPOSE' });
    fixture.detectChanges();
    expect((boutons[1] as HTMLButtonElement).disabled).withContext('après la requête').toBeFalse();
  });
});
