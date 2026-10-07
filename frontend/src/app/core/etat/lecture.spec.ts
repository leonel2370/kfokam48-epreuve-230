import { signal } from '@angular/core';
import { Subject, defer, of, throwError } from 'rxjs';
import { ErreurApi } from '../api/api.models';
import { Ecriture } from './ecriture';
import { Lecture } from './lecture';

const PANNE: ErreurApi = { code: 'ERREUR_INTERNE', message: 'Indisponible.' };

describe('Lecture et Ecriture — états partagés des écrans (#137)', () => {
  it('est en attente jusqu’à la première réponse, puis lue', () => {
    const lecture = new Lecture<number[]>([]);
    expect(lecture.enAttente()).toBeTrue();
    lecture.charger(of([1, 2]));
    expect(lecture.enAttente()).toBeFalse();
    expect(lecture.lue()).toBeTrue();
    expect(lecture.donnees()).toEqual([1, 2]);
  });

  it('garde ses données et affiche l’erreur quand un rafraîchissement échoue, puis l’efface au succès suivant', () => {
    const lecture = new Lecture<number[]>([]);
    lecture.charger(of([1]));
    lecture.charger(throwError(() => PANNE));
    expect(lecture.donnees()).toEqual([1]);
    expect(lecture.erreur()).toEqual(PANNE);
    lecture.charger(of([1, 2]));
    expect(lecture.erreur()).toBeNull();
  });

  it('n’est ni en attente ni lue après un premier échec : l’écran montre l’erreur, pas une liste vide', () => {
    const lecture = new Lecture<number[]>([]);
    lecture.charger(throwError(() => PANNE));
    expect(lecture.enAttente()).toBeFalse();
    expect(lecture.lue()).toBeFalse();
  });

  it('ne lance pas une seconde écriture tant que la première n’a pas répondu', () => {
    const ecriture = new Ecriture();
    const erreur = signal<ErreurApi | null>(PANNE);
    const reponse = new Subject<string>();
    const recus: string[] = [];
    let abonnements = 0;
    const requete = defer(() => {
      abonnements++;
      return reponse;
    });
    ecriture.lancer(requete, erreur, v => recus.push(v));
    ecriture.lancer(requete, erreur, v => recus.push(v));
    expect(abonnements).toBe(1);
    expect(ecriture.enCours()).toBeTrue();
    expect(erreur()).withContext('l’erreur précédente est effacée au lancement').toBeNull();
    reponse.next('ok');
    expect(recus).toEqual(['ok']);
    expect(ecriture.enCours()).toBeFalse();
  });

  it('range l’erreur d’une écriture dans la zone désignée et libère les boutons', () => {
    const ecriture = new Ecriture();
    const erreur = signal<ErreurApi | null>(null);
    ecriture.lancer(throwError(() => PANNE), erreur, () => fail('ne doit pas réussir'));
    expect(erreur()).toEqual(PANNE);
    expect(ecriture.enCours()).toBeFalse();
  });
});
