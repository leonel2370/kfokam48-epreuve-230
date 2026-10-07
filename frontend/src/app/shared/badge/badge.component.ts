import { Component, input } from '@angular/core';
import { TonBadge } from '../../core/libelles/libelles';

/** Étiquette d'état (DESIGN_SYSTEM Badge) : le libellé est projeté, le ton n'ajoute qu'une couleur. */
@Component({
  selector: 'app-badge',
  standalone: true,
  templateUrl: './badge.component.html',
  styleUrl: './badge.component.scss',
})
export class BadgeComponent {
  readonly ton = input<TonBadge>('neutre');
}
