import { Component, NgZone } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { BoutonNavigationComponent } from './bouton-navigation.component';

@Component({ standalone: true, template: '' })
class VideComponent {}

describe('BoutonNavigationComponent (#104)', () => {
  let router: Router;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [BoutonNavigationComponent],
      providers: [provideRouter([
        { path: 'etudiant/notes', component: VideComponent },
        { path: 'etudiant/presence', component: VideComponent },
        { path: 'formateur', component: VideComponent },
      ])],
    });
    router = TestBed.inject(Router);
  });

  function creer(chemin: string, variante: 'menu' | 'secondaire' = 'menu') {
    const fixture = TestBed.createComponent(BoutonNavigationComponent);
    fixture.componentRef.setInput('chemin', chemin);
    fixture.componentRef.setInput('variante', variante);
    fixture.detectChanges();
    return fixture;
  }

  const bouton = (f: { nativeElement: HTMLElement }) => f.nativeElement.querySelector('button') as HTMLButtonElement;
  const aller = (url: string) => TestBed.inject(NgZone).run(() => router.navigateByUrl(url));

  it('est un vrai bouton, pas un lien', () => {
    const fixture = creer('/etudiant/notes');
    expect(bouton(fixture).type).toBe('button');
    expect((fixture.nativeElement as HTMLElement).querySelector('a')).toBeNull();
  });

  it('mène au chemin demandé, avec ses paramètres d’adresse', () => {
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    const fixture = creer('/formateur', 'secondaire');
    fixture.componentRef.setInput('parametres', { promotionId: 2 });
    bouton(fixture).click();
    expect(navigate).toHaveBeenCalledWith(['/formateur'], { queryParams: { promotionId: 2 } });
  });

  it('marque le menu de l’écran affiché, et lui seul, avec aria-current', async () => {
    const notes = creer('/etudiant/notes');
    const presence = creer('/etudiant/presence');
    await aller('/etudiant/notes');
    notes.detectChanges();
    presence.detectChanges();
    expect(bouton(notes).getAttribute('aria-current')).toBe('page');
    expect(bouton(presence).getAttribute('aria-current')).toBeNull();
  });

  it('reste actif sur un sous-écran de son chemin', async () => {
    const espace = creer('/etudiant');
    await aller('/etudiant/presence');
    espace.detectChanges();
    expect(bouton(espace).getAttribute('aria-current')).toBe('page');
  });

  it('ne marque jamais un bouton d’action comme écran courant', async () => {
    const retour = creer('/formateur', 'secondaire');
    await aller('/formateur');
    retour.detectChanges();
    expect(bouton(retour).getAttribute('aria-current')).toBeNull();
  });
});
