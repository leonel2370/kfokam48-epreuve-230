import { DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, input, signal } from '@angular/core';
import { ErreurApi, ExerciceAuteur } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { ErreurComponent } from '../../shared/erreur.component';

/**
 * SF-14 / #88 : note retenue de chaque exercice (moyenne des deux pairs, RG16 v3), marquée « provisoire »
 * tant qu'un seul a rendu (RG31), avec les commentaires reçus, sans les relecteurs (RG8).
 */
@Component({
  selector: 'app-mes-notes',
  standalone: true,
  imports: [DecimalPipe, ErreurComponent],
  template: `
    <section class="carte">
      <h2>Mes exercices et mes notes</h2>
      @for (x of exercices(); track x.id) {
        <div class="ligne">
          <strong>{{ x.sessionTitre }}</strong>
          <a [href]="x.lien" target="_blank" rel="noopener">lien</a>
          @if (x.noteRetenue === null) {
            <span class="discret">en attente de relecture</span>
          } @else {
            <span>{{ x.noteRetenue | number: '1.0-2' }} / 20</span>
            @if (x.provisoire) { <span class="badge EN_ATTENTE_RELECTURE">provisoire</span> }
            @else { <span class="badge RELU">définitive</span> }
          }
        </div>
        @for (c of x.commentaires; track $index) { <p class="discret">« {{ c }} »</p> }
      } @empty { <p class="discret">Aucun exercice déposé.</p> }
      <app-erreur [erreur]="erreur()" />
    </section>
  `,
})
export class MesNotesComponent implements OnInit {
  private readonly api = inject(ApiService);

  readonly etudiantId = input.required<number>();
  readonly exercices = signal<ExerciceAuteur[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);

  ngOnInit(): void {
    this.charger();
  }

  charger(): void {
    this.api.mesExercices(this.etudiantId()).subscribe({
      next: l => this.exercices.set(l),
      error: (e: ErreurApi) => this.erreur.set(e),
    });
  }
}
