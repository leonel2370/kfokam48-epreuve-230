import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { FOURNISSEURS_TEST, PROFILS } from '../../testing';
import { FormateurComponent } from './formateur.component';

const P1 = { id: 1, nom: 'P1-2026' };
const P2 = { id: 2, nom: 'P2-2026' };

describe('FormateurComponent (SF-2, RG26)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [FormateurComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).profil.set(PROFILS.formateur);
  });

  it('ne propose que les promotions du formateur, liste leurs sessions et affiche le nouveau code', () => {
    const fixture = TestBed.createComponent(FormateurComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([P1, P2]);
    expect(fixture.componentInstance.promotions()).toEqual([P1]);
    http.expectOne('/api/sessions?promotionId=1').flush([]);
    const c = fixture.componentInstance;
    c.titre = 'TP JPA';
    c.ouvrir();
    const req = http.expectOne('/api/sessions');
    expect(req.request.body).toEqual({ titre: 'TP JPA', promotionId: 1 });
    req.flush({ id: 7, code: 'K7MX4Q', ouvertureAt: '2026-09-25T15:00:00Z', expirationAt: '2026-09-25T15:15:00Z' });
    http.expectOne('/api/sessions?promotionId=1').flush([{ id: 7, titre: 'TP JPA', promotionId: 1, code: 'K7MX4Q',
      ouvertureAt: '2026-09-25T15:00:00Z', expirationAt: '2026-09-25T15:15:00Z', statut: 'OUVERTE', clotureAt: null }]);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('.code')?.textContent).toBe('K7MX4Q');
    expect(page.querySelector('tbody')?.textContent).toContain('TP JPA');
    fixture.destroy();
  });

  it("affiche le message d'erreur du serveur", () => {
    const fixture = TestBed.createComponent(FormateurComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([P1]);
    http.expectOne('/api/sessions?promotionId=1').flush([]);
    fixture.componentInstance.titre = 'TP';
    fixture.componentInstance.ouvrir();
    http.expectOne('/api/sessions').flush({ code: 'ACCES_REFUSE', message: 'Vous n’avez pas les droits.' },
      { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('.alerte')?.textContent).toContain('ACCES_REFUSE');
    fixture.destroy();
  });
});
