import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ErreurApi, LigneTableau } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { ErreurComponent } from '../../shared/erreur.component';

/** SF-10 / EF10 : affichage brut du tableau calculé par le serveur, aucun recalcul (F3). */
@Component({
  selector: 'app-tableau',
  standalone: true,
  imports: [DecimalPipe, RouterLink, ErreurComponent],
  template: `
    <main>
      <section class="carte">
        <h2>Tableau de la promotion</h2>
        <app-erreur [erreur]="erreur()" />
        @if (chargement()) {
          <p class="discret">Chargement…</p>
        } @else if (lignes().length === 0 && !erreur()) {
          <p class="discret">Aucun étudiant dans cette promotion.</p>
        } @else if (lignes().length > 0) {
          <table>
            <thead>
              <tr>
                <th>Étudiant</th><th class="nombre">Présences</th><th class="nombre">Exercices</th>
                <th class="nombre">Moyenne</th><th class="nombre">Relectures en attente</th>
              </tr>
            </thead>
            <tbody>
              @for (l of lignes(); track l.etudiantId) {
                <tr>
                  <td>{{ l.nom }}</td>
                  <td class="nombre">{{ l.presences }}</td>
                  <td class="nombre">{{ l.exercicesDeposes }}</td>
                  <td class="nombre">{{ l.moyenne === null ? '—' : (l.moyenne | number: '1.2-2') }}</td>
                  <td class="nombre">
                    @if (l.relecturesEnAttente > 0) { <span class="badge EN_ATTENTE_RELECTURE">{{ l.relecturesEnAttente }}</span> }
                    @else { 0 }
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
        <p><a routerLink="/formateur">← Retour aux sessions</a></p>
      </section>
    </main>
  `,
})
export class TableauComponent implements OnInit {
  private readonly api = inject(ApiService);

  /** Paramètre de route :promotionId (withComponentInputBinding). */
  readonly promotionId = input.required<string>();
  readonly lignes = signal<LigneTableau[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);
  readonly chargement = signal(true);

  ngOnInit(): void {
    this.api.tableau(Number(this.promotionId())).subscribe({
      next: l => { this.lignes.set(l); this.chargement.set(false); },
      error: (e: ErreurApi) => { this.erreur.set(e); this.chargement.set(false); },
    });
  }
}
