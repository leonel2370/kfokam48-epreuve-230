import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FOURNISSEURS_TEST } from '../../testing';
import { RelecturesComponent } from './relectures.component';

const RELECTURE = { id: 7, exerciceId: 3, sessionTitre: 'TP JPA', lien: 'https://x.cm', rendue: false,
  note: null, commentaire: null };

describe('RelecturesComponent (SF-8, SF-9)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [RelecturesComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
  });

  function ouvrir() {
    const fixture = TestBed.createComponent(RelecturesComponent);
    fixture.componentRef.setInput('etudiantId', 2);
    fixture.detectChanges();
    http.expectOne('/api/etudiants/2/relectures?statut=A_FAIRE').flush([RELECTURE]);
    fixture.detectChanges();
    return fixture;
  }

  it('liste les relectures à faire et rend une note après confirmation', () => {
    spyOn(window, 'confirm').and.returnValue(true);
    const fixture = ouvrir();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('TP JPA');
    const c = fixture.componentInstance;
    c.notes[7] = 15;
    c.commentaires[7] = ' Clair ';
    c.rendre(RELECTURE);
    const post = http.expectOne('/api/relectures/7');
    expect(post.request.headers.get('X-Etudiant-Id')).toBe('2');
    expect(post.request.body).toEqual({ note: 15, commentaire: 'Clair' });
    post.flush(null);
    http.expectOne('/api/etudiants/2/relectures?statut=A_FAIRE').flush([]);
    expect(c.message()).toBe('Relecture envoyée.');
  });

  it("n'envoie rien si l'envoi définitif n'est pas confirmé (RG10)", () => {
    spyOn(window, 'confirm').and.returnValue(false);
    ouvrir().componentInstance.rendre(RELECTURE);
    http.expectNone('/api/relectures/7');
  });
});
