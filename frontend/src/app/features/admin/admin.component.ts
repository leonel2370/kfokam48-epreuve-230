import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ErreurApi, Promotion } from '../../core/api/api.models';
import { ApiService } from '../../core/api/api.service';
import { ErreurComponent } from '../../shared/erreur.component';

/**
 * Espace ADMIN (cahier §2 bis) : toutes les promotions, leur tableau et l'ouverture de session.
 * Les écrans de gestion (comptes, promotions, étudiants : #60–#62) ne sont pas livrés : c'est affiché.
 */
@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [RouterLink, ErreurComponent],
  template: `
    <main>
      <section class="carte">
        <h2>Promotions</h2>
        @for (p of promotions(); track p.id) {
          <p class="ligne">
            <strong>{{ p.nom }}</strong>
            <a [routerLink]="['/formateur/tableau', p.id]">Tableau</a>
            <a routerLink="/formateur">Sessions</a>
          </p>
        } @empty { <p class="discret">Aucune promotion.</p> }
        <app-erreur [erreur]="erreur()" />
      </section>
      <section class="carte">
        <h2>Gestion des comptes, promotions et étudiants</h2>
        <p class="discret">
          Non livrée dans cette version : reportée au backlog (#60, #61, #62), voir le cahier des charges §7.2 ter.
          Les comptes de démonstration sont créés au démarrage (README).
        </p>
      </section>
    </main>
  `,
})
export class AdminComponent implements OnInit {
  private readonly api = inject(ApiService);
  readonly promotions = signal<Promotion[]>([]);
  readonly erreur = signal<ErreurApi | null>(null);

  ngOnInit(): void {
    this.api.promotions().subscribe({ next: p => this.promotions.set(p), error: (e: ErreurApi) => this.erreur.set(e) });
  }
}
