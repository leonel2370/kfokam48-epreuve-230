import { Component, ElementRef, effect, inject, viewChild } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DialogueService } from '../../core/dialogue/dialogue.service';

/**
 * Affiche la demande courante du DialogueService dans un <dialog> modal natif : le focus y est retenu,
 * Échap annule, et le reste de l'écran est inerte tant que le dialogue est ouvert.
 */
@Component({
  selector: 'app-dialogue',
  standalone: true,
  imports: [FormsModule],
  templateUrl: './dialogue.component.html',
  styleUrl: './dialogue.component.scss',
})
export class DialogueComponent {
  readonly service = inject(DialogueService);
  private readonly cadre = viewChild.required<ElementRef<HTMLDialogElement>>('cadre');

  valeur = '';

  constructor() {
    effect(() => {
      const demande = this.service.demande();
      const cadre = this.cadre().nativeElement;
      if (demande && !cadre.open) {
        this.valeur = demande.champ?.valeur ?? '';
        cadre.showModal();
      } else if (!demande && cadre.open) {
        cadre.close();
      }
    });
  }

  valider(): void {
    const demande = this.service.demande();
    if (demande?.champ && this.valeur === '') {
      return;
    }
    this.service.fermer(this.valeur);
  }

  /** Bouton « Annuler » ou touche Échap : la fermeture passe toujours par le service. */
  annuler(evenement?: Event): void {
    evenement?.preventDefault();
    this.service.fermer(null);
  }
}
