import { TestBed } from '@angular/core/testing';
import { PaginationComponent } from './pagination.component';

describe('PaginationComponent (#138)', () => {
  function creer(page: number, aUneSuite: boolean) {
    TestBed.configureTestingModule({ imports: [PaginationComponent] });
    const fixture = TestBed.createComponent(PaginationComponent);
    fixture.componentRef.setInput('page', page);
    fixture.componentRef.setInput('total', 42);
    fixture.componentRef.setInput('aUneSuite', aUneSuite);
    fixture.componentRef.setInput('elements', 'comptes');
    const demandes: number[] = [];
    fixture.componentInstance.aller.subscribe(p => demandes.push(p));
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    return { el, demandes, boutons: () => Array.from(el.querySelectorAll('button')).map(b => b.textContent?.trim()) };
  }

  it('annonce le total et la page, comptée à partir de 1', () => {
    const { el } = creer(1, true);
    expect(el.textContent).toContain('42 comptes · page 2');
    expect(el.querySelector('nav')?.getAttribute('aria-label')).toBe('Pagination des comptes');
  });

  it('ne propose pas de page précédente sur la première page, ni de suivante sur la dernière', () => {
    expect(creer(0, true).boutons()).toEqual(['Page suivante']);
    TestBed.resetTestingModule();
    expect(creer(2, false).boutons()).toEqual(['Page précédente']);
  });

  it('demande la page voisine', () => {
    const { el, demandes } = creer(1, true);
    el.querySelectorAll('button').forEach(b => b.click());
    expect(demandes).toEqual([0, 2]);
  });
});
