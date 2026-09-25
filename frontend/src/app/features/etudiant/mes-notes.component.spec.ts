import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { FOURNISSEURS_TEST, PROFILS } from '../../testing';
import { RAFRAICHISSEMENT_MS } from './relectures.component';
import { MesNotesComponent } from './mes-notes.component';

const URL = '/api/etudiants/1/exercices';
const PROVISOIRE = { id: 1, sessionId: 1, sessionTitre: 'TP JPA', lien: 'https://x.cm', statut: 'EN_ATTENTE_RELECTURE',
  noteRetenue: 12, provisoire: true, commentaires: ['Clair'] };

describe('MesNotesComponent (#88, RG31, #101)', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [MesNotesComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).profil.set(PROFILS.awa);
  });

  it('affiche la note retenue du serveur et la marque provisoire si un seul pair a rendu', () => {
    const fixture = TestBed.createComponent(MesNotesComponent);
    fixture.detectChanges();
    http.expectOne(URL).flush([PROVISOIRE,
      { id: 2, sessionId: 2, sessionTitre: 'TP Flyway', lien: 'https://y.cm', statut: 'RELU',
        noteRetenue: 14.5, provisoire: false, commentaires: ['Bien', 'Tests à ajouter'] }]);
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('12 / 20');
    expect(texte).toContain('provisoire');
    expect(texte).toContain('14.5 / 20');
    expect(texte).toContain('définitive');
    fixture.destroy();
  });

  it('#101 — se rafraîchit seule : la note arrivée après l’ouverture apparaît', fakeAsync(() => {
    const fixture = TestBed.createComponent(MesNotesComponent);
    fixture.detectChanges();
    http.expectOne(URL).flush([{ ...PROVISOIRE, noteRetenue: null, provisoire: false, commentaires: [] }]);
    tick(RAFRAICHISSEMENT_MS);
    http.expectOne(URL).flush([PROVISOIRE]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('12 / 20');
    discardPeriodicTasks();
    fixture.destroy();
  }));
});
