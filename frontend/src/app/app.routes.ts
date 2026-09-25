import { Routes } from '@angular/router';
import { AccueilComponent } from './features/accueil/accueil.component';

export const routes: Routes = [
  { path: '', component: AccueilComponent },
  {
    path: 'formateur',
    loadComponent: () => import('./features/formateur/formateur.component').then(m => m.FormateurComponent),
  },
  { path: '**', redirectTo: '' },
];
