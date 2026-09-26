import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { routes } from './app.routes';
import { AuthService } from './core/auth/auth.service';
import { CHEMINS } from './core/navigation/chemins';
import { FOURNISSEURS_TEST, PROFILS } from './testing';

describe('Routes de l’espace étudiant — trois écrans (F2, #104)', () => {
  let harness: RouterTestingHarness;

  beforeEach(async () => {
    TestBed.configureTestingModule({ providers: [...FOURNISSEURS_TEST, provideRouter(routes)] });
    TestBed.inject(AuthService).connecter('awa', 'x').subscribe();
    TestBed.inject(HttpTestingController).expectOne('/api/auth/login').flush(PROFILS.awa);
    harness = await RouterTestingHarness.create();
  });

  it('ouvre l’écran présence et dépôt quand l’étudiant arrive sur son espace', async () => {
    await harness.navigateByUrl(CHEMINS.etudiant);
    expect(TestBed.inject(Router).url).toBe(CHEMINS.etudiantPresence);
    expect(harness.routeNativeElement?.textContent).toContain('Déposer mon exercice');
  });

  it('donne à la relecture son propre écran', async () => {
    await harness.navigateByUrl(CHEMINS.etudiantRelectures);
    const texte = harness.routeNativeElement?.textContent ?? '';
    expect(texte).toContain('Mes relectures à faire');
    expect(texte).not.toContain('Déposer mon exercice');
  });
});
