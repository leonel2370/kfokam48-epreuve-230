import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { erreurInterceptor } from '../../core/interceptors/erreur.interceptor';
import { IdentiteService } from '../../core/identite/identite.service';
import { EtudiantComponent } from './etudiant.component';

describe('EtudiantComponent (SF-1, SF-3, SF-6)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      imports: [EtudiantComponent],
      providers: [provideHttpClient(withInterceptors([erreurInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => localStorage.clear());

  it("mémorise l'identité choisie dans la liste (SF-1)", () => {
    const fixture = TestBed.createComponent(EtudiantComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([{ id: 1, nom: 'P1-2026' }]);
    fixture.componentInstance.choisirPromotion(1);
    http.expectOne('/api/promotions/1/etudiants').flush([{ id: 3, nom: 'Awa Ndiaye' }]);
    fixture.componentInstance.choisirEtudiant({ id: 3, nom: 'Awa Ndiaye' });
    expect(TestBed.inject(IdentiteService).identite()).toEqual({ promotionId: 1, etudiantId: 3, nom: 'Awa Ndiaye' });
  });

  it('marque la présence puis dépose pour la session du code (SF-3, SF-6)', () => {
    TestBed.inject(IdentiteService).choisir({ promotionId: 1, etudiantId: 3, nom: 'Awa Ndiaye' });
    const fixture = TestBed.createComponent(EtudiantComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([]);
    const c = fixture.componentInstance;
    c.code = ' k7mx4q ';
    c.marquer(3);
    const presence = http.expectOne('/api/presences');
    expect(presence.request.body).toEqual({ code: 'k7mx4q', etudiantId: 3 });
    presence.flush({ id: 1, sessionId: 9, etudiantId: 3, source: 'ETUDIANT' });
    expect(c.presenceOk()).toBeTrue();
    expect(c.sessionId).toBe(9);
    c.lien = 'https://github.com/awa/tp';
    c.deposer(3);
    http.expectOne('/api/exercices').flush({ id: 4, statut: 'EN_ATTENTE_RELECTURE' });
    expect(c.depot()?.statut).toBe('EN_ATTENTE_RELECTURE');
  });

  it('affiche le message du serveur pour un code expiré', () => {
    TestBed.inject(IdentiteService).choisir({ promotionId: 1, etudiantId: 3, nom: 'Awa Ndiaye' });
    const fixture = TestBed.createComponent(EtudiantComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([]);
    fixture.componentInstance.code = 'OLD001';
    fixture.componentInstance.marquer(3);
    http.expectOne('/api/presences').flush({ code: 'CODE_EXPIRE', message: 'Le code a expiré.' },
      { status: 410, statusText: 'Gone' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.alerte')?.textContent).toContain('Le code a expiré.');
  });
});
