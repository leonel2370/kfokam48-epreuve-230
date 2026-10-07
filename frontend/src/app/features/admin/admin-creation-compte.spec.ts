import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FOURNISSEURS_TEST } from '../../testing';
import { AdminComponent } from './admin.component';

const PAGE = { contenu: [], page: 0, taille: 20, total: 0 };

describe('Administration — créer un compte étudiant sans identifiant technique (#136)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<AdminComponent>;
  let page: HTMLElement;

  async function ouvrir() {
    TestBed.configureTestingModule({ imports: [AdminComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(AdminComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([{ id: 1, nom: 'P1-2026' }, { id: 2, nom: 'P2-2026' }]);
    http.expectOne('/api/utilisateurs?page=0&size=20').flush(PAGE);
    fixture.detectChanges();
    await fixture.whenStable();
    page = fixture.nativeElement as HTMLElement;
  }

  async function saisir(selecteur: string, valeur: string) {
    const champ = page.querySelector(selecteur) as HTMLInputElement;
    champ.value = valeur;
    champ.dispatchEvent(new Event('input'));
    fixture.detectChanges();
    await fixture.whenStable();
  }

  const boutonCreer = () => Array.from(page.querySelectorAll('button'))
    .find(b => b.textContent?.includes('Créer le compte')) as HTMLButtonElement;

  it('ne demande aucun numéro de fiche : la fiche se choisit dans une liste, après la promotion', async () => {
    await ouvrir();
    expect(page.querySelector('input[name="etudiantId"]'))
      .withContext('plus de saisie du numéro interne de la fiche').toBeNull();
    expect(page.querySelector('select[name="promotionDeLaFiche"]')).not.toBeNull();
    expect(page.querySelector('select[name="fiche"]')).not.toBeNull();
    fixture.destroy();
  });

  it('n’envoie rien tant que la fiche de l’étudiant n’est pas choisie', async () => {
    await ouvrir();
    await saisir('input[name="login"]', 'clara');
    await saisir('input[name="nomAffiche"]', 'Clara Ndongo');
    await saisir('input[name="motDePasseInitial"]', 'Provisoire48');
    const bouton = boutonCreer();
    bouton.click();
    fixture.detectChanges();
    http.expectNone('/api/utilisateurs');
    expect(bouton.disabled).withContext('le bouton reste inactif tant que le formulaire est incomplet').toBeTrue();
    fixture.destroy();
  });
});
