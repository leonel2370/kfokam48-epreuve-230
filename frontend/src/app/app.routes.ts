import { Routes } from '@angular/router';
import { AccueilComponent } from './features/accueil/accueil.component';

export const routes: Routes = [
  { path: '', component: AccueilComponent },
  { path: '**', redirectTo: '' },
];
