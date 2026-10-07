import { TestBed } from '@angular/core/testing';
import { BadgeComponent } from './badge.component';

describe('BadgeComponent (#138)', () => {
  it('applique le ton demandé en plus de la classe badge', () => {
    TestBed.configureTestingModule({ imports: [BadgeComponent] });
    const fixture = TestBed.createComponent(BadgeComponent);
    fixture.componentRef.setInput('ton', 'attente');
    fixture.detectChanges();
    const etiquette = (fixture.nativeElement as HTMLElement).querySelector('span') as HTMLElement;
    expect(etiquette.classList).toContain('badge');
    expect(etiquette.classList).toContain('attente');
  });

  it('est neutre par défaut', () => {
    TestBed.configureTestingModule({ imports: [BadgeComponent] });
    const fixture = TestBed.createComponent(BadgeComponent);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('span')?.classList).toContain('neutre');
  });
});
