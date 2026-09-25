import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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
});
