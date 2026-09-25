import { Routes } from '@angular/router';
import { accesGuard, redirectionGuard } from './core/auth/acces.guard';

/** Arborescence v2 (spécifications §1.2) : /connexion est la seule page publique. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', canActivate: [redirectionGuard], children: [] },
  {
    path: 'connexion',
    canActivate: [redirectionGuard],
    loadComponent: () => import('./features/connexion/connexion.component').then(m => m.ConnexionComponent),
  },
  {
    path: 'profil',
    canActivate: [accesGuard()],
    loadComponent: () => import('./features/profil/profil.component').then(m => m.ProfilComponent),
  },
  {
    path: 'admin',
    canActivate: [accesGuard('ADMIN')],
    loadComponent: () => import('./features/admin/admin.component').then(m => m.AdminComponent),
  },
  {
    path: 'formateur',
    canActivate: [accesGuard('FORMATEUR', 'ADMIN')],
    loadComponent: () => import('./features/formateur/formateur.component').then(m => m.FormateurComponent),
  },
  {
    path: 'formateur/tableau/:promotionId',
    canActivate: [accesGuard('FORMATEUR', 'ADMIN')],
    loadComponent: () => import('./features/formateur/tableau.component').then(m => m.TableauComponent),
  },
  {
    path: 'etudiant',
    canActivate: [accesGuard('ETUDIANT')],
    loadComponent: () => import('./features/etudiant/etudiant.component').then(m => m.EtudiantComponent),
  },
  { path: '**', redirectTo: '' },
];
