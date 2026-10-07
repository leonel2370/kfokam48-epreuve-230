import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FOURNISSEURS_TEST } from '../../../testing';
import { GestionPromotionComponent } from './gestion-promotion.component';

const P1 = { id: 1, nom: 'P1-2026' };
const P2 = { id: 2, nom: 'P2-2026' };
const AWA = { id: 10, nom: 'Awa Ndiaye', promotionId: 1, actif: true, compteLogin: 'awa' };
const MARC = { id: 11, nom: 'Marc Tchoua', promotionId: 1, actif: false, compteLogin: null };
const URL_FORMATEURS = '/api/utilisateurs?page=0&size=100&role=FORMATEUR';
const AUCUN_FORMATEUR = { contenu: [], page: 0, taille: 100, total: 0 };

describe('GestionPromotionComponent — fiches d’une promotion (SF-22, #135)', () => {
  let http: HttpTestingController;

  function ouvrir() {
    TestBed.configureTestingModule({ imports: [GestionPromotionComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(GestionPromotionComponent);
    fixture.componentRef.setInput('promotion', P1);
    fixture.detectChanges();
    http.expectOne('/api/promotions/1/fiches').flush([AWA, MARC]);
    http.expectOne('/api/promotions/1/formateurs').flush([]);
    http.expectOne(URL_FORMATEURS).flush(AUCUN_FORMATEUR);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    const ligne = (texte: string) => Array.from(page.querySelectorAll('.ligne'))
      .find(l => l.textContent?.includes(texte)) as HTMLElement;
    const bouton = (texte: string) => Array.from(page.querySelectorAll('button'))
      .find(b => b.textContent?.trim() === texte) as HTMLButtonElement;
    return { fixture, page, ligne, bouton, c: fixture.componentInstance };
  }

  it('montre le compte lié à une fiche et ne propose pas de retirer une fiche déjà désactivée', () => {
    const { ligne, fixture } = ouvrir();
    expect(ligne('Awa Ndiaye').textContent).toContain('compte : awa');
    expect(ligne('Awa Ndiaye').textContent).toContain('Supprimer / désactiver');
    expect(ligne('Marc Tchoua').textContent).not.toContain('Supprimer / désactiver');
    fixture.destroy();
  });

  it('crée une fiche puis relit la liste sur le serveur', () => {
    const { page, bouton, c, fixture } = ouvrir();
    c.nomFiche = ' Clara Ndongo ';
    fixture.detectChanges();
    bouton('Créer la fiche').click();
    const creation = http.expectOne('/api/etudiants');
    expect(creation.request.body).toEqual({ nom: 'Clara Ndongo', promotionId: 1 });
    fixture.detectChanges();
    expect(bouton('Créer la fiche').disabled).withContext('pas de double envoi pendant la requête').toBeTrue();
    creation.flush({ id: 12, nom: 'Clara Ndongo', promotionId: 1 });
    http.expectOne('/api/promotions/1/fiches').flush([AWA,
      { id: 12, nom: 'Clara Ndongo', promotionId: 1, actif: true, compteLogin: null }, MARC]);
    fixture.detectChanges();
    expect(page.textContent).toContain('Clara Ndongo');
    expect(page.querySelector('output')?.textContent).toContain('créée dans « P1-2026 »');
    fixture.destroy();
  });

  it('retire une fiche après confirmation et affiche l’état rendu par le serveur', () => {
    const { ligne, fixture } = ouvrir();
    spyOn(window, 'confirm').and.returnValue(true);
    (ligne('Awa Ndiaye').querySelectorAll('button')[1] as HTMLButtonElement).click();
    const retrait = http.expectOne('/api/etudiants/10');
    expect(retrait.request.method).toBe('DELETE');
    retrait.flush(null);
    http.expectOne('/api/promotions/1/fiches').flush([{ ...AWA, actif: false }, MARC]);
    fixture.detectChanges();
    expect(ligne('Awa Ndiaye').textContent).toContain('(désactivée)');
    fixture.destroy();
  });

  it('renomme une fiche sans la changer de promotion', () => {
    const { ligne, fixture } = ouvrir();
    spyOn(window, 'prompt').and.returnValue('Awa Ndiaye-Meli');
    (ligne('Awa Ndiaye').querySelectorAll('button')[0] as HTMLButtonElement).click();
    const modification = http.expectOne('/api/etudiants/10');
    expect(modification.request.body).toEqual({ nom: 'Awa Ndiaye-Meli', promotionId: 1 });
    modification.flush({ id: 10, nom: 'Awa Ndiaye-Meli', promotionId: 1 });
    http.expectOne('/api/promotions/1/fiches').flush([{ ...AWA, nom: 'Awa Ndiaye-Meli' }, MARC]);
    fixture.detectChanges();
    expect(ligne('Awa Ndiaye-Meli')).toBeDefined();
    fixture.destroy();
  });

  it('affiche l’erreur du serveur dans la zone des fiches et réactive les boutons', () => {
    const { page, bouton, c, fixture } = ouvrir();
    c.nomFiche = 'Double';
    fixture.detectChanges();
    bouton('Créer la fiche').click();
    http.expectOne('/api/etudiants').flush({ code: 'ACCES_REFUSE', message: 'Vous n’avez pas les droits.' },
      { status: 403, statusText: 'Forbidden' });
    fixture.detectChanges();
    const alertes = Array.from(page.querySelectorAll('.alerte'));
    expect(alertes.length).withContext('une seule alerte, celle de la zone concernée').toBe(1);
    expect(alertes[0].textContent).toContain('Vous n’avez pas les droits.');
    expect(bouton('Créer la fiche').disabled).toBeFalse();
    fixture.destroy();
  });

  it('ignore une réponse arrivée après un changement de promotion', () => {
    const { page, fixture } = ouvrir();
    fixture.componentRef.setInput('promotion', P2);
    fixture.detectChanges();
    const fichesP2 = http.expectOne('/api/promotions/2/fiches');
    http.expectOne('/api/promotions/2/formateurs').flush([]);
    http.expectOne(URL_FORMATEURS).flush(AUCUN_FORMATEUR);
    fixture.componentRef.setInput('promotion', P1);
    fixture.detectChanges();
    http.expectOne('/api/promotions/1/fiches').flush([AWA]);
    http.expectOne('/api/promotions/1/formateurs').flush([]);
    http.expectOne(URL_FORMATEURS).flush(AUCUN_FORMATEUR);
    fichesP2.flush([{ id: 20, nom: 'Nora Bella', promotionId: 2, actif: true, compteLogin: null }]);
    fixture.detectChanges();
    expect(page.textContent).toContain('Awa Ndiaye');
    expect(page.textContent).not.toContain('Nora Bella');
    fixture.destroy();
  });
});
