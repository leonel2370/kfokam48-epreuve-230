import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, Router, RouterStateSnapshot, UrlTree } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { Profil } from '../api/api.models';
import { FOURNISSEURS_TEST, PROFILS } from '../../testing';
import { accesGuard, redirectionGuard } from './acces.guard';
import { AuthService } from './auth.service';

type Resultat = boolean | UrlTree;

describe('Accès par rôle (SF-19, RG23)', () => {
  let http: HttpTestingController;
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: FOURNISSEURS_TEST });
    http = TestBed.inject(HttpTestingController);
    router = TestBed.inject(Router);
  });

  function executer(garde: ReturnType<typeof accesGuard>, url: string, profil: Profil | null): Promise<Resultat> {
    const resultat = TestBed.runInInjectionContext(() =>
      garde({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot)) as Observable<Resultat>;
    const promesse = firstValueFrom(resultat);
    const req = http.expectOne('/api/moi');
    if (profil) {
      req.flush(profil);
    } else {
      req.flush({ code: 'NON_AUTHENTIFIE', message: 'x' }, { status: 401, statusText: 'Unauthorized' });
    }
    return promesse;
  }

  const url = (r: Resultat) => (r instanceof UrlTree ? router.serializeUrl(r) : r);

  it('renvoie un visiteur non connecté vers /connexion', async () => {
    expect(url(await executer(accesGuard('FORMATEUR'), '/formateur', null))).toBe('/connexion');
  });

  it("renvoie un étudiant qui ouvre l'espace formateur vers son propre espace", async () => {
    expect(url(await executer(accesGuard('FORMATEUR', 'ADMIN'), '/formateur', PROFILS.awa))).toBe('/etudiant');
  });

  it('laisse le formateur entrer dans son espace', async () => {
    expect(await executer(accesGuard('FORMATEUR', 'ADMIN'), '/formateur', PROFILS.formateur)).toBeTrue();
  });

  it('impose le changement de mot de passe à l’admin par défaut (RG23)', async () => {
    expect(url(await executer(accesGuard('ADMIN'), '/admin', PROFILS.admin))).toBe('/profil');
  });

  it("envoie un utilisateur déjà connecté de l'accueil vers son espace", async () => {
    expect(url(await executer(redirectionGuard, '/', PROFILS.formateur))).toBe('/formateur');
  });

  it("donne l'espace de chaque rôle", () => {
    expect(AuthService.espace(PROFILS.awa)).toBe('/etudiant');
    expect(AuthService.espace(PROFILS.formateur)).toBe('/formateur');
    expect(AuthService.espace({ ...PROFILS.admin, doitChangerMotDePasse: false })).toBe('/admin');
  });
});
