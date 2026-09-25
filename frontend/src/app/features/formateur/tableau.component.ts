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
  templateUrl: './tableau.component.html',
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
