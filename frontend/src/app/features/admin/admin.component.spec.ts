import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { FOURNISSEURS_TEST } from '../../testing';
import { AdminComponent } from './admin.component';

const PAGE = {
  contenu: [
    { id: 1, login: 'admin', nomAffiche: 'Administrateur', role: 'ADMIN', etudiantId: null, actif: true,
      doitChangerMotDePasse: false },
    { id: 2, login: 'awa', nomAffiche: 'Awa Ndiaye', role: 'ETUDIANT', etudiantId: 1, actif: true,
      doitChangerMotDePasse: false },
    { id: 3, login: 'zozo', nomAffiche: 'Zozo Parte', role: 'FORMATEUR', etudiantId: null, actif: false,
      doitChangerMotDePasse: true },
  ],
  page: 0,
  taille: 20,
  total: 3,
};

describe('AdminComponent — promotions et boutons de navigation (#104)', () => {
  function ouvrir() {
    TestBed.configureTestingModule({ imports: [AdminComponent], providers: FOURNISSEURS_TEST });
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(AdminComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/promotions').flush([{ id: 1, nom: 'P1-2026' }, { id: 2, nom: 'P2-2026' }]);
    http.expectOne('/api/utilisateurs?page=0&size=20').flush(PAGE);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    const bouton = (texte: string, rang: number) => Array.from(page.querySelectorAll('button'))
      .filter(b => b.textContent?.trim() === texte)[rang];
    return { page, navigate, bouton, http, fixture };
  }

  it('présente chaque promotion avec des boutons, sans aucun lien', () => {
    const { page, fixture } = ouvrir();
    expect(page.querySelectorAll('button').length).toBeGreaterThanOrEqual(4);
    expect(page.querySelector('a')).toBeNull();
    fixture.destroy();
  });

  it('« Tableau » ouvre le tableau de la promotion cliquée', () => {
    const { navigate, bouton, fixture } = ouvrir();
    bouton('Tableau', 1).click();
    expect(navigate).toHaveBeenCalledWith(['/formateur/tableau/2'], { queryParams: undefined });
    fixture.destroy();
  });

  it('« Sessions » ouvre les sessions de la promotion cliquée, pas de la première', () => {
    const { navigate, bouton, fixture } = ouvrir();
    bouton('Sessions', 1).click();
    expect(navigate).toHaveBeenCalledWith(['/formateur'], { queryParams: { promotionId: 2 } });
    fixture.destroy();
  });
});

describe('AdminComponent — gestion des comptes (#60)', () => {
  function ouvrir() {
    TestBed.configureTestingModule({ imports: [AdminComponent], providers: FOURNISSEURS_TEST });
    const fixture = TestBed.createComponent(AdminComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/promotions').flush([]);
    http.expectOne('/api/utilisateurs?page=0&size=20').flush(PAGE);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    return { page, http, fixture };
  }

  it('liste les comptes avec rôle et état, sans jamais afficher de mot de passe', () => {
    const { page, fixture } = ouvrir();
    const texte = page.textContent ?? '';
    expect(texte).toContain('admin');
    expect(texte).toContain('awa');
    expect(texte).toContain('désactivé');
    expect(texte).not.toContain('motDePasse');
    expect(texte.toLowerCase()).not.toContain('password');
    fixture.destroy();
  });

  it('crée un compte étudiant et annonce le changement de mot de passe obligatoire (RG23)', () => {
    const { page, http, fixture } = ouvrir();
    const c = fixture.componentInstance;
    c.login = 'clara';
    c.nomAffiche = 'Clara Ndongo';
    c.motDePasseInitial = 'MotDePasse9';
    c.etudiantIdCreation = 5;
    const boutonCreer = Array.from(page.querySelectorAll('button'))
      .find(b => b.textContent?.includes('Créer le compte'))!;
    boutonCreer.click();
    const creation = http.expectOne('/api/utilisateurs');
    expect(creation.request.method).toBe('POST');
    expect(creation.request.body).toEqual({ login: 'clara', nomAffiche: 'Clara Ndongo', role: 'ETUDIANT',
      motDePasseInitial: 'MotDePasse9', etudiantId: 5 });
    creation.flush({ id: 9, login: 'clara', nomAffiche: 'Clara Ndongo', role: 'ETUDIANT', etudiantId: 5,
      actif: true, doitChangerMotDePasse: true });
    // la liste est rechargée après la création
    http.expectOne('/api/utilisateurs?page=0&size=20').flush(PAGE);
    fixture.detectChanges();
    expect(page.textContent).toContain('il devra changer son mot de passe');
    fixture.destroy();
  });

  it('affiche l’erreur du serveur quand le login est déjà pris (RG27)', () => {
    const { page, http, fixture } = ouvrir();
    const c = fixture.componentInstance;
    c.login = 'awa';
    c.nomAffiche = 'Double';
    c.motDePasseInitial = 'MotDePasse9';
    c.etudiantIdCreation = 5;
    Array.from(page.querySelectorAll('button')).find(b => b.textContent?.includes('Créer le compte'))!.click();
    http.expectOne('/api/utilisateurs').flush(
      { code: 'LOGIN_DEJA_UTILISE', message: 'Cet identifiant est déjà pris.' },
      { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(page.textContent).toContain('Cet identifiant est déjà pris.');
    fixture.destroy();
  });
});
