import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/** Accueil (arborescence §1.2) : choix du rôle, sans connexion pour les parcours imposés (RG22). */
@Component({
  selector: 'app-accueil',
  standalone: true,
  imports: [RouterLink],
  template: `
    <main>
      <section class="carte">
        <h2>Suivi de présence et relecture par les pairs</h2>
        <div class="ligne">
          <a routerLink="/formateur"><button>Je suis formateur</button></a>
          <a routerLink="/etudiant"><button class="secondaire">Je suis étudiant</button></a>
        </div>
      </section>
    </main>
  `,
})
export class AccueilComponent {}
