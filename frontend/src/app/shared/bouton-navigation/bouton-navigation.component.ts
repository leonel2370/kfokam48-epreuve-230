import { Component, computed, inject, input } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router } from '@angular/router';
import { filter, map } from 'rxjs';

/** menu : entrée de menu ou d'onglet, marquée active ; secondaire / principal : action qui change d'écran. */
export type VarianteBouton = 'menu' | 'secondaire' | 'principal';

/**
 * Navigation interne par bouton, jamais par lien texte (spécifications §1.2 bis, #104).
 * Un bouton « menu » porte aria-current="page" sur son écran et sur ses sous-écrans.
 */
@Component({
  selector: 'app-bouton-navigation',
  standalone: true,
  templateUrl: './bouton-navigation.component.html',
  styleUrl: './bouton-navigation.component.scss',
})
export class BoutonNavigationComponent {
  private readonly router = inject(Router);

  readonly chemin = input.required<string>();
  readonly parametres = input<Record<string, string | number> | undefined>(undefined);
  readonly variante = input<VarianteBouton>('menu');
  readonly titre = input<string | null>(null);

  private readonly url = toSignal(
    this.router.events.pipe(
      filter((e): e is NavigationEnd => e instanceof NavigationEnd),
      map(e => e.urlAfterRedirects),
    ),
    { initialValue: this.router.url },
  );

  readonly actif = computed(() => {
    if (this.variante() !== 'menu') {
      return false;
    }
    const courant = this.url().split(/[?#]/)[0];
    return courant === this.chemin() || courant.startsWith(`${this.chemin()}/`);
  });

  naviguer(): void {
    void this.router.navigate([this.chemin()], { queryParams: this.parametres() });
  }
}
