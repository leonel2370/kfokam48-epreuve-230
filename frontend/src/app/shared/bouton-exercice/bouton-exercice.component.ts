import { Component, computed, input } from '@angular/core';

const PROTOCOLES_OUVRABLES = new Set(['http:', 'https:']);

/**
 * Lien d'un exercice (adresse externe) présenté comme un bouton « Ouvrir l'exercice » (spécifications §1.2 bis,
 * #104) : nouvel onglet sans transmettre la page d'origine ; seules les adresses http(s) s'ouvrent.
 * L'adresse reste lisible et copiable sous le bouton.
 */
@Component({
  selector: 'app-bouton-exercice',
  standalone: true,
  templateUrl: './bouton-exercice.component.html',
  styleUrl: './bouton-exercice.component.scss',
})
export class BoutonExerciceComponent {
  readonly lien = input.required<string>();

  readonly ouvrable = computed(() => {
    try {
      return PROTOCOLES_OUVRABLES.has(new URL(this.lien()).protocol);
    } catch {
      return false;
    }
  });

  ouvrir(): void {
    if (this.ouvrable()) {
      window.open(this.lien(), '_blank', 'noopener,noreferrer');
    }
  }
}
