import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FOURNISSEURS_TEST } from '../../testing';
import { AdminComponent } from './admin.component';

const compte = (id: number, login: string, role: string, actif = true) =>
  ({ id, login, nomAffiche: login, role, etudiantId: null, actif, doitChangerMotDePasse: false });

const ANNE = compte(2, 'anne', 'FORMATEUR');
const BRUNO = compte(7, 'bruno', 'FORMATEUR');

/** Première page de comptes : aucun formateur dessus, pour prouver que le volet ne s'en sert pas. */
const PAGE_SANS_FORMATEUR = { contenu: [compte(1, 'admin', 'ADMIN')], page: 0, taille: 20, total: 30 };

/** Réponse réelle de GET /api/promotions/{id}/fiches (contrat 2.8). */
const FICHES = [
  { id: 10, nom: 'Awa Ndiaye', promotionId: 1, actif: true, compteLogin: 'awa' },
  { id: 11, nom: 'Marc Tchoua', promotionId: 1, actif: false, compteLogin: null },
];

const URL_FICHES = '/api/promotions/1/fiches';
const URL_RATTACHES = '/api/promotions/1/formateurs';
const URL_FORMATEURS = '/api/utilisateurs?page=0&size=100&role=FORMATEUR';

describe('Administration — référentiel lu sur l’API réelle (#135)', () => {
  let http: HttpTestingController;

  function ouvrirLeVolet(rattaches: unknown[]) {
    TestBed.configureTestingModule({ imports: [AdminComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(AdminComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([{ id: 1, nom: 'P1-2026' }, { id: 2, nom: 'P2-2026' }]);
    http.expectOne('/api/utilisateurs?page=0&size=20').flush(PAGE_SANS_FORMATEUR);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    const boutons = (texte: string) => Array.from(page.querySelectorAll('button'))
      .filter(b => b.textContent?.trim() === texte);
    boutons('Gérer')[0].click();
    http.expectOne(URL_FICHES).flush(FICHES);
    http.expectOne(URL_RATTACHES).flush(rattaches);
    http.expectOne(URL_FORMATEURS).flush({ contenu: [ANNE, BRUNO], page: 0, taille: 100, total: 2 });
    fixture.detectChanges();
    const ligne = (texte: string) => Array.from(page.querySelectorAll('.ligne'))
      .find(l => l.textContent?.includes(texte)) as HTMLElement;
    return { fixture, page, boutons, ligne };
  }

  it('ne marque « désactivée » que la fiche qui l’est vraiment', () => {
    const { ligne, fixture } = ouvrirLeVolet([]);
    expect(ligne('Awa Ndiaye').textContent).not.toContain('désactivé');
    expect(ligne('Marc Tchoua').textContent).toContain('désactivé');
    fixture.destroy();
  });

  it('propose tous les formateurs actifs, pas seulement ceux de la page de comptes affichée', () => {
    const { ligne, fixture } = ouvrirLeVolet([ANNE]);
    expect(ligne('anne').textContent).toContain('Détacher');
    expect(ligne('bruno').textContent).toContain('Rattacher');
    fixture.destroy();
  });

  it('rattacher un second formateur conserve le premier', () => {
    const { ligne, fixture } = ouvrirLeVolet([ANNE]);
    ligne('bruno').querySelector('button')!.click();
    const envoi = http.expectOne(r => r.method === 'PUT' && r.url === URL_RATTACHES);
    expect(envoi.request.body).toEqual({ utilisateurIds: [2, 7] });
    envoi.flush(null);
    http.expectOne(r => r.method === 'GET' && r.url === URL_RATTACHES).flush([ANNE, BRUNO]);
    fixture.detectChanges();
    expect(ligne('anne').textContent).toContain('Détacher');
    expect(ligne('bruno').textContent).toContain('Détacher');
    fixture.destroy();
  });

  it('détacher le dernier formateur envoie une liste vide', () => {
    const { ligne, fixture } = ouvrirLeVolet([ANNE]);
    ligne('anne').querySelector('button')!.click();
    const envoi = http.expectOne(r => r.method === 'PUT' && r.url === URL_RATTACHES);
    expect(envoi.request.body).toEqual({ utilisateurIds: [] });
    envoi.flush(null);
    http.expectOne(r => r.method === 'GET' && r.url === URL_RATTACHES).flush([]);
    fixture.detectChanges();
    expect(ligne('anne').textContent).toContain('Rattacher');
    fixture.destroy();
  });

  it('ne garde pas les rattachements d’une promotion quand on en ouvre une autre', () => {
    const { boutons, ligne, fixture } = ouvrirLeVolet([ANNE]);
    boutons('Gérer')[1].click();
    http.expectOne('/api/promotions/2/fiches').flush([]);
    http.expectOne('/api/promotions/2/formateurs').flush([]);
    http.expectOne(URL_FORMATEURS).flush({ contenu: [ANNE, BRUNO], page: 0, taille: 100, total: 2 });
    fixture.detectChanges();
    expect(ligne('anne').textContent).toContain('Rattacher');
    fixture.destroy();
  });
});
