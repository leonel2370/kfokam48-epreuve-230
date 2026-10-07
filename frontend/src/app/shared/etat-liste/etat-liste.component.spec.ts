import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { Lecture } from '../../core/etat/lecture';
import { EtatListeComponent } from './etat-liste.component';

describe('EtatListeComponent (#138)', () => {
  function creer(lecture: Lecture<number[]>) {
    TestBed.configureTestingModule({ imports: [EtatListeComponent] });
    const fixture = TestBed.createComponent(EtatListeComponent);
    fixture.componentRef.setInput('lecture', lecture);
    fixture.componentRef.setInput('nombre', lecture.donnees().length);
    fixture.componentRef.setInput('vide', 'Aucun compte.');
    fixture.detectChanges();
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('affiche « Chargement… » avant la première réponse, jamais le texte de liste vide', () => {
    const texte = creer(new Lecture<number[]>([]));
    expect(texte).toContain('Chargement…');
    expect(texte).not.toContain('Aucun compte.');
  });

  it('affiche le texte de liste vide quand la lecture a réussi sans élément', () => {
    const lecture = new Lecture<number[]>([]);
    lecture.charger(of([]));
    expect(creer(lecture)).toContain('Aucun compte.');
  });

  it('affiche l’erreur de la lecture, sans la faire passer pour une liste vide', () => {
    const lecture = new Lecture<number[]>([]);
    lecture.charger(throwError(() => ({ code: 'ERREUR_INTERNE', message: 'Indisponible.' })));
    const texte = creer(lecture);
    expect(texte).toContain('Indisponible.');
    expect(texte).not.toContain('Aucun compte.');
    expect(texte).not.toContain('Chargement…');
  });
});
