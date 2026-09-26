import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { PROFILS } from '../../testing';
import { TableauComponent } from './tableau.component';

describe('TableauComponent (SF-10)', () => {
  it('affiche les valeurs du serveur sans recalcul, « — » pour une moyenne nulle (RG16)', () => {
    TestBed.configureTestingModule({
      imports: [TableauComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(TableauComponent);
    fixture.componentRef.setInput('promotionId', '1');
    fixture.detectChanges();
    http.expectOne('/api/tableau?promotionId=1').flush([
      { etudiantId: 1, nom: 'Awa Ndiaye', presences: 1, exercicesDeposes: 1, moyenne: 14, relecturesEnAttente: 0 },
      { etudiantId: 2, nom: 'Hugo Talla', presences: 0, exercicesDeposes: 0, moyenne: null, relecturesEnAttente: 2 },
    ]);
    fixture.detectChanges();
    const lignes = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr');
    expect(lignes).toHaveSize(2);
    expect(lignes[0].querySelectorAll('td')[3].textContent?.trim()).toBe('14.00');
    expect(lignes[1].querySelectorAll('td')[3].textContent?.trim()).toBe('—');
    expect(lignes[1].querySelectorAll('td')[4].textContent?.trim()).toBe('2');
  });

  function retour(profil: typeof PROFILS.admin): { texte: string; navigate: jasmine.Spy } {
    TestBed.configureTestingModule({
      imports: [TableauComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    TestBed.inject(AuthService).profil.set(profil);
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    const fixture = TestBed.createComponent(TableauComponent);
    fixture.componentRef.setInput('promotionId', '2');
    fixture.detectChanges();
    TestBed.inject(HttpTestingController).expectOne('/api/tableau?promotionId=2').flush([]);
    fixture.detectChanges();
    const bouton = (fixture.nativeElement as HTMLElement).querySelector('button') as HTMLButtonElement;
    bouton.click();
    return { texte: bouton.textContent?.trim() ?? '', navigate };
  }

  it('ramène le formateur aux sessions de cette promotion (#104)', () => {
    const { texte, navigate } = retour(PROFILS.formateur);
    expect(texte).toBe('← Retour aux sessions');
    expect(navigate).toHaveBeenCalledWith(['/formateur'], { queryParams: { promotionId: '2' } });
  });

  it("ramène l'admin à l'administration (#104)", () => {
    const { texte, navigate } = retour({ ...PROFILS.admin, doitChangerMotDePasse: false });
    expect(texte).toBe("← Retour à l'administration");
    expect(navigate).toHaveBeenCalledWith(['/admin'], { queryParams: undefined });
  });

  it('#108 — le titre du tableau porte le nom de la promotion', () => {
    TestBed.configureTestingModule({
      imports: [TableauComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(TableauComponent);
    fixture.componentRef.setInput('promotionId', '1');
    fixture.detectChanges();
    http.expectOne('/api/promotions').flush([{ id: 1, nom: 'P1-2026' }]);
    http.expectOne('/api/tableau?promotionId=1').flush([
      { etudiantId: 1, nom: 'Awa Ndiaye', presences: 1, exercicesDeposes: 1, moyenne: 14, relecturesEnAttente: 0 }]);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('h2')?.textContent).toContain('P1-2026');
    fixture.destroy();
  });

  it("#108 — un identifiant non numérique dans l'adresse n'appelle pas le serveur et affiche un message", () => {
    TestBed.configureTestingModule({
      imports: [TableauComponent],
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(TableauComponent);
    fixture.componentRef.setInput('promotionId', 'abc');
    fixture.detectChanges();
    http.expectNone('/api/tableau?promotionId=abc');
    fixture.detectChanges();
    const page = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(page).toContain('invalide');
    expect(page).not.toContain('Chargement…');
    fixture.destroy();
  });
});
