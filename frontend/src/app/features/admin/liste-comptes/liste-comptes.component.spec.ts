import { HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { DialogueService } from '../../../core/dialogue/dialogue.service';
import { FOURNISSEURS_TEST } from '../../../testing';
import { ListeComptesComponent } from './liste-comptes.component';

const compte = (id: number, login: string, role: string, actif = true, etudiantId: number | null = null) =>
  ({ id, login, nomAffiche: login, role, etudiantId, actif, doitChangerMotDePasse: false });
const BRUNO = compte(7, 'bruno', 'FORMATEUR');
const ZOE = compte(8, 'zoe', 'FORMATEUR', false);
const AWA = compte(3, 'awa', 'ETUDIANT', true, 1);
const PAGE_0 = { contenu: [AWA, BRUNO, ZOE], page: 0, taille: 3, total: 5 };
const URL_PAGE_0 = '/api/utilisateurs?page=0&size=20';

describe('ListeComptesComponent (SF-20, #138)', () => {
  let http: HttpTestingController;
  let fixture: ComponentFixture<ListeComptesComponent>;
  let page: HTMLElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [ListeComptesComponent], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    fixture = TestBed.createComponent(ListeComptesComponent);
    fixture.detectChanges();
    http.expectOne(URL_PAGE_0).flush(PAGE_0);
    fixture.detectChanges();
    page = fixture.nativeElement as HTMLElement;
  });

  const ligne = (login: string) => Array.from(page.querySelectorAll('.ligne'))
    .find(l => l.querySelector('strong')?.textContent === login) as HTMLElement;
  const bouton = (login: string, texte: string) => Array.from(ligne(login).querySelectorAll('button'))
    .find(b => b.textContent?.trim() === texte) as HTMLButtonElement;

  it('montre le rôle en clair et l’état de chaque compte', () => {
    expect(ligne('awa').textContent).toContain('Étudiant');
    expect(ligne('zoe').textContent).toContain('Désactivé');
    expect(page.textContent).not.toContain('ETUDIANT');
  });

  it('propose de réactiver un compte désactivé et envoie le compte entier, actif', () => {
    expect(bouton('zoe', 'Désactiver')).toBeUndefined();
    bouton('zoe', 'Réactiver').click();
    const envoi = http.expectOne('/api/utilisateurs/8');
    expect(envoi.request.method).toBe('PUT');
    expect(envoi.request.body).toEqual({ nomAffiche: 'zoe', role: 'FORMATEUR', actif: true, etudiantId: null });
    envoi.flush({ ...ZOE, actif: true });
    http.expectOne(URL_PAGE_0).flush({ ...PAGE_0, contenu: [AWA, BRUNO, { ...ZOE, actif: true }] });
    fixture.detectChanges();
    expect(page.querySelector('output')?.textContent).toContain('réactivé');
    expect(bouton('zoe', 'Désactiver')).toBeDefined();
  });

  it('change le rôle d’un compte sans fiche, mais pas celui d’un compte étudiant', () => {
    expect(ligne('awa').querySelector('select')).withContext('un étudiant garde sa fiche et son rôle').toBeNull();
    fixture.componentInstance.changerRole(BRUNO as never, 'ADMIN');
    const envoi = http.expectOne('/api/utilisateurs/7');
    expect(envoi.request.body).toEqual({ nomAffiche: 'bruno', role: 'ADMIN', actif: true, etudiantId: null });
    envoi.flush({ ...BRUNO, role: 'ADMIN' });
    http.expectOne(URL_PAGE_0).flush(PAGE_0);
    fixture.detectChanges();
    expect(page.querySelector('output')?.textContent).toContain('« bruno » est maintenant Administrateur.');
  });

  it('affiche le refus du serveur quand on retire le dernier administrateur', () => {
    fixture.componentInstance.changerRole(BRUNO as never, 'ADMIN');
    http.expectOne('/api/utilisateurs/7').flush(
      { code: 'SUPPRESSION_IMPOSSIBLE',
        message: 'Le dernier administrateur actif ne peut être ni désactivé ni changé de rôle.' },
      { status: 409, statusText: 'Conflict' });
    fixture.detectChanges();
    expect(page.querySelector('.alerte')?.textContent).toContain('dernier administrateur actif');
  });

  it('désactive après confirmation à l’écran', async () => {
    const confirmer = spyOn(TestBed.inject(DialogueService), 'confirmer').and.resolveTo(true);
    bouton('bruno', 'Désactiver').click();
    await fixture.whenStable();
    expect(confirmer.calls.mostRecent().args[0].danger).toBeTrue();
    const envoi = http.expectOne('/api/utilisateurs/7');
    expect(envoi.request.method).toBe('DELETE');
    envoi.flush(null);
    http.expectOne(URL_PAGE_0).flush(PAGE_0);
  });

  it('n’envoie rien si la désactivation est annulée', async () => {
    spyOn(TestBed.inject(DialogueService), 'confirmer').and.resolveTo(false);
    bouton('bruno', 'Désactiver').click();
    await fixture.whenStable();
    expect(http.match('/api/utilisateurs/7')).toHaveSize(0);
  });

  it('demande le mot de passe provisoire dans un champ masqué', async () => {
    const saisir = spyOn(TestBed.inject(DialogueService), 'saisir').and.resolveTo('Provisoire48');
    bouton('bruno', 'Réinitialiser le mot de passe').click();
    await fixture.whenStable();
    expect(saisir.calls.mostRecent().args[0].champ.type).toBe('password');
    const envoi = http.expectOne('/api/utilisateurs/7/reinitialiser-mot-de-passe');
    expect(envoi.request.body).toEqual({ motDePasseInitial: 'Provisoire48' });
    envoi.flush(null);
    http.expectOne(URL_PAGE_0).flush(PAGE_0);
  });

  it('passe à la page suivante puis affiche la page rendue par le serveur', () => {
    const suivante = Array.from(page.querySelectorAll('button'))
      .find(b => b.textContent?.trim() === 'Page suivante') as HTMLButtonElement;
    suivante.click();
    http.expectOne('/api/utilisateurs?page=1&size=20').flush(
      { contenu: [compte(9, 'yann', 'ETUDIANT', true, 6)], page: 1, taille: 3, total: 5 });
    fixture.detectChanges();
    expect(page.textContent).toContain('5 comptes · page 2');
    expect(page.textContent).toContain('yann');
    expect(page.textContent).not.toContain('Page suivante');
  });
});
