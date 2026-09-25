import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';

describe('AppComponent', () => {
  it('affiche la navigation vers les trois écrans imposés (F2)', async () => {
    await TestBed.configureTestingModule({ imports: [AppComponent], providers: [provideRouter([])] }).compileComponents();
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const liens = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('nav a')).map(a => a.textContent?.trim());
    expect(liens).toEqual(['Présence48', 'Formateur', 'Étudiant', 'Relecteur']);
  });
});
