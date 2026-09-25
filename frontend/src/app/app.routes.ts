import { Routes } from '@angular/router';
import { accesGuard, redirectionGuard } from './core/auth/acces.guard';
import { PARAM_PROMOTION, SEGMENTS } from './core/navigation/chemins';

/** Arborescence v3 (spécifications §1.2, #104) : /connexion est la seule page publique. */
export const routes: Routes = [
  { path: '', pathMatch: 'full', canActivate: [redirectionGuard], children: [] },
  {
    path: SEGMENTS.connexion,
    canActivate: [redirectionGuard],
    loadComponent: () => import('./features/connexion/connexion.component').then(m => m.ConnexionComponent),
  },
  {
    path: SEGMENTS.profil,
    canActivate: [accesGuard()],
    loadComponent: () => import('./features/profil/profil.component').then(m => m.ProfilComponent),
  },
  {
    path: SEGMENTS.admin,
    canActivate: [accesGuard('ADMIN')],
    loadComponent: () => import('./features/admin/admin.component').then(m => m.AdminComponent),
  },
  {
    path: SEGMENTS.formateur,
    canActivate: [accesGuard('FORMATEUR', 'ADMIN')],
    loadComponent: () => import('./features/formateur/formateur.component').then(m => m.FormateurComponent),
  },
  {
    path: `${SEGMENTS.formateur}/${SEGMENTS.tableau}/:${PARAM_PROMOTION}`,
    canActivate: [accesGuard('FORMATEUR', 'ADMIN')],
    loadComponent: () => import('./features/formateur/tableau.component').then(m => m.TableauComponent),
  },
  {
    // Trois écrans (F2) : présence et dépôt (étudiant), notes, relectures (relecteur).
    path: SEGMENTS.etudiant,
    canActivate: [accesGuard('ETUDIANT')],
    loadComponent: () => import('./features/etudiant/espace-etudiant.component').then(m => m.EspaceEtudiantComponent),
    children: [
      { path: '', pathMatch: 'full', redirectTo: SEGMENTS.presence },
      {
        path: SEGMENTS.presence,
        loadComponent: () => import('./features/etudiant/presence.component').then(m => m.PresenceComponent),
      },
      {
        path: SEGMENTS.notes,
        loadComponent: () => import('./features/etudiant/mes-notes.component').then(m => m.MesNotesComponent),
      },
      {
        path: SEGMENTS.relectures,
        loadComponent: () => import('./features/etudiant/relectures.component').then(m => m.RelecturesComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
