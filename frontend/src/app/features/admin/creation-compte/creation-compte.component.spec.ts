import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Utilisateur } from '../../../core/api/api.models';
import { FOURNISSEURS_TEST } from '../../../testing';
import { CreationCompteComponent } from './creation-compte.component';

const PROMOTIONS = [{ id: 1, nom: 'P1-2026' }, { id: 2, nom: 'P2-2026' }];
const FICHES_P1 = [
  { id: 10, nom: 'Awa Ndiaye', promotionId: 1, actif: true, compteLogin: 'awa' },
  { id: 12, nom: 'Hugo Talla', promotionId: 1, actif: true, compteLogin: null },
  { id: 13, nom: 'Marc Tchoua', promotionId: 1, actif: false, compteLogin: null },
];
const HUGO_CREE = { id: 9, login: 'hugo', nomAffiche: 'Hugo Talla', role: 'ETUDIANT', etudiantId: 12, actif: true,
  doitChangerMotDePasse: true };

describe('CreationCompteComponent (SF-20, #136)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<CreationCompteComponent>;
  let page: HTMLElement;
  let crees: Utilisateur[];

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [CreationCompteComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(CreationCompteComponent);
    fixture.componentRef.setInput('promotions', PROMOTIONS);
    crees = [];
    fixture.componentInstance.cree.subscribe(u => crees.push(u));
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  const bouton = () => page.querySelector('button[type="submit"]') as HTMLButtonElement;

  function choisirHugo() {
    const c = fixture.componentInstance;
    c.choisirPromotion(1);
    http.expectOne('/api/promotions/1/fiches').flush(FICHES_P1);
    c.choisirFiche(12);
    fixture.detectChanges();
    return c;
  }

  it('ne propose que les fiches actives qui n’ont pas encore de compte', () => {
    const c = choisirHugo();
    expect(c.fiches().map(f => f.nom)).toEqual(['Hugo Talla']);
  });

  it('reprend le nom de la fiche choisie comme nom affiché', () => {
    expect(choisirHugo().nomAffiche).toBe('Hugo Talla');
  });

  it('crée le compte lié à la fiche choisie et prévient l’écran', () => {
    const c = choisirHugo();
    c.login = ' hugo ';
    c.motDePasseInitial = 'Provisoire48';
    fixture.detectChanges();
    expect(bouton().disabled).toBeFalse();
    bouton().click();
    const creation = http.expectOne('/api/utilisateurs');
    expect(creation.request.body).toEqual({ login: 'hugo', nomAffiche: 'Hugo Talla', role: 'ETUDIANT',
      motDePasseInitial: 'Provisoire48', etudiantId: 12 });
    fixture.detectChanges();
    expect(bouton().disabled).withContext('pas de double envoi pendant la requête').toBeTrue();
    creation.flush(HUGO_CREE);
    fixture.detectChanges();
    expect(crees).toEqual([HUGO_CREE as Utilisateur]);
    expect(page.querySelector('output')?.textContent).toContain('il devra changer son mot de passe');
    expect(c.login).withContext('formulaire vidé après la création').toBe('');
  });

  it('crée un compte formateur sans demander de fiche', () => {
    const c = fixture.componentInstance;
    c.role = 'FORMATEUR';
    c.login = 'bruno';
    c.nomAffiche = 'Bruno Essomba';
    c.motDePasseInitial = 'Provisoire48';
    fixture.detectChanges();
    expect(page.querySelector('select[name="fiche"]')).toBeNull();
    bouton().click();
    expect(http.expectOne('/api/utilisateurs').request.body.etudiantId).toBeNull();
  });

  it('affiche le message du serveur et garde la saisie quand la création est refusée', () => {
    const c = choisirHugo();
    c.login = 'awa';
    c.motDePasseInitial = 'court';
    fixture.detectChanges();
    bouton().click();
    http.expectOne('/api/utilisateurs').flush(
      { code: 'MOT_DE_PASSE_TROP_FAIBLE', message: 'Le mot de passe doit contenir au moins 8 caractères.' },
      { status: 400, statusText: 'Bad Request' });
    fixture.detectChanges();
    expect(page.querySelector('.alerte')?.textContent).toContain('au moins 8 caractères');
    expect(c.login).toBe('awa');
    expect(crees).toEqual([]);
    expect(bouton().disabled).toBeFalse();
  });

  it('oublie la fiche choisie quand on change de promotion', () => {
    const c = choisirHugo();
    c.choisirPromotion(2);
    http.expectOne('/api/promotions/2/fiches').flush([]);
    fixture.detectChanges();
    expect(c.ficheId).toBeNull();
    expect(page.textContent).toContain('Aucune fiche sans compte dans cette promotion');
  });
});
