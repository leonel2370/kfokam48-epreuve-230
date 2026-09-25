import { Routes } from '@angular/router';
import { AccueilComponent } from './features/accueil/accueil.component';

export const routes: Routes = [
  { path: '', component: AccueilComponent },
  {
    path: 'formateur',
    loadComponent: () => import('./features/formateur/formateur.component').then(m => m.FormateurComponent),
  },
  {
    path: 'formateur/tableau/:promotionId',
    loadComponent: () => import('./features/formateur/tableau.component').then(m => m.TableauComponent),
  },
  {
    path: 'etudiant',
    loadComponent: () => import('./features/etudiant/etudiant.component').then(m => m.EtudiantComponent),
  },
  { path: '**', redirectTo: '' },
];
