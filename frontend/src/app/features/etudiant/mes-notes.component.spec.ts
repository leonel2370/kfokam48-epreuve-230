import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { erreurInterceptor } from '../../core/interceptors/erreur.interceptor';
import { MesNotesComponent } from './mes-notes.component';

describe('MesNotesComponent (#88, RG31)', () => {
  it('affiche la note retenue du serveur et la marque provisoire si un seul pair a rendu', () => {
    TestBed.configureTestingModule({
      imports: [MesNotesComponent],
      providers: [provideHttpClient(withInterceptors([erreurInterceptor])), provideHttpClientTesting()],
    });
    const http = TestBed.inject(HttpTestingController);
    const fixture = TestBed.createComponent(MesNotesComponent);
    fixture.componentRef.setInput('etudiantId', 1);
    fixture.componentInstance.login = 'awa';
    fixture.componentInstance.motDePasse = 'Etudiant48';
    fixture.componentInstance.connecter();
    http.expectOne('/api/auth/login').flush({});
    http.expectOne('/api/etudiants/1/exercices').flush([
      { id: 1, sessionId: 1, sessionTitre: 'TP JPA', lien: 'https://x.cm', statut: 'EN_ATTENTE_RELECTURE',
        noteRetenue: 12, provisoire: true, commentaires: ['Clair'] },
      { id: 2, sessionId: 2, sessionTitre: 'TP Flyway', lien: 'https://y.cm', statut: 'RELU',
        noteRetenue: 14.5, provisoire: false, commentaires: ['Bien', 'Tests à ajouter'] },
    ]);
    fixture.detectChanges();
    const texte = (fixture.nativeElement as HTMLElement).textContent ?? '';
    expect(texte).toContain('12 / 20');
    expect(texte).toContain('provisoire');
    expect(texte).toContain('14.5 / 20');
    expect(texte).toContain('définitive');
  });
});
