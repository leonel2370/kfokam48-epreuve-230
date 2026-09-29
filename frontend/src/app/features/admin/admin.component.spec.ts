import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { FOURNISSEURS_TEST } from '../../testing';
import { AdminComponent } from './admin.component';

const FORMATEURS = [
  { id: 2, login: 'formateur', nomAffiche: 'Formateur P1', role: 'FORMATEUR', etudiantId: null, actif: true,
    doitChangerMotDePasse: false },
];

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

describe('AdminComponent — gestion du référentiel (#61)', () => {
  const PAGE_AVEC_FORMATEUR = { ...PAGE, contenu: [...PAGE.contenu, ...FORMATEURS] };

  function ouvrir() {
    TestBed.configureTestingModule({ imports: [AdminComponent], providers: FOURNISSEURS_TEST });
    const fixture = TestBed.createComponent(AdminComponent);
    fixture.detectChanges();
    const http = TestBed.inject(HttpTestingController);
    http.expectOne('/api/promotions').flush([{ id: 1, nom: 'P1-2026' }, { id: 2, nom: 'P2-2026' }]);
    http.expectOne('/api/utilisateurs?page=0&size=20').flush(PAGE_AVEC_FORMATEUR);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    const bouton = (texte: string, rang = 0) => Array.from(page.querySelectorAll('button'))
      .filter(b => b.textContent?.trim() === texte)[rang];
    return { page, http, fixture, bouton, c: fixture.componentInstance };
  }

  function gerer(http: HttpTestingController, fixture: import('@angular/core/testing').ComponentFixture<AdminComponent>) {
    fixture.componentInstance.gerer({ id: 1, nom: 'P1-2026' });
    const fiches = http.expectOne('/api/promotions/1/etudiants?admin=true');
    expect(fiches.request.method).toBe('GET');
    fiches.flush([
      { id: 10, nom: 'Awa Ndiaye', promotionId: 1, actif: true },
      { id: 11, nom: 'Boris Nkoulou', promotionId: 1, actif: false },
    ]);
    fixture.detectChanges();
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

  it('rattache un formateur et le montre comme rattaché (RG26)', () => {
    const { page, http, fixture, bouton } = ouvrir();
    gerer(http, fixture);
    bouton('Rattacher').click();
    const requete = http.expectOne('/api/promotions/1/formateurs');
    expect(requete.request.method).toBe('PUT');
    expect(requete.request.body).toEqual({ utilisateurIds: [2] });
    requete.flush(null);
    fixture.detectChanges();
    expect(page.textContent).toContain('Détacher');
    expect(page.textContent).toContain('enregistrés');
    fixture.destroy();
  });

  it('crée une fiche étudiant dans la promotion gérée (SF-22)', () => {
    const { page, http, fixture, c, bouton } = ouvrir();
    gerer(http, fixture);
    c.nomFicheCreation = 'Clara Ndongo';
    bouton('Créer la fiche').click();
    const creation = http.expectOne('/api/etudiants');
    expect(creation.request.method).toBe('POST');
    expect(creation.request.body).toEqual({ nom: 'Clara Ndongo', promotionId: 1 });
    creation.flush({ id: 12, nom: 'Clara Ndongo', promotionId: 1 });
    fixture.detectChanges();
    expect(page.textContent).toContain('Clara Ndongo');
    expect(page.textContent).toContain('créée dans');
    fixture.destroy();
  });

  it('affiche les fiches désactivées et le volet RG28', () => {
    const { page, http, fixture } = ouvrir();
    gerer(http, fixture);
    expect(page.textContent).toContain('(désactivé)');
    expect(page.textContent).toContain('RG28');
    fixture.destroy();
  });

  it('propage le renommage d’une fiche au serveur', () => {
    const { page, http, fixture } = ouvrir();
    gerer(http, fixture);
    const c = fixture.componentInstance;
    spyOn(window, 'prompt').and.returnValue('Awa Ndiaye-Meli');
    c.renommerFiche({ id: 10, nom: 'Awa Ndiaye', promotionId: 1, actif: true });
    const modification = http.expectOne('/api/etudiants/10');
    expect(modification.request.method).toBe('PUT');
    expect(modification.request.body).toEqual({ nom: 'Awa Ndiaye-Meli', promotionId: 1 });
    modification.flush({ id: 10, nom: 'Awa Ndiaye-Meli', promotionId: 1 });
    fixture.detectChanges();
    expect(page.textContent).toContain('Awa Ndiaye-Meli');
    fixture.destroy();
  });
});
