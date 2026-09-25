import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { AppComponent } from './app.component';
import { AuthService } from './core/auth/auth.service';
import { FOURNISSEURS_TEST, PROFILS } from './testing';

describe('AppComponent — en-tête par rôle (spécifications §1.1 v2)', () => {
  beforeEach(() => TestBed.configureTestingModule({ imports: [AppComponent], providers: FOURNISSEURS_TEST }));

  function liens(): string[] {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    return Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('nav a, nav button'))
      .map(e => e.textContent?.trim() ?? '');
  }

  it("n'affiche aucun menu tant que personne n'est connecté", () => {
    expect(liens()).toEqual(['Présence48']);
  });

  it("n'affiche à l'étudiant que son espace, son nom et la déconnexion", () => {
    TestBed.inject(AuthService).profil.set(PROFILS.awa);
    expect(liens()).toEqual(['Présence48', 'Mon espace', 'Awa Ndiaye · ETUDIANT', 'Se déconnecter']);
  });

  it("n'affiche au formateur que ses sessions", () => {
    TestBed.inject(AuthService).profil.set(PROFILS.formateur);
    expect(liens()).toEqual(['Présence48', 'Mes sessions', 'Jean Fokam · FORMATEUR', 'Se déconnecter']);
  });

  it('déconnecte côté serveur puis oublie le profil (SF-16)', () => {
    const auth = TestBed.inject(AuthService);
    auth.profil.set(PROFILS.awa);
    const fixture = TestBed.createComponent(AppComponent);
    fixture.componentInstance.deconnecter();
    TestBed.inject(HttpTestingController).expectOne('/api/auth/logout').flush(null);
    expect(auth.profil()).toBeNull();
  });
});
