import { HttpTestingController } from '@angular/common/http/testing';
import { NgZone } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { AuthService } from './core/auth/auth.service';
import { SEGMENTS } from './core/navigation/chemins';
import { FOURNISSEURS_TEST, PROFILS } from './testing';

describe('AppComponent — en-tête par rôle, en boutons (spécifications §1.2 bis, #104)', () => {
  beforeEach(() => TestBed.configureTestingModule({
    imports: [AppComponent],
    providers: [...FOURNISSEURS_TEST, provideRouter([{ path: SEGMENTS.connexion, children: [] }])],
  }));

  function boutons(): string[] {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const entete = (fixture.nativeElement as HTMLElement).querySelector('header') as HTMLElement;
    expect(entete.querySelector('a')).withContext('aucun lien texte dans l’en-tête').toBeNull();
    return Array.from(entete.querySelectorAll('button')).map(e => e.textContent?.trim() ?? '');
  }

  it("n'affiche aucun menu tant que personne n'est connecté", () => {
    expect(boutons()).toEqual([]);
  });

  it("n'affiche à l'étudiant que son espace, son nom et la déconnexion", () => {
    TestBed.inject(AuthService).profil.set(PROFILS.awa);
    expect(boutons()).toEqual(['Mon espace', 'Awa Ndiaye · Étudiant', 'Se déconnecter']);
  });

  it("n'affiche au formateur que ses sessions", () => {
    TestBed.inject(AuthService).profil.set(PROFILS.formateur);
    expect(boutons()).toEqual(['Mes sessions', 'Jean Fokam · Formateur', 'Se déconnecter']);
  });

  it('déconnecte côté serveur puis oublie le profil (SF-16)', () => {
    const auth = TestBed.inject(AuthService);
    auth.profil.set(PROFILS.awa);
    const fixture = TestBed.createComponent(AppComponent);
    fixture.componentInstance.deconnecter();
    const deconnexion = TestBed.inject(HttpTestingController).expectOne('/api/auth/logout');
    TestBed.inject(NgZone).run(() => deconnexion.flush(null));
    expect(auth.profil()).toBeNull();
  });
});
