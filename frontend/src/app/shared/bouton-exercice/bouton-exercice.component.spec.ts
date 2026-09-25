import { TestBed } from '@angular/core/testing';
import { BoutonExerciceComponent } from './bouton-exercice.component';

describe('BoutonExerciceComponent (#104)', () => {
  function creer(lien: string) {
    TestBed.configureTestingModule({ imports: [BoutonExerciceComponent] });
    const fixture = TestBed.createComponent(BoutonExerciceComponent);
    fixture.componentRef.setInput('lien', lien);
    fixture.detectChanges();
    const page = fixture.nativeElement as HTMLElement;
    return { page, bouton: page.querySelector('button') as HTMLButtonElement };
  }

  it("ouvre l'exercice dans un nouvel onglet sans transmettre la page d'origine", () => {
    const open = spyOn(window, 'open');
    const { page, bouton } = creer('https://github.com/awa/tp');
    bouton.click();
    expect(open).toHaveBeenCalledOnceWith('https://github.com/awa/tp', '_blank', 'noopener,noreferrer');
    expect(page.querySelector('.adresse')?.textContent).toBe('https://github.com/awa/tp');
    expect(page.querySelector('a')).toBeNull();
  });

  it("refuse d'ouvrir une adresse qui n'est pas http(s)", () => {
    const open = spyOn(window, 'open');
    const { bouton } = creer('javascript:alert(1)');
    expect(bouton.disabled).toBeTrue();
    bouton.click();
    expect(open).not.toHaveBeenCalled();
  });

  it('désactive le bouton pour une adresse illisible', () => {
    expect(creer('pas une adresse').bouton.disabled).toBeTrue();
  });
});
