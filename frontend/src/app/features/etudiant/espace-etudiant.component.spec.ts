import { TestBed } from '@angular/core/testing';
import { AuthService } from '../../core/auth/auth.service';
import { FOURNISSEURS_TEST, PROFILS } from '../../testing';
import { EspaceEtudiantComponent } from './espace-etudiant.component';

describe('EspaceEtudiantComponent — trois écrans (F2, #104)', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [EspaceEtudiantComponent], providers: FOURNISSEURS_TEST }));

  function page(): HTMLElement {
    const fixture = TestBed.createComponent(EspaceEtudiantComponent);
    fixture.detectChanges();
    return fixture.nativeElement as HTMLElement;
  }

  it('propose trois onglets en boutons, dont l’écran relecteur', () => {
    TestBed.inject(AuthService).profil.set(PROFILS.awa);
    const p = page();
    expect(Array.from(p.querySelectorAll('nav button')).map(b => b.textContent?.trim()))
      .toEqual(['Présence et dépôt', 'Mes notes', 'Mes relectures']);
    expect(p.querySelector('a')).toBeNull();
  });

  it('signale un compte sans fiche étudiant au lieu d’afficher les écrans', () => {
    TestBed.inject(AuthService).profil.set({ ...PROFILS.awa, etudiantId: null });
    const p = page();
    expect(p.querySelector('.alerte')?.textContent).toContain("n'est lié à aucune fiche étudiant");
    expect(p.querySelector('nav')).toBeNull();
  });
});
