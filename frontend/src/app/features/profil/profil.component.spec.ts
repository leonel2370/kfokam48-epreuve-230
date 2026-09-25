import { HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { FOURNISSEURS_TEST, PROFILS } from '../../testing';
import { ProfilComponent } from './profil.component';

describe('ProfilComponent (SF-17, SF-18)', () => {
  it("change le mot de passe imposé puis envoie l'admin dans son espace (RG23)", () => {
    TestBed.configureTestingModule({ imports: [ProfilComponent], providers: FOURNISSEURS_TEST });
    const http = TestBed.inject(HttpTestingController);
    const router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl').and.resolveTo(true);
    TestBed.inject(AuthService).profil.set(PROFILS.admin);
    const fixture = TestBed.createComponent(ProfilComponent);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Premier accès');
    const c = fixture.componentInstance;
    c.ancien = 'admin';
    c.nouveau = 'NouveauMdp48';
    c.changer();
    const put = http.expectOne('/api/moi/mot-de-passe');
    expect(put.request.method).toBe('PUT');
    expect(put.request.body).toEqual({ ancien: 'admin', nouveau: 'NouveauMdp48' });
    put.flush(null);
    http.expectOne('/api/moi').flush({ ...PROFILS.admin, doitChangerMotDePasse: false });
    expect(router.navigateByUrl).toHaveBeenCalledWith('/admin');
  });
});
