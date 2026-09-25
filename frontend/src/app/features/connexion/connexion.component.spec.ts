import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { FOURNISSEURS_TEST, PROFILS } from '../../testing';
import { ConnexionComponent } from './connexion.component';

describe('ConnexionComponent (SF-15)', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ConnexionComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
  });

  it("redirige chacun vers l'espace de son rôle", () => {
    const c = TestBed.createComponent(ConnexionComponent).componentInstance;
    c.login = 'formateur';
    c.motDePasse = 'Formateur48';
    c.connecter();
    const req = http.expectOne('/api/auth/login');
    expect(req.request.body).toEqual({ login: 'formateur', motDePasse: 'Formateur48' });
    req.flush(PROFILS.formateur);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/formateur');
  });

  it('envoie l’admin par défaut changer son mot de passe (RG23)', () => {
    const c = TestBed.createComponent(ConnexionComponent).componentInstance;
    c.login = 'admin';
    c.motDePasse = 'admin';
    c.connecter();
    http.expectOne('/api/auth/login').flush(PROFILS.admin);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/profil');
  });

  it('affiche le refus du serveur et vide le mot de passe', () => {
    const fixture = TestBed.createComponent(ConnexionComponent);
    const c = fixture.componentInstance;
    c.login = 'paul';
    c.motDePasse = 'faux';
    c.connecter();
    http.expectOne('/api/auth/login').flush({ code: 'IDENTIFIANTS_INVALIDES', message: 'Identifiants invalides.' },
      { status: 401, statusText: 'Unauthorized' });
    fixture.detectChanges();
    expect(c.motDePasse).toBe('');
    expect((fixture.nativeElement as HTMLElement).querySelector('.alerte')?.textContent).toContain('Identifiants invalides.');
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
