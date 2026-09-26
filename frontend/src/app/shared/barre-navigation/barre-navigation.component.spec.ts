import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { BarreNavigationComponent } from './barre-navigation.component';

describe('BarreNavigationComponent (#104)', () => {
  it('affiche un bouton par entrée, dans une navigation nommée', () => {
    TestBed.configureTestingModule({ imports: [BarreNavigationComponent], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(BarreNavigationComponent);
    fixture.componentRef.setInput('libelle', 'Espace étudiant');
    fixture.componentRef.setInput('entrees', [
      { chemin: '/etudiant/presence', libelle: 'Présence et dépôt' },
      { chemin: '/etudiant/notes', libelle: 'Mes notes' },
    ]);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    expect(page.querySelector('nav')?.getAttribute('aria-label')).toBe('Espace étudiant');
    expect(Array.from(page.querySelectorAll('button')).map(b => b.textContent?.trim()))
      .toEqual(['Présence et dépôt', 'Mes notes']);
    expect(page.querySelector('a')).toBeNull();
  });
});
