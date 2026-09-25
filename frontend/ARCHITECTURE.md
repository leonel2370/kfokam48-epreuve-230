# Architecture Angular 17

Application standalone (sans NgModule), signaux, routes chargées à la demande. Squelette généré par
`createAngularStructure.sh`, puis adapté au projet (#13, #59, #103).

## Structure

```text
src/app/
├── core/                  infrastructure globale, sans écran
│   ├── api/               api.models.ts (types du contrat) · api.service.ts (seul accès HTTP)
│   ├── auth/              auth.service.ts (profil connecté) · acces.guard.ts (gardes par rôle)
│   ├── interceptors/      erreur (toute erreur → { code, message }) · session (401 → connexion)
│   └── navigation/        chemins.ts : seule source des adresses de l'application (#104)
├── shared/                composants réutilisables, un dossier chacun, sans appel HTTP :
│                          bouton-navigation · barre-navigation · bouton-exercice · erreur
├── features/              un dossier par espace : connexion, profil, admin, formateur,
│                          etudiant (espace-etudiant + écrans presence, mes-notes, relectures)
├── app.component.*        en-tête (menus du rôle, profil, déconnexion, en boutons) + <router-outlet>
├── app.routes.ts          routes et gardes
└── app.config.ts          routeur, HttpClient, intercepteurs, XSRF (cookie XSRF-TOKEN, même origine)
```

## Règles

- **Aucune règle métier côté client** (F3) : le serveur décide et calcule ; le frontend affiche.
- **Un seul accès HTTP** : `core/api/api.service.ts`. Le frontend appelle `/api` en même origine
  (`proxy.conf.json` en développement, nginx en production).
- **Un composant = quatre fichiers** (#103) :

  ```text
  x.component.ts      logique (signaux, appels à ApiService)
  x.component.html    template
  x.component.scss    styles propres au composant (seulement s'il y en a)
  x.component.spec.ts tests
  ```

  Jamais de `template:` / `styles:` en ligne ni d'attribut `style="…"`. Un style utile à plusieurs écrans va dans
  `src/styles.scss` (classes utilitaires : `.carte`, `.ligne`, `.ligne.entre`, `.formulaire`, `.extensible`,
  `.badge`, `.alerte`, `.succes`, `.discret`), sinon dans le `.scss` du composant.
- **Composants réutilisables d'abord** : tout élément d'interface répété entre écrans va dans `shared/`
  (entrées par `input()`, sorties par `output()`, pas d'accès HTTP). Les écrans de `features/` les assemblent.
- **Navigation par boutons** (#104, spécifications §1.2 bis) : aucune balise `<a>`, aucun `routerLink`, aucun
  `[href]`. Navigation interne : `app-bouton-navigation` / `app-barre-navigation` ; adresse d'exercice :
  `app-bouton-exercice` (« Ouvrir l'exercice »). Les chemins viennent de `core/navigation/chemins.ts`, jamais d'une
  chaîne écrite dans un composant.
- **États obligatoires** de chaque écran : chargement, erreur (message de l'API tel quel), vide, données
  (docs/design/DESIGN_SYSTEM.md).
- **Styles** : tokens du système de design (thème shadcn « zinc ») en variables CSS dans `src/styles.scss`.
