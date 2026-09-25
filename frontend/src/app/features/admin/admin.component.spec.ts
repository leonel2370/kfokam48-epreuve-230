import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { FOURNISSEURS_TEST } from '../../testing';
import { AdminComponent } from './admin.component';

describe('AdminComponent — promotions et boutons de navigation (#104)', () => {
  function ouvrir() {
    TestBed.configureTestingModule({ imports: [AdminComponent], providers: FOURNISSEURS_TEST });
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(AdminComponent);
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne('/api/promotions')
      .flush([{ id: 1, nom: 'P1-2026' }, { id: 2, nom: 'P2-2026' }]);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    const bouton = (texte: string, rang: number) => Array.from(page.querySelectorAll('button'))
      .filter(b => b.textContent?.trim() === texte)[rang];
    return { page, navigate, bouton };
  }

  it('présente chaque promotion avec des boutons, sans aucun lien', () => {
    const { page } = ouvrir();
    expect(page.querySelectorAll('button').length).toBe(4);
    expect(page.querySelector('a')).toBeNull();
  });

  it('« Tableau » ouvre le tableau de la promotion cliquée', () => {
    const { navigate, bouton } = ouvrir();
    bouton('Tableau', 1).click();
    expect(navigate).toHaveBeenCalledWith(['/formateur/tableau/2'], { queryParams: undefined });
  });

  it('« Sessions » ouvre les sessions de la promotion cliquée, pas de la première', () => {
    const { navigate, bouton } = ouvrir();
    bouton('Sessions', 1).click();
    expect(navigate).toHaveBeenCalledWith(['/formateur'], { queryParams: { promotionId: 2 } });
  });
});
