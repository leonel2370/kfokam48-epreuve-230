# Frontend Présence48 — Angular 17

Client web de Présence48 (suivi de présence et relecture par les pairs). Angular 17 en composants **standalone**, TypeScript strict. La documentation du projet (démarrage, comptes de démonstration, parcours, architecture complète) est à la racine : [README.md](../README.md) · [ARCHITECTURE.md](ARCHITECTURE.md) · [CONTRIBUTING.md](../docs/CONTRIBUTING.md).

## Commandes

```bash
npm ci                    # installation
npm start                 # serveur de développement sur http://localhost:4200 (proxy /api → :8080, proxy.conf.json)
npm run build             # build de production dans dist/
npx ng test --watch=false --browsers=ChromeHeadless   # tests unitaires (Karma)
```

La même chose sans installer Node : `docker compose up --build` depuis la racine.

## Organisation

```text
src/app/
  core/
    api/            Seule couche autorisée à appeler HttpClient (F3) — un service typé par ressource
    auth/           Session, profil, gardes de routes par rôle
    navigation/     Libellés partagés (rôles, statuts) et chemins
    interceptors/   XSRF, erreurs au format {code, message}
  features/
    connexion/      Écran de connexion
    etudiant/       Espace étudiant : espace-etudiant, presence, mes-notes, relectures (F2, trois écrans)
    formateur/      Sessions et tableau de la promotion
    profil/         Profil connecté, changement de mot de passe
    admin/          Administration des promotions
  shared/           Composants réutilisables : barre-navigation, bouton-navigation, bouton-exercice, erreur
```

Règles du projet (F3) : aucune règle métier dans le frontend (le serveur calcule), les post-conditions sont testées sur les signaux Angular dans les `.spec.ts` (helpers de test dans `src/app/testing`).
