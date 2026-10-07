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
});

describe('AdminComponent — promotions (#61)', () => {
  function ouvrir() {
    TestBed.configureTestingModule({ imports: [AdminComponent], providers: FOURNISSEURS_TEST });
    const fixture = TestBed.createComponent(AdminComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/promotions').flush([{ id: 1, nom: 'P1-2026' }, { id: 2, nom: 'P2-2026' }]);
    http.expectOne('/api/utilisateurs?page=0&size=20').flush(PAGE);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    const bouton = (texte: string, rang = 0) => Array.from(page.querySelectorAll('button'))
      .filter(b => b.textContent?.trim() === texte)[rang];
    return { page, http, fixture, bouton, c: fixture.componentInstance };
  }

  it('crée une promotion et l’ajoute à la liste', () => {
    const { page, http, fixture, c, bouton } = ouvrir();
    c.nomPromotionCreation = 'P3-2026';
    bouton('Créer la promotion').click();
    const creation = http.expectOne('/api/promotions');
    expect(creation.request.method).toBe('POST');
    expect(creation.request.body).toEqual({ nom: 'P3-2026' });
    creation.flush({ id: 5, nom: 'P3-2026' });
    fixture.detectChanges();
    expect(page.textContent).toContain('P3-2026');
    expect(page.textContent).toContain('créée');
    fixture.destroy();
  });

  it('affiche l’erreur quand le nom de promotion est déjà pris (409 CONFLIT)', () => {
    const { page, http, fixture, c, bouton } = ouvrir();
    c.nomPromotionCreation = 'P1-2026';
    bouton('Créer la promotion').click();
    http.expectOne('/api/promotions').flush(
      { code: 'CONFLIT', message: 'Ce nom de promotion est déjà pris.' },
      { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(page.textContent).toContain('Ce nom de promotion est déjà pris.');
    fixture.destroy();
  });
});
