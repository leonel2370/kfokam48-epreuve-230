import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { erreurInterceptor } from '../../core/interceptors/erreur.interceptor';
import { FormateurComponent } from './formateur.component';

describe('FormateurComponent (SF-2)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [FormateurComponent],
      providers: [provideHttpClient(withInterceptors([erreurInterceptor])), provideHttpClientTesting(), provideRouter([])],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('ouvre une session et affiche le code renvoyé par le serveur', () => {
    const fixture = TestBed.createComponent(FormateurComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([{ id: 1, nom: 'P1-2026' }]);
    const composant = fixture.componentInstance;
    composant.titre = 'TP JPA';
    composant.ouvrir();
    const req = http.expectOne('/api/sessions');
    expect(req.request.body).toEqual({ titre: 'TP JPA', promotionId: 1 });
    req.flush({ id: 7, code: 'K7MX4Q', ouvertureAt: '2026-09-25T15:00:00Z', expirationAt: '2026-09-25T15:15:00Z' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.code')?.textContent).toBe('K7MX4Q');
    fixture.destroy();
  });

  it("affiche le message d'erreur du serveur", () => {
    const fixture = TestBed.createComponent(FormateurComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([{ id: 1, nom: 'P1-2026' }]);
    fixture.componentInstance.titre = 'TP';
    fixture.componentInstance.ouvrir();
    http.expectOne('/api/sessions').flush({ code: 'ACCES_REFUSE', message: 'Vous n’avez pas les droits.' },
      { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.alerte')?.textContent).toContain('ACCES_REFUSE');
    fixture.destroy();
  });
});
