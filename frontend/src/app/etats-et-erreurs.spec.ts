import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Type } from '@angular/core';
import { AuthService } from './core/auth/auth.service';
import { DialogueService } from './core/dialogue/dialogue.service';
import { AdminComponent } from './features/admin/admin.component';
import { MesNotesComponent } from './features/etudiant/mes-notes.component';
import { RelecturesComponent } from './features/etudiant/relectures.component';
import { FormateurComponent } from './features/formateur/formateur.component';
import { TableauComponent } from './features/formateur/tableau.component';
import { FOURNISSEURS_TEST, PROFILS } from './testing';

const PANNE = { code: 'ERREUR_INTERNE', message: 'Le service est momentanément indisponible.' };
const EN_PANNE = { status: 500, statusText: 'Server Error' };
const PAGE = {
  contenu: [{ id: 2, login: 'awa', nomAffiche: 'Awa Ndiaye', role: 'ETUDIANT', etudiantId: 1, actif: true,
    doitChangerMotDePasse: false }],
  page: 0, taille: 20, total: 1,
};

/** #137 : une erreur n'est effacée que par la réussite de la MÊME requête ; chaque écran a ses états. */
describe('États et erreurs des écrans (#137)', () => {
  let http: HttpTestingController;

  function creer<T>(composant: Type<T>, profil = PROFILS.awa) {
    TestBed.configureTestingModule({ imports: [composant], providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(AuthService).profil.set(profil);
    const fixture = TestBed.createComponent(composant);
    return { fixture, page: fixture.nativeElement as HTMLElement };
  }

  const alertes = (page: HTMLElement) => Array.from(page.querySelectorAll('.alerte')).map(a => a.textContent ?? '');

  it('administration : la réussite des comptes n’efface pas l’échec des promotions', () => {
    const { fixture, page } = creer(AdminComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush(PANNE, EN_PANNE);
    http.expectOne('/api/utilisateurs?page=0&size=20').flush(PAGE);
    fixture.detectChanges();
    expect(alertes(page).join(' ')).toContain('momentanément indisponible');
    expect(page.textContent).withContext('une panne n’est pas une liste vide').not.toContain('Aucune promotion.');
    fixture.destroy();
  });

  it('administration : pendant le chargement, ni « Aucune promotion » ni « Aucun compte »', () => {
    const { fixture, page } = creer(AdminComponent);
    fixture.detectChanges();
    expect(page.textContent).toContain('Chargement');
    expect(page.textContent).not.toContain('Aucune promotion.');
    expect(page.textContent).not.toContain('Aucun compte.');
    http.expectOne('/api/promotions').flush([]);
    http.expectOne('/api/utilisateurs?page=0&size=20').flush(PAGE);
    fixture.destroy();
  });

  it('administration : « Désactiver » ne peut pas partir deux fois', async () => {
    const { fixture, page } = creer(AdminComponent);
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([]);
    http.expectOne('/api/utilisateurs?page=0&size=20').flush(PAGE);
    fixture.detectChanges();
    spyOn(TestBed.inject(DialogueService), 'confirmer').and.resolveTo(true);
    const bouton = () => Array.from(page.querySelectorAll('button'))
      .find(b => b.textContent?.trim() === 'Désactiver') as HTMLButtonElement;
    bouton().click();
    await fixture.whenStable();
    fixture.detectChanges();
    expect(bouton().disabled).toBeTrue();
    bouton().click();
    await fixture.whenStable();
    expect(http.match('/api/utilisateurs/2').length).withContext('une seule requête de désactivation').toBe(1);
    fixture.destroy();
  });

  it('mes relectures : la réussite des relectures rendues n’efface pas l’échec des relectures à faire', () => {
    const { fixture, page } = creer(RelecturesComponent);
    fixture.detectChanges();
    http.expectOne('/api/etudiants/1/relectures?statut=A_FAIRE').flush(PANNE, EN_PANNE);
    http.expectOne('/api/etudiants/1/relectures?statut=RENDUE').flush([]);
    fixture.detectChanges();
    expect(alertes(page).join(' ')).toContain('momentanément indisponible');
    expect(page.textContent).not.toContain('Aucune relecture en attente.');
    fixture.destroy();
  });

  it('mes notes : l’échec du récapitulatif est affiché, pas avalé', () => {
    const { fixture, page } = creer(MesNotesComponent);
    fixture.detectChanges();
    http.expectOne('/api/etudiants/1/exercices').flush([]);
    http.expectOne('/api/moi/recap').flush(PANNE, EN_PANNE);
    fixture.detectChanges();
    expect(alertes(page).join(' ')).toContain('momentanément indisponible');
    fixture.destroy();
  });

  it('formateur : des sessions rechargées avec succès effacent l’erreur précédente des sessions', () => {
    const { fixture, page } = creer(FormateurComponent, { ...PROFILS.admin, doitChangerMotDePasse: false });
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([{ id: 1, nom: 'P1-2026' }, { id: 2, nom: 'P2-2026' }]);
    http.expectOne('/api/sessions?promotionId=1').flush(PANNE, EN_PANNE);
    fixture.detectChanges();
    expect(alertes(page).length).toBe(1);
    fixture.componentInstance.choisir(2);
    http.expectOne('/api/sessions?promotionId=2').flush([]);
    fixture.detectChanges();
    expect(alertes(page)).toEqual([]);
    fixture.destroy();
  });

  it('tableau : une adresse invalide n’affiche pas en plus « Aucun étudiant »', () => {
    const { fixture, page } = creer(TableauComponent, PROFILS.formateur);
    fixture.componentRef.setInput('promotionId', 'abc');
    fixture.detectChanges();
    expect(alertes(page).join(' ')).toContain('invalide');
    expect(page.textContent).not.toContain('Aucun étudiant dans cette promotion.');
    fixture.destroy();
  });
});
