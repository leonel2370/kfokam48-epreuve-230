import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { erreurInterceptor } from '../../core/interceptors/erreur.interceptor';
import { RelecteurComponent } from './relecteur.component';

const PAUL = { id: 5, login: 'paul', nomAffiche: 'Paul Mbarga', role: 'ETUDIANT', etudiantId: 2, promotionIds: [1],
  doitChangerMotDePasse: false };

describe('RelecteurComponent (SF-8, SF-9)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [RelecteurComponent],
      providers: [provideHttpClient(withInterceptors([erreurInterceptor])), provideHttpClientTesting()],
    });
    http = TestBed.inject(HttpTestingController);
  });

  it('se connecte, liste les relectures à faire puis rend une note après confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    const fixture = TestBed.createComponent(RelecteurComponent);
    const c = fixture.componentInstance;
    c.login = 'paul';
    c.motDePasse = 'Etudiant48';
    c.connecter();
    http.expectOne('/api/auth/login').flush(PAUL);
    const relecture = { id: 7, exerciceId: 3, sessionTitre: 'TP JPA', lien: 'https://x.cm', rendue: false,
      note: null, commentaire: null };
    http.expectOne('/api/etudiants/2/relectures?statut=A_FAIRE').flush([relecture]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('TP JPA');
    c.notes[7] = 15;
    c.commentaires[7] = ' Clair ';
    c.rendre(relecture, c.profil()!);
    const post = http.expectOne('/api/relectures/7');
    expect(post.request.headers.get('X-Etudiant-Id')).toBe('2');
    expect(post.request.body).toEqual({ note: 15, commentaire: 'Clair' });
    post.flush(null);
    http.expectOne('/api/etudiants/2/relectures?statut=A_FAIRE').flush([]);
    expect(c.message()).toBe('Relecture envoyée.');
  });

  it("n'envoie rien si l'envoi définitif n'est pas confirmé (RG10)", () => {
    spyOn(window, 'confirm').and.returnValue(false);
    const c = TestBed.createComponent(RelecteurComponent).componentInstance;
    c.rendre({ id: 7, exerciceId: 3, sessionTitre: 'TP', lien: 'https://x.cm', rendue: false, note: null,
      commentaire: null }, { ...PAUL, role: 'ETUDIANT' });
    http.expectNone('/api/relectures/7');
  });

  it("affiche l'erreur de connexion du serveur", () => {
    const fixture = TestBed.createComponent(RelecteurComponent);
    fixture.componentInstance.login = 'paul';
    fixture.componentInstance.motDePasse = 'faux';
    fixture.componentInstance.connecter();
    http.expectOne('/api/auth/login').flush({ code: 'IDENTIFIANTS_INVALIDES', message: 'Identifiants invalides.' },
      { status: 401, statusText: 'Unauthorized' });
    expect(fixture.componentInstance.erreur()?.code).toBe('IDENTIFIANTS_INVALIDES');
  });
});
