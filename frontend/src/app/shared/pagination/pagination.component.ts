import { Component, input, output } from '@angular/core';

/**
 * Pagination d'une liste lue page par page sur le serveur (DESIGN_SYSTEM Pagination, #138).
 * Le composant n'invente rien : la page, le total et l'existence d'une page suivante viennent de l'appelant.
 */
@Component({
  selector: 'app-pagination',
  standalone: true,
  templateUrl: './pagination.component.html',
})
export class PaginationComponent {
  /** Numéro de page rendu par le serveur, à partir de 0. */
  readonly page = input.required<number>();
  readonly total = input.required<number>();
  readonly aUneSuite = input.required<boolean>();
  /** Nom des éléments comptés, au pluriel : « comptes ». */
  readonly elements = input.required<string>();
  /** Page demandée par l'utilisateur. */
  readonly aller = output<number>();
}
