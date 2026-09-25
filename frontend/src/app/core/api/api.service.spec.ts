import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ErreurApi } from './api.models';
import { ApiService } from './api.service';
import { erreurInterceptor } from '../interceptors/erreur.interceptor';

describe('ApiService (contrat v2.0)', () => {
  let api: ApiService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([erreurInterceptor])), provideHttpClientTesting()],
    });
    api = TestBed.inject(ApiService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('appelle les opérations imposées avec les corps du contrat', () => {
    api.ouvrirSession('TP', 1).subscribe();
    const session = http.expectOne('/api/sessions');
    expect(session.request.method).toBe('POST');
    expect(session.request.body).toEqual({ titre: 'TP', promotionId: 1 });
    session.flush({});

    api.marquerPresence('K7MX4Q', 3).subscribe();
    expect(http.expectOne('/api/presences').request.body).toEqual({ code: 'K7MX4Q', etudiantId: 3 });

    api.deposerExercice(5, 3, 'https://x.cm').subscribe();
    expect(http.expectOne('/api/exercices').request.body).toEqual({ sessionId: 5, etudiantId: 3, lien: 'https://x.cm' });

    api.tableau(1).subscribe();
    expect(http.expectOne('/api/tableau?promotionId=1').request.method).toBe('GET');
  });

  it("envoie l'identité du relecteur dans l'en-tête X-Etudiant-Id", () => {
    api.rendreRelecture(9, 3, 15, 'ok').subscribe();
    const req = http.expectOne('/api/relectures/9');
    expect(req.request.headers.get('X-Etudiant-Id')).toBe('3');
    expect(req.request.body).toEqual({ note: 15, commentaire: 'ok' });
  });

  it('transmet l’erreur { code, message } du serveur telle quelle', () => {
    let recue: ErreurApi | undefined;
    api.marquerPresence('XXX', 3).subscribe({ error: (e: ErreurApi) => (recue = e) });
    http.expectOne('/api/presences').flush({ code: 'CODE_EXPIRE', message: 'Le code a expiré.' },
      { status: 410, statusText: 'Gone' });
    expect(recue).toEqual({ code: 'CODE_EXPIRE', message: 'Le code a expiré.' });
  });

  it('remplace une erreur sans corps par un message générique', () => {
    let recue: ErreurApi | undefined;
    api.promotions().subscribe({ error: (e: ErreurApi) => (recue = e) });
    http.expectOne('/api/promotions').error(new ProgressEvent('error'), { status: 0 });
    expect(recue?.code).toBe('SERVEUR_INJOIGNABLE');
  });
});
