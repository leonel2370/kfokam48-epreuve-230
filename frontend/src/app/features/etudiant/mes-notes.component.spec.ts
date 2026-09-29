import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed, discardPeriodicTasks, fakeAsync, tick } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { FOURNISSEURS_TEST, PROFILS } from '../../testing';
import { RAFRAICHISSEMENT_MS } from './relectures.component';
import { MesNotesComponent } from './mes-notes.component';

const URL = '/api/etudiants/1/exercices';
const RECAP = '/api/moi/recap';
const PROVISOIRE = { id: 1, sessionId: 1, sessionTitre: 'TP JPA', lien: 'https://x.cm', statut: 'EN_ATTENTE_RELECTURE',
  noteRetenue: 12, provisoire: true, commentaires: ['Clair'] };
const LIGNE_AWA = { etudiantId: 1, nom: 'Awa Ndiaye', presences: 2, exercicesDeposes: 1, moyenne: 13.5,
  relecturesEnAttente: 1 };

describe('MesNotesComponent (#88, RG31, #101, #112)', () => {
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
    http.expectOne(RECAP).flush(LIGNE_AWA);
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
    http.expectOne(RECAP).flush(LIGNE_AWA);
    tick(RAFRAICHISSEMENT_MS);
    http.expectOne(URL).flush([PROVISOIRE]);
    http.expectOne(RECAP).flush(LIGNE_AWA);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('12 / 20');
    discardPeriodicTasks();
    fixture.destroy();
  }));

  it('#106 — un rafraîchissement réussi efface l’erreur précédente', fakeAsync(() => {
    const fixture = TestBed.createComponent(MesNotesComponent);
    fixture.detectChanges();
    http.expectOne(URL).flush([PROVISOIRE]);
    http.expectOne(RECAP).flush(LIGNE_AWA);
    const c = fixture.componentInstance;
    tick(RAFRAICHISSEMENT_MS);
    http.expectOne(URL).flush({ code: 'ERREUR_INTERNE', message: 'Coupure passagère.' },
      { status: 500, statusText: 'Server Error' });
    http.expectOne(RECAP).flush(LIGNE_AWA);
    expect(c.erreur()).not.toBeNull();
    tick(RAFRAICHISSEMENT_MS);
    http.expectOne(URL).flush([PROVISOIRE]);
    http.expectOne(RECAP).flush(LIGNE_AWA);
    expect(c.erreur()).withContext('les données sont à jour, l’erreur doit disparaître').toBeNull();
    discardPeriodicTasks();
    fixture.destroy();
  }));

  it('#112 — affiche le récapitulatif du serveur, avec « — » sans note, et rien tant qu’il n’est pas arrivé', () => {
    const fixture = TestBed.createComponent(MesNotesComponent);
    fixture.detectChanges();
    http.expectOne(URL).flush([PROVISOIRE]);
    // le récapitulatif n'est pas encore arrivé : l'encart reste absent
    expect((fixture.nativeElement as HTMLElement).textContent).not.toContain('Récapitulatif');
    http.expectOne(RECAP).flush({ ...LIGNE_AWA, moyenne: null, relecturesEnAttente: 2 });
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('Récapitulatif');
    expect(texte).toContain('2 présences');
    expect(texte).toContain('moyenne —');
    expect(texte).toContain('2 relectures en attente');
    fixture.destroy();
  });
});
