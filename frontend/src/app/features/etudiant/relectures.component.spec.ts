import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed, discardPeriodicTasks, fakeAsync, flushMicrotasks, tick } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { DialogueService } from '../../core/dialogue/dialogue.service';
import { FOURNISSEURS_TEST, PROFILS } from '../../testing';
import { RAFRAICHISSEMENT_MS, RelecturesComponent } from './relectures.component';

const A_FAIRE = '/api/etudiants/2/relectures?statut=A_FAIRE';
const RENDUES = '/api/etudiants/2/relectures?statut=RENDUE';
const RELECTURE = { id: 7, exerciceId: 3, sessionTitre: 'TP JPA', lien: 'https://x.cm', rendue: false,
  note: null, commentaire: null };
const RENDUE = { id: 5, exerciceId: 2, sessionTitre: 'TP Flyway', lien: 'https://y.cm', rendue: true,
  note: 14, commentaire: 'Bon découpage' };

describe('RelecturesComponent (SF-8, SF-9, #101)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [RelecturesComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).profil.set({ ...PROFILS.awa, etudiantId: 2 });
  });

  function ouvrir(aFaire = [RELECTURE], rendues = [RENDUE]) {
    const fixture = TestBed.createComponent(RelecturesComponent);
    fixture.detectChanges();
    http.expectOne(A_FAIRE).flush(aFaire);
    http.expectOne(RENDUES).flush(rendues);
    fixture.detectChanges();
    return fixture;
  }

  it('liste les relectures à faire et rend une note après confirmation', async () => {
    spyOn(TestBed.inject(DialogueService), 'confirmer').and.resolveTo(true);
    const fixture = ouvrir();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('TP JPA');
    const c = fixture.componentInstance;
    c.notes[7] = 15;
    c.commentaires[7] = ' Clair ';
    await c.rendre(RELECTURE);
    const post = http.expectOne('/api/relectures/7');
    expect(post.request.headers.get('X-Etudiant-Id')).toBe('2');
    expect(post.request.body).toEqual({ note: 15, commentaire: 'Clair' });
    post.flush(null);
    http.expectOne(A_FAIRE).flush([]);
    http.expectOne(RENDUES).flush([{ ...RELECTURE, rendue: true, note: 15, commentaire: 'Clair' }, RENDUE]);
    expect(c.message()).toBe('Relecture envoyée.');
    fixture.destroy();
  });

  it("n'envoie rien si l'envoi définitif n'est pas confirmé (RG10)", async () => {
    const confirmation = spyOn(TestBed.inject(DialogueService), 'confirmer').and.resolveTo(false);
    const fixture = ouvrir();
    const c = fixture.componentInstance;
    c.notes[RELECTURE.id] = 14;
    c.commentaires[RELECTURE.id] = 'Clair';
    await c.rendre(RELECTURE);
    expect(confirmation).withContext('la confirmation est bien demandée').toHaveBeenCalledTimes(1);
    expect(http.match('/api/relectures/7')).withContext('refus : aucune requête').toHaveSize(0);
    fixture.destroy();
  });

  it('#101 — affiche les relectures déjà rendues, avec la note et le commentaire', () => {
    const fixture = ouvrir();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('Relectures rendues');
    expect(texte).toContain('TP Flyway');
    expect(texte).toContain('14 / 20');
    expect(texte).toContain('Bon découpage');
    fixture.destroy();
  });

  it('#101 — se rafraîchit seule : une relecture assignée après l’ouverture apparaît', fakeAsync(() => {
    const fixture = ouvrir([], []);
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Aucune relecture en attente.');
    tick(RAFRAICHISSEMENT_MS);
    http.expectOne(A_FAIRE).flush([RELECTURE]);
    http.expectOne(RENDUES).flush([]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('TP JPA');
    discardPeriodicTasks();
    fixture.destroy();
  }));

  it('#101 — le bouton n’est actif que pour une note entière de 0 à 20 et un commentaire', () => {
    const fixture = ouvrir();
    const c = fixture.componentInstance;
    c.commentaires[7] = 'ok';
    const cas: [number | null | undefined, boolean][] = [[undefined, false], [null, false], [21, false], [-1, false],
      [12.5, false], [0, true], [20, true]];
    for (const [note, attendu] of cas) {
      c.notes[7] = note;
      expect(c.peutEnvoyer(7)).withContext(`note ${note}`).toBe(attendu);
    }
    c.notes[7] = 15;
    c.commentaires[7] = '   ';
    expect(c.peutEnvoyer(7)).withContext('commentaire vide').toBeFalse();
    fixture.destroy();
  });

  it('#106 — un rafraîchissement réussi efface l’erreur précédente', fakeAsync(() => {
    const fixture = ouvrir();
    const alertes = () => (fixture.nativeElement as HTMLElement).querySelectorAll('.alerte');
    tick(RAFRAICHISSEMENT_MS);
    http.expectOne(A_FAIRE).flush({ code: 'ERREUR_INTERNE', message: 'Coupure passagère.' },
      { status: 500, statusText: 'Server Error' });
    http.expectOne(RENDUES).flush({ code: 'ERREUR_INTERNE', message: 'Coupure passagère.' },
      { status: 500, statusText: 'Server Error' });
    fixture.detectChanges();
    // #137 : chaque liste affiche sa propre erreur
    expect(alertes()).toHaveSize(2);
    tick(RAFRAICHISSEMENT_MS);
    http.expectOne(A_FAIRE).flush([]);
    http.expectOne(RENDUES).flush([]);
    fixture.detectChanges();
    expect(alertes()).withContext('les listes sont à jour, les erreurs doivent disparaître').toHaveSize(0);
    discardPeriodicTasks();
    fixture.destroy();
  }));

  it('#106 — le message « Relecture envoyée » disparaît au bout de quelques secondes', fakeAsync(() => {
    spyOn(TestBed.inject(DialogueService), 'confirmer').and.resolveTo(true);
    const fixture = ouvrir();
    const c = fixture.componentInstance;
    c.notes[7] = 15;
    c.commentaires[7] = 'ok';
    void c.rendre(RELECTURE);
    flushMicrotasks();
    http.expectOne('/api/relectures/7').flush(null);
    http.expectOne(A_FAIRE).flush([]);
    http.expectOne(RENDUES).flush([]);
    expect(c.message()).toBe('Relecture envoyée.');
    tick(5001);
    expect(c.message()).withContext('le message ne doit pas rester indéfiniment').toBe('');
    discardPeriodicTasks();
    fixture.destroy();
  }));

  it('#106 — le bouton Envoyer est désactivé pendant la requête (pas de double envoi)', async () => {
    spyOn(TestBed.inject(DialogueService), 'confirmer').and.resolveTo(true);
    const fixture = ouvrir();
    const c = fixture.componentInstance;
    c.notes[7] = 15;
    c.commentaires[7] = 'ok';
    await c.rendre(RELECTURE);
    fixture.detectChanges();
    const bouton = (fixture.nativeElement as HTMLElement).querySelector('button[type="submit"]') as HTMLButtonElement;
    expect(bouton.disabled).withContext('pendant la requête').toBeTrue();
    http.expectOne('/api/relectures/7').flush(null);
    http.expectOne(A_FAIRE).flush([]);
    http.expectOne(RENDUES).flush([]);
    fixture.detectChanges();
    expect(c.envoi()).withContext('la requête est terminée').toBeFalse();
    fixture.destroy();
  });
});
