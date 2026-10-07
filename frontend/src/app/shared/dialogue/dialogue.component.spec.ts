import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DialogueService } from '../../core/dialogue/dialogue.service';
import { DialogueComponent } from './dialogue.component';

describe('DialogueComponent et DialogueService (#138)', () => {
  let fixture: ComponentFixture<DialogueComponent>;
  let service: DialogueService;
  let page: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [DialogueComponent] });
    fixture = TestBed.createComponent(DialogueComponent);
    service = TestBed.inject(DialogueService);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  const cadre = () => page.querySelector('dialog') as HTMLDialogElement;
  const bouton = (texte: string) => Array.from(page.querySelectorAll('button'))
    .find(b => b.textContent?.trim() === texte) as HTMLButtonElement;

  async function afficher() {
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
  }

  it('reste fermé tant que rien n’est demandé', () => {
    expect(cadre().open).toBeFalse();
  });

  it('ouvre un dialogue modal, confirme et se referme', async () => {
    const reponse = service.confirmer({ titre: 'Retirer la fiche ?', message: 'Elle sera désactivée.',
      confirmer: 'Désactiver', danger: true });
    await afficher();
    expect(cadre().open).toBeTrue();
    expect(page.textContent).toContain('Retirer la fiche ?');
    expect(bouton('Désactiver').classList).toContain('danger');
    bouton('Désactiver').click();
    expect(await reponse).toBeTrue();
    await afficher();
    expect(cadre().open).toBeFalse();
  });

  it('répond faux quand on annule', async () => {
    const reponse = service.confirmer({ titre: 'Supprimer ?', confirmer: 'Supprimer' });
    await afficher();
    bouton('Annuler').click();
    expect(await reponse).toBeFalse();
  });

  it('répond faux quand on ferme avec Échap', async () => {
    const reponse = service.confirmer({ titre: 'Supprimer ?', confirmer: 'Supprimer' });
    await afficher();
    cadre().dispatchEvent(new Event('cancel', { cancelable: true }));
    expect(await reponse).toBeFalse();
  });

  it('rend la valeur saisie, dans un champ masqué pour un mot de passe', async () => {
    const reponse = service.saisir({ titre: 'Mot de passe provisoire', confirmer: 'Réinitialiser',
      champ: { libelle: 'Mot de passe', type: 'password' } });
    await afficher();
    const champ = page.querySelector('input') as HTMLInputElement;
    expect(champ.type).toBe('password');
    expect(bouton('Réinitialiser').disabled).withContext('rien à envoyer tant que le champ est vide').toBeTrue();
    champ.value = 'Provisoire48';
    champ.dispatchEvent(new Event('input'));
    await afficher();
    bouton('Réinitialiser').click();
    expect(await reponse).toBe('Provisoire48');
  });

  it('préremplit le champ et rend null quand la saisie est annulée', async () => {
    const reponse = service.saisir({ titre: 'Renommer', confirmer: 'Enregistrer',
      champ: { libelle: 'Nouveau nom', type: 'text', valeur: 'P1-2026' } });
    await afficher();
    expect((page.querySelector('input') as HTMLInputElement).value).toBe('P1-2026');
    bouton('Annuler').click();
    expect(await reponse).toBeNull();
  });

  it('annule la demande précédente si une nouvelle arrive', async () => {
    const premiere = service.confirmer({ titre: 'Première', confirmer: 'Oui' });
    const seconde = service.confirmer({ titre: 'Seconde', confirmer: 'Oui' });
    expect(await premiere).toBeFalse();
    await afficher();
    expect(page.textContent).toContain('Seconde');
    bouton('Oui').click();
    expect(await seconde).toBeTrue();
  });
});
